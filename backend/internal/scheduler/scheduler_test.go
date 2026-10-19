package scheduler_test

import (
	"context"
	"testing"
	"time"

	"github.com/jplanckeel/thevaultbo/internal/domain"
	"github.com/jplanckeel/thevaultbo/internal/scheduler"
	"github.com/jplanckeel/thevaultbo/internal/store"
)

// memJobStore minimal in-memory implementation
type memJobStore struct {
	jobs map[string]domain.ScrapeJob
}

func newMemJobStore() *memJobStore { return &memJobStore{jobs: make(map[string]domain.ScrapeJob)} }

func (m *memJobStore) Create(_ context.Context, job domain.ScrapeJob) (domain.ScrapeJob, error) {
	job.ID = "job-sched-1"
	m.jobs[job.ID] = job
	return job, nil
}
func (m *memJobStore) Update(_ context.Context, job domain.ScrapeJob) error {
	m.jobs[job.ID] = job
	return nil
}
func (m *memJobStore) GetByID(_ context.Context, id string) (domain.ScrapeJob, error) {
	j, ok := m.jobs[id]
	if !ok {
		return domain.ScrapeJob{}, store.ErrNotFound
	}
	return j, nil
}
func (m *memJobStore) List(_ context.Context) ([]domain.ScrapeJob, error) {
	result := make([]domain.ScrapeJob, 0, len(m.jobs))
	for _, v := range m.jobs {
		result = append(result, v)
	}
	return result, nil
}

func TestScheduler_FiresRunner(t *testing.T) {
	ranCh := make(chan string, 1) // receives triggeredBy
	runner := func(jobID, triggeredBy string, itemIDs ...string) {
		ranCh <- triggeredBy
	}
	js := newMemJobStore()

	// Use "@every 50ms" so the test doesn't have to wait a full hour
	s := scheduler.New(js, runner, scheduler.WithSchedule("@every 50ms"))
	if err := s.Start(); err != nil {
		t.Fatalf("Start: %v", err)
	}
	defer s.Stop()

	select {
	case tb := <-ranCh:
		if tb != "cron" {
			t.Errorf("want triggeredBy 'cron', got %q", tb)
		}
	case <-time.After(500 * time.Millisecond):
		t.Fatal("runner was not called within 500ms")
	}
}

func TestScheduler_CreatesJob_BeforeCallingRunner(t *testing.T) {
	jobIDCh := make(chan string, 1)
	runner := func(jobID, triggeredBy string, itemIDs ...string) {
		jobIDCh <- jobID
	}
	js := newMemJobStore()

	s := scheduler.New(js, runner, scheduler.WithSchedule("@every 50ms"))
	if err := s.Start(); err != nil {
		t.Fatalf("Start: %v", err)
	}
	defer s.Stop()

	select {
	case jobID := <-jobIDCh:
		if jobID == "" {
			t.Error("want non-empty job ID passed to runner")
		}
		j, err := js.GetByID(context.Background(), jobID)
		if err != nil {
			t.Fatalf("job not found in store: %v", err)
		}
		if j.TriggeredBy != "cron" {
			t.Errorf("want TriggeredBy 'cron', got %q", j.TriggeredBy)
		}
	case <-time.After(500 * time.Millisecond):
		t.Fatal("runner was not called within 500ms")
	}
}

func TestScheduler_InvalidSchedule_ReturnsError(t *testing.T) {
	s := scheduler.New(newMemJobStore(), func(string, string, ...string) {},
		scheduler.WithSchedule("not-a-valid-cron"),
	)
	if err := s.Start(); err == nil {
		s.Stop()
		t.Error("want error for invalid cron expression, got nil")
	}
}

func TestScheduler_CronPath_FiresRunner(t *testing.T) {
	ranCh := make(chan string, 1)
	runner := func(jobID, triggeredBy string, itemIDs ...string) {
		ranCh <- triggeredBy
	}
	// "@every 1s" uses robfig/cron directly (>= 1 second)
	s := scheduler.New(newMemJobStore(), runner, scheduler.WithSchedule("@every 1s"))
	if err := s.Start(); err != nil {
		t.Fatalf("Start: %v", err)
	}
	defer s.Stop()

	select {
	case tb := <-ranCh:
		if tb != "cron" {
			t.Errorf("want triggeredBy 'cron', got %q", tb)
		}
	case <-time.After(3 * time.Second):
		t.Fatal("runner was not called within 3s via robfig/cron path")
	}
}
