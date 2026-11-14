package handlers

import (
	"encoding/json"
	"net/http"

	"github.com/go-chi/chi/v5"
	"github.com/jplanckeel/thevaultbo/internal/domain"
	"github.com/jplanckeel/thevaultbo/internal/store"
)

// ConsolesHandler handles HTTP requests for consoles.
type ConsolesHandler struct {
	items store.ItemStore
}

// NewConsolesHandler creates a new ConsolesHandler.
func NewConsolesHandler(items store.ItemStore) *ConsolesHandler {
	return &ConsolesHandler{items: items}
}

// ListConsoles handles GET /api/v1/consoles — returns all console items.
func (h *ConsolesHandler) ListConsoles(w http.ResponseWriter, r *http.Request) {
	items, err := h.items.List(r.Context())
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}
	if items == nil {
		items = []domain.Item{}
	}

	// Filter only consoles
	consoles := make([]domain.Item, 0)
	for _, item := range items {
		if item.Type == "console" {
			consoles = append(consoles, item)
		}
	}

	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(consoles)
}

// GetConsole handles GET /api/v1/consoles/{id} — returns a single console.
func (h *ConsolesHandler) GetConsole(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")
	item, err := h.items.GetByID(r.Context(), id)
	if err != nil {
		if err == store.ErrNotFound {
			http.Error(w, "not found", http.StatusNotFound)
		} else {
			http.Error(w, "internal error", http.StatusInternalServerError)
		}
		return
	}

	// Verify it's a console
	if item.Type != "console" {
		http.Error(w, "not a console", http.StatusNotFound)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(item)
}

// GetConsoleGames handles GET /api/v1/consoles/{id}/games — returns games for a console.
func (h *ConsolesHandler) GetConsoleGames(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")
	item, err := h.items.GetByID(r.Context(), id)
	if err != nil {
		if err == store.ErrNotFound {
			http.Error(w, "console not found", http.StatusNotFound)
		} else {
			http.Error(w, "internal error", http.StatusInternalServerError)
		}
		return
	}

	// Get all items
	allItems, err := h.items.List(r.Context())
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}

	// Filter games for this console
	games := make([]domain.Item, 0)
	for _, i := range allItems {
		if i.Type == "game" && i.Platform == item.Platform {
			games = append(games, i)
		}
	}

	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(games)
}
