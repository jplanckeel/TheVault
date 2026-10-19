package handlers_test

import (
	"context"
	"encoding/json"
	"errors"
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

// errPriceStore returns an error from LatestPerItem.
type errPriceStore struct{}

func (e *errPriceStore) Insert(_ context.Context, s domain.PriceSnapshot) (domain.PriceSnapshot, error) {
	return s, nil
}
func (e *errPriceStore) ListByItemID(_ context.Context, _ string) ([]domain.PriceSnapshot, error) {
	return nil, nil
}
func (e *errPriceStore) LatestPerItem(_ context.Context) ([]domain.PriceSnapshot, error) {
	return nil, errors.New("db error")
}
func (e *errPriceStore) TopMovers(_ context.Context, _, _ int) ([]domain.TopMover, error) {
	return nil, errors.New("db error")
}

func TestGetLatestPrices_StoreError_Returns500(t *testing.T) {
	h := handlers.NewPricesHandler(&errPriceStore{})
	req := httptest.NewRequest(http.MethodGet, "/api/v1/prices/latest", nil)
	w := httptest.NewRecorder()

	h.GetLatestPrices(w, req)

	if w.Code != http.StatusInternalServerError {
		t.Errorf("want 500, got %d", w.Code)
	}
}
