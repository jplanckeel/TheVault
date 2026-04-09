package handlers_test

import (
	"bytes"
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/jplanckeel/thevaultbo/internal/api/handlers"
	"github.com/jplanckeel/thevaultbo/internal/domain"
	"github.com/jplanckeel/thevaultbo/internal/store"
)

// mockJobStore is an in-memory JobStore for testing.
type mockJobStore struct {
	jobs map[string]domain.ScrapeJob
}

func newMockJobStore() *mockJobStore {
	return &mockJobStore{jobs: make(map[string]domain.ScrapeJob)}
}

func (m *mockJobStore) Create(_ context.Context, job domain.ScrapeJob) (domain.ScrapeJob, error) {
	job.ID = "job-1"
	m.jobs[job.ID] = job
	return job, nil
}

func (m *mockJobStore) Update(_ context.Context, job domain.ScrapeJob) error {
	if _, ok := m.jobs[job.ID]; !ok {
		return store.ErrNotFound
	}
	m.jobs[job.ID] = job
	return nil
}

func (m *mockJobStore) GetByID(_ context.Context, id string) (domain.ScrapeJob, error) {
	j, ok := m.jobs[id]
	if !ok {
		return domain.ScrapeJob{}, store.ErrNotFound
	}
	return j, nil
}

func (m *mockJobStore) List(_ context.Context) ([]domain.ScrapeJob, error) {
	result := make([]domain.ScrapeJob, 0, len(m.jobs))
	for _, v := range m.jobs {
		result = append(result, v)
	}
	return result, nil
}

func TestTriggerScrape_Returns202(t *testing.T) {
	ranCh := make(chan struct{}, 1)
	runner := func(triggeredBy string, itemIDs ...string) {
		ranCh <- struct{}{}
	}
	h := handlers.NewScrapeHandler(newMockJobStore(), runner)
	body, _ := json.Marshal(map[string]any{"triggered_by": "test"})
	req := httptest.NewRequest(http.MethodPost, "/api/v1/scrape", bytes.NewReader(body))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()

	h.TriggerScrape(w, req)

	if w.Code != http.StatusAccepted {
		t.Errorf("want 202, got %d", w.Code)
	}
	var resp domain.ScrapeJob
	if err := json.NewDecoder(w.Body).Decode(&resp); err != nil {
		t.Fatalf("decode: %v", err)
	}
	if resp.ID == "" {
		t.Error("want job ID in response")
	}
}

func TestListJobs_Returns200(t *testing.T) {
	js := newMockJobStore()
	js.jobs["job-1"] = domain.ScrapeJob{ID: "job-1", TriggeredBy: "user", Status: "done"}
	h := handlers.NewScrapeHandler(js, func(string, ...string) {})
	req := httptest.NewRequest(http.MethodGet, "/api/v1/scrape", nil)
	w := httptest.NewRecorder()

	h.ListJobs(w, req)

	if w.Code != http.StatusOK {
		t.Errorf("want 200, got %d", w.Code)
	}
	var jobs []domain.ScrapeJob
	if err := json.NewDecoder(w.Body).Decode(&jobs); err != nil {
		t.Fatalf("decode: %v", err)
	}
	if len(jobs) != 1 {
		t.Errorf("want 1 job, got %d", len(jobs))
	}
}

func TestGetJob_NotFound_Returns404(t *testing.T) {
	h := handlers.NewScrapeHandler(newMockJobStore(), func(string, ...string) {})
	req := chiRequest(http.MethodGet, "/api/v1/scrape/nonexistent", "nonexistent", nil)
	w := httptest.NewRecorder()

	h.GetJob(w, req)

	if w.Code != http.StatusNotFound {
		t.Errorf("want 404, got %d", w.Code)
	}
}

func TestGetJob_Found_Returns200(t *testing.T) {
	js := newMockJobStore()
	js.jobs["job-1"] = domain.ScrapeJob{ID: "job-1", TriggeredBy: "user", Status: "done"}
	h := handlers.NewScrapeHandler(js, func(string, ...string) {})
	req := chiRequest(http.MethodGet, "/api/v1/scrape/job-1", "job-1", nil)
	w := httptest.NewRecorder()

	h.GetJob(w, req)

	if w.Code != http.StatusOK {
		t.Errorf("want 200, got %d", w.Code)
	}
}
