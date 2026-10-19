package scheduler

import (
	"context"
	"fmt"
	"os"
	"strings"
	"time"

	"github.com/robfig/cron/v3"

	"github.com/jplanckeel/thevaultbo/internal/domain"
	"github.com/jplanckeel/thevaultbo/internal/store"
)

const defaultEbaySchedule = "0 * * * *" // every hour

// IGDBRunner is the interface the scheduler uses to trigger an IGDB sync.
type IGDBRunner interface {
	Run(triggeredBy string)
}

// Option configures the Scheduler.
type Option func(*Scheduler)

// WithEbaySchedule overrides the eBay cron expression.
func WithEbaySchedule(expr string) Option {
	return func(s *Scheduler) { s.ebaySchedule = expr }
}

// WithIGDB registers an IGDB runner on the given cron schedule.
// An empty schedule disables the IGDB cron.
func WithIGDB(runner IGDBRunner, schedule string) Option {
	return func(s *Scheduler) {
		s.igdbRunner = runner
		s.igdbSchedule = schedule
	}
}

// Scheduler runs scrape jobs on cron schedules.
type Scheduler struct {
	jobs         store.JobStore
	ebayRunner   func(jobID, triggeredBy string, itemIDs ...string)
	ebaySchedule string
	igdbRunner   IGDBRunner
	igdbSchedule string
	cron         *cron.Cron
	ticker       *time.Ticker
	stopCh       chan struct{}
}

// New creates a new Scheduler.
func New(
	jobs store.JobStore,
	runner func(jobID, triggeredBy string, itemIDs ...string),
	opts ...Option,
) *Scheduler {
	s := &Scheduler{
		jobs:         jobs,
		ebayRunner:   runner,
		ebaySchedule: defaultEbaySchedule,
	}
	for _, o := range opts {
		o(s)
	}
	return s
}

// Start registers the cron jobs and starts the scheduler.
// If the eBay schedule is "@every <duration>" with a duration < 1s, a time.Ticker
// is used instead of robfig/cron (which rounds sub-second durations to 1s).
// Returns an error if any schedule expression is invalid.
func (s *Scheduler) Start() error {
	// Sub-second eBay schedule: use ticker (test helper).
	if dur, ok := parseEvery(s.ebaySchedule); ok && dur < time.Second {
		s.stopCh = make(chan struct{})
		s.ticker = time.NewTicker(dur)
		go func() {
			for {
				select {
				case <-s.ticker.C:
					s.fireEbay()
				case <-s.stopCh:
					return
				}
			}
		}()
		return nil
	}

	s.cron = cron.New()

	if _, err := s.cron.AddFunc(s.ebaySchedule, s.fireEbay); err != nil {
		return fmt.Errorf("scheduler: invalid eBay schedule %q: %w", s.ebaySchedule, err)
	}

	if s.igdbRunner != nil && s.igdbSchedule != "" {
		if _, err := s.cron.AddFunc(s.igdbSchedule, s.fireIGDB); err != nil {
			return fmt.Errorf("scheduler: invalid IGDB schedule %q: %w", s.igdbSchedule, err)
		}
	}

	s.cron.Start()
	return nil
}

// Stop gracefully stops the scheduler.
func (s *Scheduler) Stop() {
	if s.ticker != nil {
		s.ticker.Stop()
		close(s.stopCh)
	}
	if s.cron != nil {
		s.cron.Stop()
	}
}

func (s *Scheduler) fireEbay() {
	ctx := context.Background()
	job, err := s.jobs.Create(ctx, domain.ScrapeJob{
		JobType:     "ebay",
		TriggeredBy: "cron",
		Status:      "pending",
	})
	if err != nil {
		fmt.Fprintf(os.Stderr, "scheduler: create ebay job: %v\n", err)
		return
	}
	s.ebayRunner(job.ID, "cron")
}

func (s *Scheduler) fireIGDB() {
	s.igdbRunner.Run("cron")
}

// parseEvery checks if expr is "@every <duration>" and returns the duration.
func parseEvery(expr string) (time.Duration, bool) {
	const prefix = "@every "
	if !strings.HasPrefix(expr, prefix) {
		return 0, false
	}
	dur, err := time.ParseDuration(strings.TrimSpace(expr[len(prefix):]))
	if err != nil {
		return 0, false
	}
	return dur, true
}
