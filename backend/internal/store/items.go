package store

import (
	"context"
	"errors"
	"fmt"
	"strings"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/jplanckeel/thevaultbo/internal/domain"
)

// PostgresItemStore implements ItemStore using pgx.
type PostgresItemStore struct {
	pool *pgxpool.Pool
}

// NewItemStore creates a new PostgresItemStore.
func NewItemStore(pool *pgxpool.Pool) *PostgresItemStore {
	return &PostgresItemStore{pool: pool}
}

const itemCols = `id, external_id, type, title, platform, region, condition, status,
	cover_url, igdb_id, summary, genres, developer, publisher, release_year,
	admin_edited, created_at, updated_at`

// prefixedItemCols returns itemCols qualified with a table alias (e.g. "i.id, i.external_id, …"),
// so that joins stay in sync with scanItem.
func prefixedItemCols(alias string) string {
	cols := strings.Split(itemCols, ",")
	for k, c := range cols {
		cols[k] = alias + "." + strings.TrimSpace(c)
	}
	return strings.Join(cols, ", ")
}

func scanItem(row interface {
	Scan(dest ...any) error
}, i *domain.Item) error {
	return row.Scan(
		&i.ID, &i.ExternalID, &i.Type, &i.Title,
		&i.Platform, &i.Region, &i.Condition, &i.Status,
		&i.CoverURL, &i.IgdbID, &i.Summary, &i.Genres,
		&i.Developer, &i.Publisher, &i.ReleaseYear,
		&i.AdminEdited, &i.CreatedAt, &i.UpdatedAt,
	)
}

// Upsert inserts or updates an item identified by external_id.
//
//   - IGDB fields are preserved on conflict (not overwritten by import).
//   - Catalogue fields corrected by an admin (admin_edited) are preserved too.
//   - An external_id merged into another item resolves to that item (item_aliases).
//   - An external_id deleted by an admin is rejected with ErrTombstoned.
func (s *PostgresItemStore) Upsert(ctx context.Context, item domain.Item) (domain.Item, error) {
	var tombstoned bool
	if err := s.pool.QueryRow(ctx,
		`SELECT EXISTS (SELECT 1 FROM item_tombstones WHERE external_id = $1)`, item.ExternalID,
	).Scan(&tombstoned); err != nil {
		return domain.Item{}, fmt.Errorf("item upsert tombstone: %w", err)
	}
	if tombstoned {
		return domain.Item{}, ErrTombstoned
	}

	var out domain.Item
	err := scanItem(s.pool.QueryRow(ctx, `
		UPDATE items SET updated_at = NOW()
		WHERE id = (SELECT item_id FROM item_aliases WHERE external_id = $1)
		RETURNING `+itemCols, item.ExternalID), &out)
	if err == nil {
		return out, nil
	}
	if !errors.Is(err, pgx.ErrNoRows) {
		return domain.Item{}, fmt.Errorf("item upsert alias: %w", err)
	}

	const q = `
		INSERT INTO items (external_id, type, title, platform, region, condition, status, cover_url, created_at, updated_at)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, NOW(), NOW())
		ON CONFLICT (external_id) DO UPDATE SET
			type       = CASE WHEN items.admin_edited THEN items.type ELSE EXCLUDED.type END,
			title      = CASE WHEN items.admin_edited THEN items.title ELSE EXCLUDED.title END,
			platform   = CASE WHEN items.admin_edited THEN items.platform ELSE EXCLUDED.platform END,
			region     = CASE WHEN items.admin_edited THEN items.region ELSE EXCLUDED.region END,
			condition  = EXCLUDED.condition,
			status     = EXCLUDED.status,
			cover_url  = CASE WHEN items.igdb_id = '' AND NOT items.admin_edited THEN EXCLUDED.cover_url ELSE items.cover_url END,
			updated_at = NOW()
		RETURNING ` + itemCols

	err = scanItem(s.pool.QueryRow(ctx, q,
		item.ExternalID, string(item.Type), item.Title, item.Platform,
		item.Region, item.Condition, item.Status, item.CoverURL,
	), &out)
	if err != nil {
		return domain.Item{}, fmt.Errorf("item upsert: %w", err)
	}
	return out, nil
}

// List returns all items ordered by title.
func (s *PostgresItemStore) List(ctx context.Context) ([]domain.Item, error) {
	q := `SELECT ` + itemCols + ` FROM items ORDER BY title ASC`
	rows, err := s.pool.Query(ctx, q)
	if err != nil {
		return nil, fmt.Errorf("items list: %w", err)
	}
	defer rows.Close()

	var items []domain.Item
	for rows.Next() {
		var i domain.Item
		if err := scanItem(rows, &i); err != nil {
			return nil, fmt.Errorf("item scan: %w", err)
		}
		items = append(items, i)
	}
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("items list rows: %w", err)
	}
	return items, nil
}

