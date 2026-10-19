package store

import (
	"context"
	"errors"
	"fmt"

	"github.com/jackc/pgx/v5"
	"github.com/jplanckeel/thevaultbo/internal/domain"
)

// AdminItemStore groups the catalogue corrections available in the back-office.
type AdminItemStore interface {
	// Update applies an admin correction and locks the item against sync overwrites.
	Update(ctx context.Context, id string, patch domain.ItemPatch) (domain.Item, error)
	// Delete removes an item and tombstones its external_id so syncs cannot recreate it.
	Delete(ctx context.Context, id, deletedBy string) (domain.Item, error)
	// Merge moves collections, prices and history from source to target, aliases the
	// source external_id to target, then deletes source.
	Merge(ctx context.Context, sourceID, targetID string) (domain.Item, error)
	// OwnerCounts returns the number of users holding each item (owned or wishlist).
	OwnerCounts(ctx context.Context) (map[string]int, error)
	// PricedItemIDs returns the IDs of items with at least one price snapshot.
	PricedItemIDs(ctx context.Context) (map[string]bool, error)
}

// Update applies patch to item id. Only non-nil fields change.
func (s *PostgresItemStore) Update(ctx context.Context, id string, p domain.ItemPatch) (domain.Item, error) {
	var itemType *string
	if p.Type != nil {
		t := string(*p.Type)
		itemType = &t
	}
	q := `
		UPDATE items SET
			type         = COALESCE($2, type),
			title        = COALESCE($3, title),
			platform     = COALESCE($4, platform),
			region       = COALESCE($5, region),
			condition    = COALESCE($6, condition),
			cover_url    = COALESCE($7, cover_url),
			igdb_id      = COALESCE($8, igdb_id),
			summary      = COALESCE($9, summary),
			genres       = COALESCE($10, genres),
			developer    = COALESCE($11, developer),
			publisher    = COALESCE($12, publisher),
			release_year = COALESCE($13, release_year),
			admin_edited = TRUE,
			updated_at   = NOW()
		WHERE id = $1
		RETURNING ` + itemCols
	var out domain.Item
	err := scanItem(s.pool.QueryRow(ctx, q, id, itemType, p.Title, p.Platform, p.Region, p.Condition,
		p.CoverURL, p.IgdbID, p.Summary, p.Genres, p.Developer, p.Publisher, p.ReleaseYear), &out)
	if errors.Is(err, pgx.ErrNoRows) {
		return domain.Item{}, ErrNotFound
	}
	if err != nil {
		return domain.Item{}, fmt.Errorf("item update: %w", err)
	}
	return out, nil
}

// Delete removes item id and records a tombstone for its external_id.
func (s *PostgresItemStore) Delete(ctx context.Context, id, deletedBy string) (domain.Item, error) {
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return domain.Item{}, fmt.Errorf("item delete begin: %w", err)
	}
	defer func() { _ = tx.Rollback(ctx) }()

	var out domain.Item
	err = scanItem(tx.QueryRow(ctx, `DELETE FROM items WHERE id = $1 RETURNING `+itemCols, id), &out)
	if errors.Is(err, pgx.ErrNoRows) {
		return domain.Item{}, ErrNotFound
	}
	if err != nil {
		return domain.Item{}, fmt.Errorf("item delete: %w", err)
	}
	// Aliases pointing to this item disappear with it (ON DELETE CASCADE): tombstone them too.
	if _, err := tx.Exec(ctx, `
		INSERT INTO item_tombstones (external_id, title, deleted_by)
		VALUES ($1, $2, $3)
		ON CONFLICT (external_id) DO NOTHING`, out.ExternalID, out.Title, deletedBy); err != nil {
		return domain.Item{}, fmt.Errorf("item tombstone: %w", err)
	}
	if err := tx.Commit(ctx); err != nil {
		return domain.Item{}, fmt.Errorf("item delete commit: %w", err)
	}
	return out, nil
}

