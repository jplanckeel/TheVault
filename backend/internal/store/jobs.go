package store

import (
	"context"
	"fmt"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/jplanckeel/thevaultbo/internal/domain"
)

// PostgresJobStore implements JobStore using pgx.
type PostgresJobStore struct {
	pool *pgxpool.Pool
}

// NewJobStore creates a new PostgresJobStore.
func NewJobStore(pool *pgxpool.Pool) *PostgresJobStore {
	return &PostgresJobStore{pool: pool}
}

// Create inserts a new scrape job and returns it with its generated ID.
func (s *PostgresJobStore) Create(ctx context.Context, job domain.ScrapeJob) (domain.ScrapeJob, error) {
	const q = `
		INSERT INTO scrape_jobs (triggered_by, status, items_total, items_done, error, started_at)
		VALUES ($1, $2, $3, $4, $5, NOW())
		RETURNING id, triggered_by, status, items_total, items_done, error, started_at, finished_at
	`
	return scanJob(s.pool.QueryRow(ctx, q,
		job.TriggeredBy, job.Status, job.ItemsTotal, job.ItemsDone, job.Error,
	))
}

// Update persists status, items_done, error, and finished_at for a job.
func (s *PostgresJobStore) Update(ctx context.Context, job domain.ScrapeJob) error {
	const q = `
		UPDATE scrape_jobs SET status=$1, items_done=$2, error=$3, finished_at=$4
		WHERE id=$5
	`
	_, err := s.pool.Exec(ctx, q, job.Status, job.ItemsDone, job.Error, job.FinishedAt, job.ID)
	if err != nil {
		return fmt.Errorf("job update: %w", err)
	}
	return nil
}

// GetByID returns a job by its UUID.
func (s *PostgresJobStore) GetByID(ctx context.Context, id string) (domain.ScrapeJob, error) {
	const q = `
		SELECT id, triggered_by, status, items_total, items_done, error, started_at, finished_at
		FROM scrape_jobs WHERE id = $1
	`
	return scanJob(s.pool.QueryRow(ctx, q, id))
}

// List returns all jobs ordered by start time descending.
func (s *PostgresJobStore) List(ctx context.Context) ([]domain.ScrapeJob, error) {
	const q = `
		SELECT id, triggered_by, status, items_total, items_done, error, started_at, finished_at
		FROM scrape_jobs ORDER BY started_at DESC
	`
	rows, err := s.pool.Query(ctx, q)
	if err != nil {
		return nil, fmt.Errorf("jobs list: %w", err)
	}
	defer rows.Close()

	var jobs []domain.ScrapeJob
	for rows.Next() {
		var j domain.ScrapeJob
		if err := rows.Scan(
			&j.ID, &j.TriggeredBy, &j.Status, &j.ItemsTotal,
			&j.ItemsDone, &j.Error, &j.StartedAt, &j.FinishedAt,
		); err != nil {
			return nil, fmt.Errorf("job scan: %w", err)
		}
		jobs = append(jobs, j)
	}
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("jobs list rows: %w", err)
	}
	return jobs, nil
}

// rowScanner abstracts pgx.Row for testability.
type rowScanner interface {
	Scan(dest ...any) error
}

func scanJob(row rowScanner) (domain.ScrapeJob, error) {
	var j domain.ScrapeJob
	err := row.Scan(
		&j.ID, &j.TriggeredBy, &j.Status, &j.ItemsTotal,
		&j.ItemsDone, &j.Error, &j.StartedAt, &j.FinishedAt,
	)
	if err != nil {
		return domain.ScrapeJob{}, fmt.Errorf("job scan: %w", err)
	}
	return j, nil
}

// compile-time interface satisfaction check
var _ JobStore = (*PostgresJobStore)(nil)
