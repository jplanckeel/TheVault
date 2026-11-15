package igdb

import (
	"context"
	"fmt"
	"time"

	"github.com/jplanckeel/thevaultbo/internal/domain"
	"github.com/jplanckeel/thevaultbo/internal/store"
)

// Runner handles IGDB sync jobs.
type Runner struct {
	items  store.ItemStore
	jobs   store.JobStore
	client *Client
}

// NewRunner creates a new Runner.
func NewRunner(items store.ItemStore, jobs store.JobStore, client *Client) *Runner {
	return &Runner{items: items, jobs: jobs, client: client}
}

// Prepare lists items missing IGDB data and creates a tracked job.
// Returns the job and items so the caller can respond before starting sync.
func (r *Runner) Prepare(ctx context.Context, triggeredBy string) (domain.ScrapeJob, []domain.Item, error) {
	items, err := r.items.ListWithoutIGDB(ctx)
	if err != nil {
		return domain.ScrapeJob{}, nil, fmt.Errorf("list items: %w", err)
	}

	job, err := r.jobs.Create(ctx, domain.ScrapeJob{
		JobType:     "igdb",
		TriggeredBy: triggeredBy,
		Status:      "running",
		ItemsTotal:  len(items),
	})
	if err != nil {
		return domain.ScrapeJob{}, nil, fmt.Errorf("create job: %w", err)
	}

	return job, items, nil
}

// Sync enriches items with IGDB data and updates the job progress.
// It runs synchronously; wrap in a goroutine if needed.
func (r *Runner) Sync(ctx context.Context, job domain.ScrapeJob, items []domain.Item) {
	done := 0
	var lastErr string

	for _, item := range items {
		switch item.Type {
		case "game":
			data, err := r.client.SearchGame(item.Title)
			if err != nil {
				fmt.Printf("igdb sync: %s — error: %v\n", item.Title, err)
				lastErr = err.Error()
			} else if data == nil {
				fmt.Printf("igdb sync: %s — no result\n", item.Title)
				_ = r.items.MarkIGDBChecked(ctx, item.ID)
			} else {
				if err := r.items.UpdateIGDB(ctx, item.ID, *data); err != nil {
					fmt.Printf("igdb sync: %s — db error: %v\n", item.Title, err)
					lastErr = err.Error()
				} else {
					fmt.Printf("igdb sync: %s — OK (igdb_id=%s)\n", item.Title, data.IgdbID)
				}
			}
		case "console":
			logoURL, err := r.client.SearchPlatform(item.Platform)
			if err != nil {
				fmt.Printf("igdb sync platform: %s — error: %v\n", item.Platform, err)
				lastErr = err.Error()
			} else if logoURL == "" {
				fmt.Printf("igdb sync platform: %s — no logo\n", item.Platform)
				_ = r.items.MarkIGDBChecked(ctx, item.ID)
			} else {
				data := domain.IGDBData{CoverURL: logoURL}
				if err := r.items.UpdateIGDB(ctx, item.ID, data); err != nil {
					fmt.Printf("igdb sync platform: %s — db error: %v\n", item.Platform, err)
					lastErr = err.Error()
				} else {
					fmt.Printf("igdb sync platform: %s — OK (%s)\n", item.Platform, logoURL)
				}
			}
		default:
			// accessory: skip in auto-sync, handled via manual cover_url
			fmt.Printf("igdb sync: %s — skipped (type=%s)\n", item.Title, item.Type)
			_ = r.items.MarkIGDBChecked(ctx, item.ID)
		}

		done++
		_ = r.jobs.Update(ctx, domain.ScrapeJob{
			ID:         job.ID,
			Status:     "running",
			ItemsTotal: len(items),
			ItemsDone:  done,
			Error:      lastErr,
		})

		time.Sleep(350 * time.Millisecond)
	}

	now := time.Now()
	_ = r.jobs.Update(ctx, domain.ScrapeJob{
		ID:         job.ID,
		Status:     "done",
		ItemsTotal: len(items),
		ItemsDone:  done,
		Error:      lastErr,
		FinishedAt: &now,
	})
	fmt.Printf("igdb sync: finished (%d/%d)\n", done, len(items))
}

// Run is a fire-and-forget entry point used by the scheduler.
// It creates the job and runs the sync in a new goroutine.
func (r *Runner) Run(triggeredBy string) {
	go func() {
		ctx := context.Background()
		job, items, err := r.Prepare(ctx, triggeredBy)
		if err != nil {
			fmt.Printf("igdb runner: %v\n", err)
			return
		}
		r.Sync(ctx, job, items)
	}()
}
