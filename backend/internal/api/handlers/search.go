package handlers

import (
	"encoding/json"
	"net/http"

	"github.com/jplanckeel/thevaultbo/internal/igdb"
)

// SearchHandler handles game search via IGDB.
type SearchHandler struct {
	client *igdb.Client
}

// NewSearchHandler creates a new SearchHandler.
func NewSearchHandler(client *igdb.Client) *SearchHandler {
	return &SearchHandler{client: client}
}

// Search handles GET /api/v1/search?q=<query>.
func (h *SearchHandler) Search(w http.ResponseWriter, r *http.Request) {
	q := r.URL.Query().Get("q")
	if q == "" {
		http.Error(w, "q parameter is required", http.StatusBadRequest)
		return
	}

	results, err := h.client.SearchGames(q)
	if err != nil {
		http.Error(w, "search failed", http.StatusInternalServerError)
		return
	}
	if results == nil {
		results = []igdb.SearchGameResult{}
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(results)
}
