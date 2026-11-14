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

// quotesRequest is the body of POST /api/v1/prices/quotes.
type quotesRequest struct {
	ExternalIDs []string `json:"external_ids"`
	Days        int      `json:"days"`
}

// GetQuotes handles POST /api/v1/prices/quotes — the latest eBay quote per condition of
// the items the app owns, with the median quoted `days` (default 30) ago.
func (h *PricesHandler) GetQuotes(w http.ResponseWriter, r *http.Request) {
	var req quotesRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "invalid body", http.StatusBadRequest)
		return
	}
	if err := domain.ValidateExternalIDs(req.ExternalIDs); err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}
	days := domain.ClampDays(req.Days, domain.DefaultQuoteDays)
	quotes, err := h.prices.QuotesByExternalIDs(r.Context(), req.ExternalIDs, days)
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}
	if quotes == nil {
		quotes = []domain.ExternalQuote{}
	}
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(quotes)
}

// GetHistory handles GET /api/v1/prices/history?external_id=…&days=365 — the price
// snapshots of one item, newest first.
func (h *PricesHandler) GetHistory(w http.ResponseWriter, r *http.Request) {
	externalID := r.URL.Query().Get("external_id")
	if err := domain.ValidateExternalIDs([]string{externalID}); err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}
	days, _ := strconv.Atoi(r.URL.Query().Get("days"))
	snaps, err := h.prices.HistoryByExternalID(r.Context(), externalID, domain.ClampDays(days, domain.MaxHistoryDays))
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
