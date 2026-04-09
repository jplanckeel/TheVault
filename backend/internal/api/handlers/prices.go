package handlers

import (
	"encoding/json"
	"net/http"

	"github.com/jplanckeel/thevaultbo/internal/domain"
	"github.com/jplanckeel/thevaultbo/internal/store"
)

// PricesHandler handles HTTP requests for price snapshots.
type PricesHandler struct {
	prices store.PriceStore
}

// NewPricesHandler creates a new PricesHandler.
func NewPricesHandler(prices store.PriceStore) *PricesHandler {
	return &PricesHandler{prices: prices}
}

// GetLatestPrices handles GET /api/v1/prices/latest — returns the latest snapshot per item.
func (h *PricesHandler) GetLatestPrices(w http.ResponseWriter, r *http.Request) {
	snaps, err := h.prices.LatestPerItem(r.Context())
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
