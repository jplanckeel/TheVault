package store

import (
	"context"
	"fmt"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/jplanckeel/thevaultbo/internal/domain"
)

// SyncEventStore persists the audit log of catalogue writes.
type SyncEventStore interface {
	Record(ctx context.Context, e domain.SyncEvent) error
	List(ctx context.Context, f domain.SyncEventFilter) ([]domain.SyncEvent, error)
	Summary(ctx context.Context, since time.Time) (domain.SyncSummary, error)
}

// PostgresSyncEventStore implements SyncEventStore.
type PostgresSyncEventStore struct {
	pool *pgxpool.Pool
}

// NewSyncEventStore creates a PostgresSyncEventStore.
func NewSyncEventStore(pool *pgxpool.Pool) *PostgresSyncEventStore {
	return &PostgresSyncEventStore{pool: pool}
}

const maxSyncEvents = 500

// Record appends an event.
func (s *PostgresSyncEventStore) Record(ctx context.Context, e domain.SyncEvent) error {
	_, err := s.pool.Exec(ctx, `
		INSERT INTO sync_events (source, action, item_id, external_id, title, actor, detail)
		VALUES ($1, $2, $3, $4, $5, $6, $7)`,
		e.Source, e.Action, e.ItemID, e.ExternalID, e.Title, e.Actor, e.Detail)
	if err != nil {
		return fmt.Errorf("sync event record: %w", err)
	}
	return nil
}

// List returns the most recent events matching f (at most 500).
func (s *PostgresSyncEventStore) List(ctx context.Context, f domain.SyncEventFilter) ([]domain.SyncEvent, error) {
	limit := f.Limit
	if limit <= 0 || limit > maxSyncEvents {
		limit = maxSyncEvents
	}
	rows, err := s.pool.Query(ctx, `
		SELECT id, source, action, item_id, external_id, title, actor, detail, created_at
		FROM sync_events
		WHERE ($1 = '' OR source = $1)
		  AND ($2 = '' OR action = $2)
		  AND ($3 = '' OR item_id::text = $3)
		ORDER BY created_at DESC
		LIMIT $4`, f.Source, f.Action, f.ItemID, limit)
	if err != nil {
		return nil, fmt.Errorf("sync events list: %w", err)
	}
	defer rows.Close()
	events := []domain.SyncEvent{}
	for rows.Next() {
		var e domain.SyncEvent
		if err := rows.Scan(&e.ID, &e.Source, &e.Action, &e.ItemID, &e.ExternalID, &e.Title, &e.Actor, &e.Detail, &e.CreatedAt); err != nil {
			return nil, fmt.Errorf("sync event scan: %w", err)
		}
		events = append(events, e)
	}
	return events, rows.Err()
}

// Summary counts events per source and action since the given time.
func (s *PostgresSyncEventStore) Summary(ctx context.Context, since time.Time) (domain.SyncSummary, error) {
	rows, err := s.pool.Query(ctx, `
		SELECT source, action, COUNT(*) FROM sync_events WHERE created_at >= $1 GROUP BY source, action`, since)
	if err != nil {
		return domain.SyncSummary{}, fmt.Errorf("sync summary: %w", err)
	}
	defer rows.Close()
	out := domain.SyncSummary{Since: since, Counts: map[string]map[string]int{}}
	for rows.Next() {
		var source, action string
		var n int
		if err := rows.Scan(&source, &action, &n); err != nil {
			return domain.SyncSummary{}, fmt.Errorf("sync summary scan: %w", err)
		}
		if out.Counts[source] == nil {
			out.Counts[source] = map[string]int{}
		}
		out.Counts[source][action] = n
	}
	return out, rows.Err()
}

var _ SyncEventStore = (*PostgresSyncEventStore)(nil)
