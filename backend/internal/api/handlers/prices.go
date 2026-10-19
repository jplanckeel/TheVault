package handlers

import (
	"encoding/json"
	"net/http"
	"strconv"

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

// GetTopMovers handles GET /api/v1/prices/top-movers?days=30&limit=3.
func (h *PricesHandler) GetTopMovers(w http.ResponseWriter, r *http.Request) {
	days, _ := strconv.Atoi(r.URL.Query().Get("days"))
	if days <= 0 {
		days = 30
	}
	limit, _ := strconv.Atoi(r.URL.Query().Get("limit"))
	if limit <= 0 {
		limit = 3
	}
	movers, err := h.prices.TopMovers(r.Context(), days, limit)
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}
	if movers == nil {
		movers = []domain.TopMover{}
	}
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(movers)
}
