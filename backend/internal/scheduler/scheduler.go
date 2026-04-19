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

const defaultSchedule = "0 * * * *" // every hour

// Option configures the Scheduler.
type Option func(*Scheduler)

// WithSchedule overrides the cron expression (useful in tests).
func WithSchedule(expr string) Option {
	return func(s *Scheduler) { s.schedule = expr }
}

// Scheduler runs the eBay scraper on a cron schedule.
type Scheduler struct {
	jobs     store.JobStore
	runner   func(jobID, triggeredBy string, itemIDs ...string)
	schedule string
	cron     *cron.Cron
	ticker   *time.Ticker
	stopCh   chan struct{}
}

// New creates a new Scheduler.
func New(
	jobs store.JobStore,
	runner func(jobID, triggeredBy string, itemIDs ...string),
	opts ...Option,
) *Scheduler {
	s := &Scheduler{
		jobs:     jobs,
		runner:   runner,
		schedule: defaultSchedule,
	}
	for _, o := range opts {
		o(s)
	}
	return s
}

// Start registers the cron job and starts the scheduler.
// If the schedule is "@every <duration>" with a duration < 1s, a time.Ticker
// is used instead of robfig/cron (which rounds sub-second durations to 1s).
// Returns an error if the schedule expression is invalid.
func (s *Scheduler) Start() error {
	if dur, ok := parseEvery(s.schedule); ok && dur < time.Second {
		s.stopCh = make(chan struct{})
		s.ticker = time.NewTicker(dur)
		go func() {
			for {
				select {
				case <-s.ticker.C:
					s.fire()
				case <-s.stopCh:
					return
				}
			}
		}()
		return nil
	}

	s.cron = cron.New()
	if _, err := s.cron.AddFunc(s.schedule, s.fire); err != nil {
		return fmt.Errorf("scheduler: invalid schedule %q: %w", s.schedule, err)
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

func (s *Scheduler) fire() {
	ctx := context.Background()
	job, err := s.jobs.Create(ctx, domain.ScrapeJob{
		JobType:     "ebay",
		TriggeredBy: "cron",
		Status:      "pending",
	})
	if err != nil {
		fmt.Fprintf(os.Stderr, "scheduler: create job: %v\n", err)
		return
	}
	s.runner(job.ID, "cron")
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