// Merge folds source into target in a single transaction and returns the updated target.
func (s *PostgresItemStore) Merge(ctx context.Context, sourceID, targetID string) (domain.Item, error) {
	if sourceID == targetID {
		return domain.Item{}, fmt.Errorf("item merge: source and target are the same")
	}
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return domain.Item{}, fmt.Errorf("item merge begin: %w", err)
	}
	defer func() { _ = tx.Rollback(ctx) }()

	var sourceExternalID string
	err = tx.QueryRow(ctx, `SELECT external_id FROM items WHERE id = $1 FOR UPDATE`, sourceID).Scan(&sourceExternalID)
	if errors.Is(err, pgx.ErrNoRows) {
		return domain.Item{}, ErrNotFound
	}
	if err != nil {
		return domain.Item{}, fmt.Errorf("item merge source: %w", err)
	}

	steps := []struct{ name, sql string }{
		{"collections", `INSERT INTO user_collections (user_id, item_id, status, added_at)
			SELECT user_id, $2, status, added_at FROM user_collections WHERE item_id = $1
			ON CONFLICT (user_id, item_id) DO NOTHING`},
		{"prices", `UPDATE price_snapshots SET item_id = $2 WHERE item_id = $1`},
		{"events", `UPDATE sync_events SET item_id = $2 WHERE item_id = $1`},
		{"aliases", `UPDATE item_aliases SET item_id = $2 WHERE item_id = $1`},
	}
	for _, step := range steps {
		if _, err := tx.Exec(ctx, step.sql, sourceID, targetID); err != nil {
			return domain.Item{}, fmt.Errorf("item merge %s: %w", step.name, err)
		}
	}
	if _, err := tx.Exec(ctx, `DELETE FROM items WHERE id = $1`, sourceID); err != nil {
		return domain.Item{}, fmt.Errorf("item merge delete: %w", err)
	}
	if _, err := tx.Exec(ctx, `
		INSERT INTO item_aliases (external_id, item_id) VALUES ($1, $2)
		ON CONFLICT (external_id) DO UPDATE SET item_id = EXCLUDED.item_id`, sourceExternalID, targetID); err != nil {
		return domain.Item{}, fmt.Errorf("item merge alias: %w", err)
	}

	var out domain.Item
	err = scanItem(tx.QueryRow(ctx, `UPDATE items SET updated_at = NOW() WHERE id = $1 RETURNING `+itemCols, targetID), &out)
	if errors.Is(err, pgx.ErrNoRows) {
		return domain.Item{}, ErrNotFound
	}
	if err != nil {
		return domain.Item{}, fmt.Errorf("item merge target: %w", err)
	}
	if err := tx.Commit(ctx); err != nil {
		return domain.Item{}, fmt.Errorf("item merge commit: %w", err)
	}
	return out, nil
}

// OwnerCounts returns item_id -> number of users having it in a collection or wishlist.
func (s *PostgresItemStore) OwnerCounts(ctx context.Context) (map[string]int, error) {
	rows, err := s.pool.Query(ctx, `SELECT item_id, COUNT(*) FROM user_collections GROUP BY item_id`)
	if err != nil {
		return nil, fmt.Errorf("owner counts: %w", err)
	}
	defer rows.Close()
	out := map[string]int{}
	for rows.Next() {
		var id string
		var n int
		if err := rows.Scan(&id, &n); err != nil {
			return nil, fmt.Errorf("owner counts scan: %w", err)
		}
		out[id] = n
	}
	return out, rows.Err()
}

// PricedItemIDs returns the set of items with at least one price snapshot.
func (s *PostgresItemStore) PricedItemIDs(ctx context.Context) (map[string]bool, error) {
	rows, err := s.pool.Query(ctx, `SELECT DISTINCT item_id FROM price_snapshots WHERE price_median > 0`)
	if err != nil {
		return nil, fmt.Errorf("priced items: %w", err)
	}
	defer rows.Close()
	out := map[string]bool{}
	for rows.Next() {
		var id string
		if err := rows.Scan(&id); err != nil {
			return nil, fmt.Errorf("priced items scan: %w", err)
		}
		out[id] = true
	}
	return out, rows.Err()
}

var _ AdminItemStore = (*PostgresItemStore)(nil)
