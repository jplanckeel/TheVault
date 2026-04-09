package handlers_test

import (
	"bytes"
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/go-chi/chi/v5"
	"github.com/jplanckeel/thevaultbo/internal/api/handlers"
	"github.com/jplanckeel/thevaultbo/internal/domain"
	"github.com/jplanckeel/thevaultbo/internal/store"
)


// mockItemStore is an in-memory ItemStore for testing.
type mockItemStore struct {
	items map[string]domain.Item
}

func newMockItemStore() *mockItemStore {
	return &mockItemStore{items: make(map[string]domain.Item)}
}

func (m *mockItemStore) Upsert(_ context.Context, item domain.Item) (domain.Item, error) {
	if item.ID == "" {
		item.ID = item.ExternalID
	}
	m.items[item.ExternalID] = item
	return item, nil
}

func (m *mockItemStore) List(_ context.Context) ([]domain.Item, error) {
	result := make([]domain.Item, 0, len(m.items))
	for _, v := range m.items {
		result = append(result, v)
	}
	return result, nil
}

func (m *mockItemStore) GetByID(_ context.Context, id string) (domain.Item, error) {
	for _, v := range m.items {
		if v.ID == id {
			return v, nil
		}
	}
	return domain.Item{}, store.ErrNotFound
}

// mockPriceStore is a minimal PriceStore for testing items handler.
type mockPriceStore struct{}

func (m *mockPriceStore) Insert(_ context.Context, s domain.PriceSnapshot) (domain.PriceSnapshot, error) {
	return s, nil
}
func (m *mockPriceStore) ListByItemID(_ context.Context, _ string) ([]domain.PriceSnapshot, error) {
	return []domain.PriceSnapshot{}, nil
}
func (m *mockPriceStore) LatestPerItem(_ context.Context) ([]domain.PriceSnapshot, error) {
	return []domain.PriceSnapshot{}, nil
}

func TestUpsertItem_Returns201(t *testing.T) {
	store := newMockItemStore()
	h := handlers.NewItemsHandler(store, &mockPriceStore{})

	body, _ := json.Marshal(domain.Item{
		ExternalID: "ext-001",
		Type:       domain.ItemTypeGame,
		Title:      "Super Mario 64",
		Platform:   "Nintendo 64",
	})

	req := httptest.NewRequest(http.MethodPost, "/api/v1/items", bytes.NewReader(body))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()

	h.UpsertItem(w, req)

	if w.Code != http.StatusCreated {
		t.Errorf("want 201, got %d", w.Code)
	}
	var resp domain.Item
	if err := json.NewDecoder(w.Body).Decode(&resp); err != nil {
		t.Fatalf("decode response: %v", err)
	}
	if resp.Title != "Super Mario 64" {
		t.Errorf("want title 'Super Mario 64', got %q", resp.Title)
	}
}

func TestUpsertItem_MissingExternalID_Returns400(t *testing.T) {
	h := handlers.NewItemsHandler(newMockItemStore(), &mockPriceStore{})
	body, _ := json.Marshal(domain.Item{Title: "No external ID"})
	req := httptest.NewRequest(http.MethodPost, "/api/v1/items", bytes.NewReader(body))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()

	h.UpsertItem(w, req)

	if w.Code != http.StatusBadRequest {
		t.Errorf("want 400, got %d", w.Code)
	}
}

func TestListItems_ReturnsJSON(t *testing.T) {
	store := newMockItemStore()
	store.items["ext-001"] = domain.Item{ID: "1", ExternalID: "ext-001", Title: "Zelda"}

	h := handlers.NewItemsHandler(store, &mockPriceStore{})
	req := httptest.NewRequest(http.MethodGet, "/api/v1/items", nil)
	w := httptest.NewRecorder()

	h.ListItems(w, req)

	if w.Code != http.StatusOK {
		t.Errorf("want 200, got %d", w.Code)
	}
	var items []domain.Item
	if err := json.NewDecoder(w.Body).Decode(&items); err != nil {
		t.Fatalf("decode response: %v", err)
	}
	if len(items) != 1 {
		t.Errorf("want 1 item, got %d", len(items))
	}
}

func chiRequest(method, path, id string, body *bytes.Reader) *http.Request {
	var req *http.Request
	if body != nil {
		req = httptest.NewRequest(method, path, body)
	} else {
		req = httptest.NewRequest(method, path, nil)
	}
	rctx := chi.NewRouteContext()
	rctx.URLParams.Add("id", id)
	return req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))
}

func TestGetItem_NotFound_Returns404(t *testing.T) {
	h := handlers.NewItemsHandler(newMockItemStore(), &mockPriceStore{})
	req := chiRequest(http.MethodGet, "/api/v1/items/nonexistent", "nonexistent", nil)
	w := httptest.NewRecorder()

	h.GetItem(w, req)

	if w.Code != http.StatusNotFound {
		t.Errorf("want 404, got %d", w.Code)
	}
}

func TestGetItem_Found_Returns200(t *testing.T) {
	s := newMockItemStore()
	s.items["ext-1"] = domain.Item{ID: "item-1", ExternalID: "ext-1", Title: "Metroid"}
	h := handlers.NewItemsHandler(s, &mockPriceStore{})
	req := chiRequest(http.MethodGet, "/api/v1/items/item-1", "item-1", nil)
	w := httptest.NewRecorder()

	h.GetItem(w, req)

	if w.Code != http.StatusOK {
		t.Errorf("want 200, got %d", w.Code)
	}
	var resp domain.Item
	if err := json.NewDecoder(w.Body).Decode(&resp); err != nil {
		t.Fatalf("decode: %v", err)
	}
	if resp.Title != "Metroid" {
		t.Errorf("want 'Metroid', got %q", resp.Title)
	}
}

func TestGetItemPrices_Returns200(t *testing.T) {
	h := handlers.NewItemsHandler(newMockItemStore(), &mockPriceStore{})
	req := chiRequest(http.MethodGet, "/api/v1/items/any/prices", "any", nil)
	w := httptest.NewRecorder()

	h.GetItemPrices(w, req)

	if w.Code != http.StatusOK {
		t.Errorf("want 200, got %d", w.Code)
	}
	var snaps []domain.PriceSnapshot
	if err := json.NewDecoder(w.Body).Decode(&snaps); err != nil {
		t.Fatalf("decode: %v", err)
	}
}
