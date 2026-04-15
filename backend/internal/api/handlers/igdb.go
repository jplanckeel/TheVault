package handlers

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"time"

	"github.com/jplanckeel/thevaultbo/internal/domain"
	"github.com/jplanckeel/thevaultbo/internal/igdb"
	"github.com/jplanckeel/thevaultbo/internal/store"
)

// IGDBHandler handles IGDB enrichment requests.
type IGDBHandler struct {
	items  store.ItemStore
	jobs   store.JobStore
	client *igdb.Client
}

// NewIGDBHandler creates a new IGDBHandler.
func NewIGDBHandler(items store.ItemStore, jobs store.JobStore, client *igdb.Client) *IGDBHandler {
	return &IGDBHandler{items: items, jobs: jobs, client: client}
}

// SyncIGDB handles POST /api/v1/items/sync-igdb.
// Creates a tracked job and enriches all game items missing IGDB data in the background.
func (h *IGDBHandler) SyncIGDB(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()

	items, err := h.items.ListWithoutIGDB(ctx)
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}

	job, err := h.jobs.Create(ctx, domain.ScrapeJob{
		JobType:     "igdb",
		TriggeredBy: "user",
		Status:      "running",
		ItemsTotal:  len(items),
		ItemsDone:   0,
	})
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusAccepted)
	json.NewEncoder(w).Encode(map[string]any{
		"id":      job.ID,
		"message": fmt.Sprintf("IGDB sync démarré pour %d jeu(x)", len(items)),
		"total":   len(items),
	})

	go func() {
		bgCtx := context.Background()
		done := 0
		var lastErr string

		for _, item := range items {
			data, err := h.client.SearchGame(item.Title)
			if err != nil {
				fmt.Printf("igdb sync: %s — error: %v\n", item.Title, err)
				lastErr = err.Error()
			} else if data == nil {
				fmt.Printf("igdb sync: %s — no result\n", item.Title)
			} else {
				if err := h.items.UpdateIGDB(bgCtx, item.ID, *data); err != nil {
					fmt.Printf("igdb sync: %s — db error: %v\n", item.Title, err)
					lastErr = err.Error()
				} else {
					fmt.Printf("igdb sync: %s — OK (igdb_id=%s)\n", item.Title, data.IgdbID)
				}
			}

			done++
			_ = h.jobs.Update(bgCtx, domain.ScrapeJob{
				ID:         job.ID,
				Status:     "running",
				ItemsTotal: len(items),
				ItemsDone:  done,
				Error:      lastErr,
			})

			time.Sleep(350 * time.Millisecond)
		}

		now := time.Now()
		status := "done"
		if lastErr != "" {
			status = "done" // partial errors are non-fatal
		}
		_ = h.jobs.Update(bgCtx, domain.ScrapeJob{
			ID:         job.ID,
			Status:     status,
			ItemsTotal: len(items),
			ItemsDone:  done,
			Error:      lastErr,
			FinishedAt: &now,
		})
		fmt.Printf("igdb sync: finished (%d/%d)\n", done, len(items))
	}()
}
