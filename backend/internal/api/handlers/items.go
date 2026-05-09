package handlers

import (
	"encoding/json"
	"errors"
	"net/http"

	"github.com/go-chi/chi/v5"
	"github.com/jplanckeel/thevaultbo/internal/domain"
	"github.com/jplanckeel/thevaultbo/internal/store"
)

// ItemsHandler handles HTTP requests for items.
type ItemsHandler struct {
	items  store.ItemStore
	prices store.PriceStore
}

// NewItemsHandler creates a new ItemsHandler.
func NewItemsHandler(items store.ItemStore, prices store.PriceStore) *ItemsHandler {
	return &ItemsHandler{items: items, prices: prices}
}

// UpsertItem handles POST /api/v1/items — upsert from mobile app.
func (h *ItemsHandler) UpsertItem(w http.ResponseWriter, r *http.Request) {
	var item domain.Item
	if err := json.NewDecoder(r.Body).Decode(&item); err != nil {
		http.Error(w, "invalid body", http.StatusBadRequest)
		return
	}
	if item.ExternalID == "" {
		http.Error(w, "external_id is required", http.StatusBadRequest)
		return
	}

	saved, err := h.items.Upsert(r.Context(), item)
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(saved)
}

// ListItems handles GET /api/v1/items.
func (h *ItemsHandler) ListItems(w http.ResponseWriter, r *http.Request) {
	items, err := h.items.List(r.Context())
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}
	if items == nil {
		items = []domain.Item{}
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(items)
}

// GetItem handles GET /api/v1/items/{id}.
func (h *ItemsHandler) GetItem(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")
	item, err := h.items.GetByID(r.Context(), id)
	if err != nil {
		if errors.Is(err, store.ErrNotFound) {
			http.Error(w, "not found", http.StatusNotFound)
		} else {
			http.Error(w, "internal error", http.StatusInternalServerError)
		}
		return
	}
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(item)
}

// GetItemPrices handles GET /api/v1/items/{id}/prices.
func (h *ItemsHandler) GetItemPrices(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")
	snaps, err := h.prices.ListByItemID(r.Context(), id)
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}
	if snaps == nil {
		snaps = []domain.PriceSnapshot{}
	}
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(snaps)
}

// UpdateCover handles PUT /api/v1/items/{id}/cover — manual cover URL update.
func (h *ItemsHandler) UpdateCover(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")
	var body struct {
		CoverURL string `json:"cover_url"`
	}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil || body.CoverURL == "" {
		http.Error(w, "cover_url is required", http.StatusBadRequest)
		return
	}

	if err := h.items.UpdateCoverURL(r.Context(), id, body.CoverURL); err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]string{"cover_url": body.CoverURL})
}
