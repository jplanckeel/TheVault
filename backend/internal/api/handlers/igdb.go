package handlers

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"time"

	"github.com/jplanckeel/thevaultbo/internal/igdb"
	"github.com/jplanckeel/thevaultbo/internal/store"
)

// IGDBHandler handles IGDB enrichment requests.
type IGDBHandler struct {
	items  store.ItemStore
	client *igdb.Client
}

// NewIGDBHandler creates a new IGDBHandler.
func NewIGDBHandler(items store.ItemStore, client *igdb.Client) *IGDBHandler {
	return &IGDBHandler{items: items, client: client}
}

type syncResponse struct {
	Enriched int    `json:"enriched"`
	Skipped  int    `json:"skipped"`
	Failed   int    `json:"failed"`
	Message  string `json:"message"`
}

// SyncIGDB handles POST /api/v1/items/sync-igdb.
// It enriches all game items that have no IGDB data yet.
// The operation runs asynchronously; the handler returns immediately with a 202.
func (h *IGDBHandler) SyncIGDB(w http.ResponseWriter, r *http.Request) {
	items, err := h.items.ListWithoutIGDB(r.Context())
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusAccepted)
	json.NewEncoder(w).Encode(map[string]any{
		"message": fmt.Sprintf("IGDB sync démarré pour %d jeu(x)", len(items)),
		"total":   len(items),
	})

	// Run enrichment in background
	go func() {
		ctx := context.Background()
		for _, item := range items {
			data, err := h.client.SearchGame(item.Title)
			if err != nil {
				fmt.Printf("igdb sync: %s — error: %v\n", item.Title, err)
				time.Sleep(350 * time.Millisecond)
				continue
			}
			if data == nil {
				fmt.Printf("igdb sync: %s — no result\n", item.Title)
				time.Sleep(350 * time.Millisecond)
				continue
			}
			if err := h.items.UpdateIGDB(ctx, item.ID, *data); err != nil {
				fmt.Printf("igdb sync: %s — db error: %v\n", item.Title, err)
			} else {
				fmt.Printf("igdb sync: %s — OK (igdb_id=%s)\n", item.Title, data.IgdbID)
			}
			time.Sleep(350 * time.Millisecond) // respect IGDB rate limit (~4 req/s)
		}
		fmt.Printf("igdb sync: done\n")
	}()
}
