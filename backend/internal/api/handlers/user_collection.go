package handlers

import (
	"encoding/json"
	"net/http"

	"github.com/go-chi/chi/v5"
	"github.com/jplanckeel/thevaultbo/internal/store"
)

// UserCollectionHandler handles user collection operations.
type UserCollectionHandler struct {
	collections store.UserCollectionStore
}

func NewUserCollectionHandler(collections store.UserCollectionStore) *UserCollectionHandler {
	return &UserCollectionHandler{collections: collections}
}

func (h *UserCollectionHandler) AddItem(w http.ResponseWriter, r *http.Request) {
	userID := r.Context().Value(UserIDKey).(string)
	var body struct {
		ItemID string `json:"item_id"`
		Status string `json:"status"`
	}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil || body.ItemID == "" {
		http.Error(w, "item_id required", http.StatusBadRequest)
		return
	}
	if body.Status == "" {
		body.Status = "owned"
	}
	if err := h.collections.Add(r.Context(), userID, body.ItemID, body.Status); err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func (h *UserCollectionHandler) RemoveItem(w http.ResponseWriter, r *http.Request) {
	userID := r.Context().Value(UserIDKey).(string)
	itemID := chi.URLParam(r, "itemId")
	if err := h.collections.Remove(r.Context(), userID, itemID); err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func (h *UserCollectionHandler) ListCollection(w http.ResponseWriter, r *http.Request) {
	userID := r.Context().Value(UserIDKey).(string)
	items, err := h.collections.ListByUser(r.Context(), userID, "owned")
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(items)
}

func (h *UserCollectionHandler) ListWishlist(w http.ResponseWriter, r *http.Request) {
	userID := r.Context().Value(UserIDKey).(string)
	items, err := h.collections.ListByUser(r.Context(), userID, "wishlist")
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(items)
}