// GetByID returns an item by its UUID.
func (s *PostgresItemStore) GetByID(ctx context.Context, id string) (domain.Item, error) {
	q := `SELECT ` + itemCols + ` FROM items WHERE id = $1`
	var i domain.Item
	if err := scanItem(s.pool.QueryRow(ctx, q, id), &i); err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return domain.Item{}, ErrNotFound
		}
		return domain.Item{}, fmt.Errorf("item get: %w", err)
	}
	return i, nil
}

// ListWithoutIGDB returns items that have not yet been enriched from IGDB.
func (s *PostgresItemStore) ListWithoutIGDB(ctx context.Context) ([]domain.Item, error) {
	q := `SELECT ` + itemCols + ` FROM items WHERE (type = 'game' AND igdb_id = '') OR ((type = 'console' OR type = 'accessory') AND cover_url = '') ORDER BY type, title ASC`
	rows, err := s.pool.Query(ctx, q)
	if err != nil {
		return nil, fmt.Errorf("items without igdb: %w", err)
	}
	defer rows.Close()

	var items []domain.Item
	for rows.Next() {
		var i domain.Item
		if err := scanItem(rows, &i); err != nil {
			return nil, fmt.Errorf("item scan: %w", err)
		}
		items = append(items, i)
	}
	return items, rows.Err()
}

// UpdateIGDB writes IGDB-sourced metadata onto an existing item.
func (s *PostgresItemStore) UpdateIGDB(ctx context.Context, id string, data domain.IGDBData) error {
	const q = `
		UPDATE items SET
			igdb_id      = $2,
			cover_url    = $3,
			summary      = $4,
			genres       = $5,
			developer    = $6,
			publisher    = $7,
			release_year = $8,
			updated_at   = NOW()
		WHERE id = $1
	`
	_, err := s.pool.Exec(ctx, q,
		id, data.IgdbID, data.CoverURL, data.Summary,
		data.Genres, data.Developer, data.Publisher, data.ReleaseYear,
	)
	if err != nil {
		return fmt.Errorf("item update igdb: %w", err)
	}
	return nil
}

// UpdateCoverURL sets the cover_url for an item (manual fallback).
func (s *PostgresItemStore) UpdateCoverURL(ctx context.Context, id string, coverURL string) error {
	_, err := s.pool.Exec(ctx, `UPDATE items SET cover_url = $2, updated_at = NOW() WHERE id = $1`, id, coverURL)
	if err != nil {
		return fmt.Errorf("item update cover: %w", err)
	}
	return nil
}

// compile-time interface satisfaction check
var _ ItemStore = (*PostgresItemStore)(nil)

// LookupByExternalIDs returns the catalogue sheets of the items known by externalIDs.
func (s *PostgresItemStore) LookupByExternalIDs(ctx context.Context, externalIDs []string) ([]domain.CatalogEntry, error) {
	q := `
		WITH ids AS (` + itemsByExternalID + `)
		SELECT ids.ext, i.id, i.type, i.title, i.platform, i.cover_url, i.igdb_id, i.summary,
		       i.genres, i.developer, i.publisher, i.release_year, i.admin_edited, i.updated_at
		FROM ids JOIN items i ON i.id = ids.item_id
		ORDER BY ids.ext`
	rows, err := s.pool.Query(ctx, q, externalIDs)
	if err != nil {
		return nil, fmt.Errorf("lookup: %w", err)
	}
	defer rows.Close()
	entries := make([]domain.CatalogEntry, 0)
	for rows.Next() {
		var e domain.CatalogEntry
		if err := rows.Scan(&e.ExternalID, &e.ItemID, &e.Type, &e.Title, &e.Platform, &e.CoverURL, &e.IgdbID,
			&e.Summary, &e.Genres, &e.Developer, &e.Publisher, &e.ReleaseYear, &e.AdminEdited, &e.UpdatedAt); err != nil {
			return nil, fmt.Errorf("lookup scan: %w", err)
		}
		entries = append(entries, e)
	}
	return entries, rows.Err()
}

// ListForPricing returns at most limit items not quoted for olderThan, never-quoted first.
func (s *PostgresItemStore) ListForPricing(ctx context.Context, olderThan time.Duration, limit int) ([]domain.Item, error) {
	q := `
		SELECT ` + prefixedItemCols("i") + `
		FROM items i
		LEFT JOIN LATERAL (
			SELECT MAX(p.scraped_at) AS last FROM price_snapshots p WHERE p.item_id = i.id
		) lp ON TRUE
		WHERE lp.last IS NULL OR lp.last < NOW() - make_interval(secs => $1)
		ORDER BY lp.last ASC NULLS FIRST, i.created_at ASC
		LIMIT $2`
	rows, err := s.pool.Query(ctx, q, olderThan.Seconds(), limit)
	if err != nil {
		return nil, fmt.Errorf("list for pricing: %w", err)
	}
	defer rows.Close()
	items := make([]domain.Item, 0)
	for rows.Next() {
		var i domain.Item
		if err := scanItem(rows, &i); err != nil {
			return nil, fmt.Errorf("list for pricing scan: %w", err)
		}
		items = append(items, i)
	}
	return items, rows.Err()
}
