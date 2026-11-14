package handlers

import (
	"encoding/json"
	"errors"
	"io"
	"net/http"

	"github.com/go-chi/chi/v5"
	"github.com/jplanckeel/thevaultbo/internal/domain"
	"github.com/jplanckeel/thevaultbo/internal/store"
)

// ScrapeHandler handles HTTP requests for scrape jobs.
type ScrapeHandler struct {
	jobs   store.JobStore
	runner func(jobID, triggeredBy string, itemIDs ...string)
}

// NewScrapeHandler creates a new ScrapeHandler.
func NewScrapeHandler(jobs store.JobStore, runner func(jobID, triggeredBy string, itemIDs ...string)) *ScrapeHandler {
	return &ScrapeHandler{jobs: jobs, runner: runner}
}

// TriggerScrape handles POST /api/v1/scrape — starts a scrape job asynchronously.
func (h *ScrapeHandler) TriggerScrape(w http.ResponseWriter, r *http.Request) {
	var req struct {
		TriggeredBy string   `json:"triggered_by"`
		ItemIDs     []string `json:"item_ids"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil && !errors.Is(err, io.EOF) {
		http.Error(w, "invalid body", http.StatusBadRequest)
		return
	}

	job, err := h.jobs.Create(r.Context(), domain.ScrapeJob{
		JobType:     "ebay",
		TriggeredBy: req.TriggeredBy,
		Status:      "pending",
	})
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}

	go h.runner(job.ID, req.TriggeredBy, req.ItemIDs...)

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusAccepted)
	_ = json.NewEncoder(w).Encode(job)
}

// ListJobs handles GET /api/v1/scrape — lists all scrape jobs.
func (h *ScrapeHandler) ListJobs(w http.ResponseWriter, r *http.Request) {
	jobs, err := h.jobs.List(r.Context())
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}
	if jobs == nil {
		jobs = []domain.ScrapeJob{}
	}
	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(jobs)
}

// GetJob handles GET /api/v1/scrape/{id} — returns a single scrape job.
func (h *ScrapeHandler) GetJob(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")
	job, err := h.jobs.GetByID(r.Context(), id)
	if err != nil {
		if errors.Is(err, store.ErrNotFound) {
			http.Error(w, "not found", http.StatusNotFound)
		} else {
			http.Error(w, "internal error", http.StatusInternalServerError)
		}
		return
	}
	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(job)
}
