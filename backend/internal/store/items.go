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

// Upsert inserts or updates an item identified by external_id.
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
			cover_url  = EXCLUDED.cover_url,
			updated_at = NOW()
		RETURNING id, external_id, type, title, platform, region, condition, status, cover_url, created_at, updated_at
	`
	var out domain.Item
	err := s.pool.QueryRow(ctx, q,
		item.ExternalID, string(item.Type), item.Title, item.Platform,
		item.Region, item.Condition, item.Status, item.CoverURL,
	).Scan(
		&out.ID, &out.ExternalID, &out.Type, &out.Title,
		&out.Platform, &out.Region, &out.Condition, &out.Status,
		&out.CoverURL, &out.CreatedAt, &out.UpdatedAt,
	)
	if err != nil {
		return domain.Item{}, fmt.Errorf("item upsert: %w", err)
	}
	return out, nil
}

// List returns all items ordered by title.
func (s *PostgresItemStore) List(ctx context.Context) ([]domain.Item, error) {
	const q = `
		SELECT id, external_id, type, title, platform, region, condition, status, cover_url, created_at, updated_at
		FROM items ORDER BY title ASC
	`
	rows, err := s.pool.Query(ctx, q)
	if err != nil {
		return nil, fmt.Errorf("items list: %w", err)
	}
	defer rows.Close()

	var items []domain.Item
	for rows.Next() {
		var i domain.Item
		if err := rows.Scan(
			&i.ID, &i.ExternalID, &i.Type, &i.Title,
			&i.Platform, &i.Region, &i.Condition, &i.Status,
			&i.CoverURL, &i.CreatedAt, &i.UpdatedAt,
		); err != nil {
			return nil, fmt.Errorf("item scan: %w", err)
		}
		items = append(items, i)
	}
	return items, rows.Err()
}

// GetByID returns an item by its UUID.
func (s *PostgresItemStore) GetByID(ctx context.Context, id string) (domain.Item, error) {
	const q = `
		SELECT id, external_id, type, title, platform, region, condition, status, cover_url, created_at, updated_at
		FROM items WHERE id = $1
	`
	var i domain.Item
	err := s.pool.QueryRow(ctx, q, id).Scan(
		&i.ID, &i.ExternalID, &i.Type, &i.Title,
		&i.Platform, &i.Region, &i.Condition, &i.Status,
		&i.CoverURL, &i.CreatedAt, &i.UpdatedAt,
	)
	if err != nil {
		return domain.Item{}, fmt.Errorf("item get: %w", err)
	}
	return i, nil
}
