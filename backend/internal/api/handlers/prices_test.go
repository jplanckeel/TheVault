package handlers_test

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/jplanckeel/thevaultbo/internal/api/handlers"
	"github.com/jplanckeel/thevaultbo/internal/domain"
)

func TestGetLatestPrices_Returns200(t *testing.T) {
	h := handlers.NewPricesHandler(&mockPriceStore{})
	req := httptest.NewRequest(http.MethodGet, "/api/v1/prices/latest", nil)
	w := httptest.NewRecorder()

	h.GetLatestPrices(w, req)

	if w.Code != http.StatusOK {
		t.Errorf("want 200, got %d", w.Code)
	}
	var snaps []domain.PriceSnapshot
	if err := json.NewDecoder(w.Body).Decode(&snaps); err != nil {
		t.Fatalf("decode: %v", err)
	}
}
