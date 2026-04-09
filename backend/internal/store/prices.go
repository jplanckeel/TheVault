package store

import (
	"context"
	"fmt"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/jplanckeel/thevaultbo/internal/domain"
)

// PostgresPriceStore implements PriceStore using pgx.
type PostgresPriceStore struct {
	pool *pgxpool.Pool
}

// NewPriceStore creates a new PostgresPriceStore.
func NewPriceStore(pool *pgxpool.Pool) *PostgresPriceStore {
	return &PostgresPriceStore{pool: pool}
}

// Insert stores a new price snapshot.
func (s *PostgresPriceStore) Insert(ctx context.Context, snap domain.PriceSnapshot) (domain.PriceSnapshot, error) {
	const q = `
		INSERT INTO price_snapshots (item_id, source, condition, price_min, price_max, price_median, sample_count, currency, scraped_at)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, NOW())
		RETURNING id, item_id, source, condition, price_min, price_max, price_median, sample_count, currency, scraped_at
	`
	var out domain.PriceSnapshot
	err := s.pool.QueryRow(ctx, q,
		snap.ItemID, snap.Source, string(snap.Condition),
		snap.PriceMin, snap.PriceMax, snap.PriceMedian,
		snap.SampleCount, snap.Currency,
	).Scan(
		&out.ID, &out.ItemID, &out.Source, &out.Condition,
		&out.PriceMin, &out.PriceMax, &out.PriceMedian,
		&out.SampleCount, &out.Currency, &out.ScrapedAt,
	)
	if err != nil {
		return domain.PriceSnapshot{}, fmt.Errorf("price insert: %w", err)
	}
	return out, nil
}

// ListByItemID returns all snapshots for one item, newest first.
func (s *PostgresPriceStore) ListByItemID(ctx context.Context, itemID string) ([]domain.PriceSnapshot, error) {
	const q = `
		SELECT id, item_id, source, condition, price_min, price_max, price_median, sample_count, currency, scraped_at
		FROM price_snapshots WHERE item_id = $1 ORDER BY scraped_at DESC
	`
	rows, err := s.pool.Query(ctx, q, itemID)
	if err != nil {
		return nil, fmt.Errorf("prices list: %w", err)
	}
	defer rows.Close()

	var snaps []domain.PriceSnapshot
	for rows.Next() {
		var p domain.PriceSnapshot
		if err := rows.Scan(
			&p.ID, &p.ItemID, &p.Source, &p.Condition,
			&p.PriceMin, &p.PriceMax, &p.PriceMedian,
			&p.SampleCount, &p.Currency, &p.ScrapedAt,
		); err != nil {
			return nil, fmt.Errorf("price scan: %w", err)
		}
		snaps = append(snaps, p)
	}
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("prices list rows: %w", err)
	}
	return snaps, nil
}

// LatestPerItem returns the single most recent snapshot per (item_id, condition).
func (s *PostgresPriceStore) LatestPerItem(ctx context.Context) ([]domain.PriceSnapshot, error) {
	const q = `
		SELECT DISTINCT ON (item_id, condition)
			id, item_id, source, condition, price_min, price_max, price_median, sample_count, currency, scraped_at
		FROM price_snapshots
		ORDER BY item_id, condition, scraped_at DESC
	`
	rows, err := s.pool.Query(ctx, q)
	if err != nil {
		return nil, fmt.Errorf("latest prices: %w", err)
	}
	defer rows.Close()

	var snaps []domain.PriceSnapshot
	for rows.Next() {
		var p domain.PriceSnapshot
		if err := rows.Scan(
			&p.ID, &p.ItemID, &p.Source, &p.Condition,
			&p.PriceMin, &p.PriceMax, &p.PriceMedian,
			&p.SampleCount, &p.Currency, &p.ScrapedAt,
		); err != nil {
			return nil, fmt.Errorf("latest price scan: %w", err)
		}
		snaps = append(snaps, p)
	}
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("latest prices rows: %w", err)
	}
	return snaps, nil
}

// compile-time interface satisfaction check
var _ PriceStore = (*PostgresPriceStore)(nil)
