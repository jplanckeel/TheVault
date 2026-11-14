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

	snaps := make([]domain.PriceSnapshot, 0)
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

	snaps := make([]domain.PriceSnapshot, 0)
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

// TopMovers returns the items with the largest absolute price increase over the last N days.
func (s *PostgresPriceStore) TopMovers(ctx context.Context, days, limit int) ([]domain.TopMover, error) {
	const q = `
		WITH oldest AS (
			SELECT DISTINCT ON (item_id, condition)
				item_id, condition, price_median
			FROM price_snapshots
			WHERE scraped_at >= NOW() - ($1 || ' days')::interval
			ORDER BY item_id, condition, scraped_at ASC
		),
		newest AS (
			SELECT DISTINCT ON (item_id, condition)
				item_id, condition, price_median
			FROM price_snapshots
			ORDER BY item_id, condition, scraped_at DESC
		)
		SELECT
			n.item_id,
			n.condition,
			o.price_median AS price_old,
			n.price_median AS price_new,
			(n.price_median - o.price_median) AS delta_abs,
			CASE WHEN o.price_median > 0 THEN (n.price_median - o.price_median) / o.price_median * 100 ELSE 0 END AS delta_pct
		FROM newest n
		JOIN oldest o ON n.item_id = o.item_id AND n.condition = o.condition
		WHERE n.price_median > o.price_median
		ORDER BY delta_abs DESC
		LIMIT $2
	`
	rows, err := s.pool.Query(ctx, q, days, limit)
	if err != nil {
		return nil, fmt.Errorf("top movers: %w", err)
	}
	defer rows.Close()

	var movers []domain.TopMover
	for rows.Next() {
		var m domain.TopMover
		if err := rows.Scan(&m.ItemID, &m.Condition, &m.PriceOld, &m.PriceNew, &m.DeltaAbs, &m.DeltaPct); err != nil {
			return nil, fmt.Errorf("top mover scan: %w", err)
		}
		movers = append(movers, m)
	}
	return movers, rows.Err()
}

// itemsByExternalID resolves external IDs (directly or through item_aliases) to item IDs.
const itemsByExternalID = `
	SELECT i.id AS item_id, i.external_id AS ext FROM items i WHERE i.external_id = ANY($1)
	UNION
	SELECT a.item_id, a.external_id FROM item_aliases a WHERE a.external_id = ANY($1)
`

// QuotesByExternalIDs returns, for every item known by one of externalIDs, its latest
// quote per condition and the median quoted `days` ago.
func (s *PostgresPriceStore) QuotesByExternalIDs(ctx context.Context, externalIDs []string, days int) ([]domain.ExternalQuote, error) {
	q := `
		WITH ids AS (` + itemsByExternalID + `),
		latest AS (
			SELECT DISTINCT ON (p.item_id, p.condition)
				p.item_id, p.condition, p.price_min, p.price_max, p.price_median, p.sample_count, p.scraped_at
			FROM price_snapshots p
			WHERE p.item_id IN (SELECT item_id FROM ids)
			ORDER BY p.item_id, p.condition, p.scraped_at DESC
		),
		past AS (
			SELECT DISTINCT ON (p.item_id, p.condition) p.item_id, p.condition, p.price_median
			FROM price_snapshots p
			WHERE p.item_id IN (SELECT item_id FROM ids)
			  AND p.scraped_at <= NOW() - make_interval(days => $2)
			ORDER BY p.item_id, p.condition, p.scraped_at DESC
		)
		SELECT ids.ext, l.condition, l.price_min, l.price_max, l.price_median, l.sample_count, l.scraped_at, past.price_median
		FROM ids
		JOIN latest l ON l.item_id = ids.item_id
		LEFT JOIN past ON past.item_id = l.item_id AND past.condition = l.condition
		ORDER BY ids.ext, l.condition
	`
	rows, err := s.pool.Query(ctx, q, externalIDs, days)
	if err != nil {
		return nil, fmt.Errorf("quotes: %w", err)
	}
	defer rows.Close()

	quotes := make([]domain.ExternalQuote, 0)
	for rows.Next() {
		var x domain.ExternalQuote
		if err := rows.Scan(
			&x.ExternalID, &x.Condition, &x.PriceMin, &x.PriceMax, &x.PriceMedian,
			&x.SampleCount, &x.ScrapedAt, &x.PricePast,
		); err != nil {
			return nil, fmt.Errorf("quote scan: %w", err)
		}
		quotes = append(quotes, x)
	}
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("quotes rows: %w", err)
	}
	return quotes, nil
}

// HistoryByExternalID returns the snapshots of the last `days` days of the item known
// by externalID, newest first.
func (s *PostgresPriceStore) HistoryByExternalID(ctx context.Context, externalID string, days int) ([]domain.PriceSnapshot, error) {
	q := `
		WITH ids AS (` + itemsByExternalID + `)
		SELECT id, item_id, source, condition, price_min, price_max, price_median, sample_count, currency, scraped_at
		FROM price_snapshots
		WHERE item_id IN (SELECT item_id FROM ids)
		  AND scraped_at >= NOW() - make_interval(days => $2)
		ORDER BY scraped_at DESC
	`
	rows, err := s.pool.Query(ctx, q, []string{externalID}, days)
	if err != nil {
		return nil, fmt.Errorf("price history: %w", err)
	}
	defer rows.Close()

	snaps := make([]domain.PriceSnapshot, 0)
	for rows.Next() {
		var p domain.PriceSnapshot
		if err := rows.Scan(
			&p.ID, &p.ItemID, &p.Source, &p.Condition,
			&p.PriceMin, &p.PriceMax, &p.PriceMedian,
			&p.SampleCount, &p.Currency, &p.ScrapedAt,
		); err != nil {
			return nil, fmt.Errorf("price history scan: %w", err)
		}
		snaps = append(snaps, p)
	}
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("price history rows: %w", err)
	}
	return snaps, nil
}

// compile-time interface satisfaction check
var _ PriceStore = (*PostgresPriceStore)(nil)
