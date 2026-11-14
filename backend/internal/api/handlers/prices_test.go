package handlers_test

import (
	"context"
	"encoding/json"
	"errors"
	"net/http"
	"net/http/httptest"
	"strings"
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

func (e *errPriceStore) QuotesByExternalIDs(_ context.Context, _ []string, _ int) ([]domain.ExternalQuote, error) {
	return nil, errors.New("db error")
}
func (e *errPriceStore) HistoryByExternalID(_ context.Context, _ string, _ int) ([]domain.PriceSnapshot, error) {
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

func TestGetQuotes_ValidRequest_Returns200(t *testing.T) {
	h := handlers.NewPricesHandler(&mockPriceStore{})
	req := httptest.NewRequest(http.MethodPost, "/api/v1/prices/quotes", strings.NewReader(`{"external_ids":["a","b"]}`))
	w := httptest.NewRecorder()

	h.GetQuotes(w, req)

	if w.Code != http.StatusOK {
		t.Fatalf("want 200, got %d", w.Code)
	}
	var quotes []domain.ExternalQuote
	if err := json.NewDecoder(w.Body).Decode(&quotes); err != nil {
		t.Fatalf("decode: %v", err)
	}
}

func TestGetQuotes_InvalidBodies_Return400(t *testing.T) {
	tooMany := `{"external_ids":[` + strings.Repeat(`"x",`, domain.MaxQuoteIDs) + `"x"]}`
	for name, body := range map[string]string{
		"not json": `nope`,
		"empty":    `{"external_ids":[]}`,
		"blank id": `{"external_ids":["  "]}`,
		"too many": tooMany,
	} {
		t.Run(name, func(t *testing.T) {
			h := handlers.NewPricesHandler(&mockPriceStore{})
			req := httptest.NewRequest(http.MethodPost, "/api/v1/prices/quotes", strings.NewReader(body))
			w := httptest.NewRecorder()
			h.GetQuotes(w, req)
			if w.Code != http.StatusBadRequest {
				t.Errorf("want 400, got %d", w.Code)
			}
		})
	}
}

func TestGetQuotes_StoreError_Returns500(t *testing.T) {
	h := handlers.NewPricesHandler(&errPriceStore{})
	req := httptest.NewRequest(http.MethodPost, "/api/v1/prices/quotes", strings.NewReader(`{"external_ids":["a"]}`))
	w := httptest.NewRecorder()

	h.GetQuotes(w, req)

	if w.Code != http.StatusInternalServerError {
		t.Errorf("want 500, got %d", w.Code)
	}
}

func TestGetHistory_MissingID_Returns400(t *testing.T) {
	h := handlers.NewPricesHandler(&mockPriceStore{})
	req := httptest.NewRequest(http.MethodGet, "/api/v1/prices/history", nil)
	w := httptest.NewRecorder()

	h.GetHistory(w, req)

	if w.Code != http.StatusBadRequest {
		t.Errorf("want 400, got %d", w.Code)
	}
}

func TestGetHistory_Returns200(t *testing.T) {
	h := handlers.NewPricesHandler(&mockPriceStore{})
	req := httptest.NewRequest(http.MethodGet, "/api/v1/prices/history?external_id=a&days=99999", nil)
	w := httptest.NewRecorder()

	h.GetHistory(w, req)

	if w.Code != http.StatusOK {
		t.Errorf("want 200, got %d", w.Code)
	}
}

func TestClampDays(t *testing.T) {
	cases := []struct{ in, def, want int }{{0, 30, 30}, {-4, 30, 30}, {12, 30, 12}, {99999, 30, domain.MaxHistoryDays}}
	for _, c := range cases {
		if got := domain.ClampDays(c.in, c.def); got != c.want {
			t.Errorf("ClampDays(%d, %d) = %d, want %d", c.in, c.def, got, c.want)
		}
	}
}
