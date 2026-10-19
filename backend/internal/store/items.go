package store

import (
	"context"
	"fmt"

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
	created_at, updated_at`

func scanItem(row interface {
	Scan(dest ...any) error
}, i *domain.Item) error {
	return row.Scan(
		&i.ID, &i.ExternalID, &i.Type, &i.Title,
		&i.Platform, &i.Region, &i.Condition, &i.Status,
		&i.CoverURL, &i.IgdbID, &i.Summary, &i.Genres,
		&i.Developer, &i.Publisher, &i.ReleaseYear,
		&i.CreatedAt, &i.UpdatedAt,
	)
}

// Upsert inserts or updates an item identified by external_id.
// IGDB fields are preserved on conflict (not overwritten by import).
func (s *PostgresItemStore) Upsert(ctx context.Context, item domain.Item) (domain.Item, error) {
	const q = `
		INSERT INTO items (external_id, type, title, platform, region, condition, status, cover_url, created_at, updated_at)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, NOW(), NOW())
		ON CONFLICT (external_id) DO UPDATE SET
			type       = EXCLUDED.type,
			title      = EXCLUDED.title,
			platform   = EXCLUDED.platform,
			region     = EXCLUDED.region,
			condition  = EXCLUDED.condition,
			status     = EXCLUDED.status,
			cover_url  = CASE WHEN items.igdb_id = '' THEN EXCLUDED.cover_url ELSE items.cover_url END,
			updated_at = NOW()
		RETURNING ` + itemCols

	var out domain.Item
	err := scanItem(s.pool.QueryRow(ctx, q,
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
