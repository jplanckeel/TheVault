package handlers

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"

	"github.com/jplanckeel/thevaultbo/internal/igdb"
)

// IGDBHandler handles IGDB enrichment requests.
type IGDBHandler struct {
	runner *igdb.Runner
}

// NewIGDBHandler creates a new IGDBHandler.
func NewIGDBHandler(runner *igdb.Runner) *IGDBHandler {
	return &IGDBHandler{runner: runner}
}

// SyncIGDB handles POST /api/v1/items/sync-igdb.
// Creates a tracked job and enriches all game items missing IGDB data in the background.
func (h *IGDBHandler) SyncIGDB(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()

	job, items, err := h.runner.Prepare(ctx, "user")
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusAccepted)
	_ = json.NewEncoder(w).Encode(map[string]any{
		"id":      job.ID,
		"message": fmt.Sprintf("IGDB sync démarré pour %d jeu(x)", len(items)),
		"total":   len(items),
	})

	go h.runner.Sync(context.WithoutCancel(ctx), job, items)
}
