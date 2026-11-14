package handlers

import (
	"encoding/json"
	"net/http"

	"github.com/go-chi/chi/v5"
	"github.com/jplanckeel/thevaultbo/internal/domain"
	"github.com/jplanckeel/thevaultbo/internal/store"
)

type AccessoriesHandler struct {
	items store.ItemStore
}

func NewAccessoriesHandler(items store.ItemStore) *AccessoriesHandler {
	return &AccessoriesHandler{items: items}
}

func (h *AccessoriesHandler) ListAccessories(w http.ResponseWriter, r *http.Request) {
	items, err := h.items.List(r.Context())
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}
	if items == nil {
		items = []domain.Item{}
	}

	accessories := make([]domain.Item, 0)
	for _, item := range items {
		if item.Type == "accessory" {
			accessories = append(accessories, item)
		}
	}

	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(accessories)
}

func (h *AccessoriesHandler) GetAccessory(w http.ResponseWriter, r *http.Request) {
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

	if item.Type != "accessory" {
		http.Error(w, "not an accessory", http.StatusNotFound)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(item)
}
