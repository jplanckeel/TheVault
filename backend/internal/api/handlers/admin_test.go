package handlers_test

import (
	"context"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/jplanckeel/thevaultbo/internal/api/handlers"
	"github.com/jplanckeel/thevaultbo/internal/domain"
	"github.com/jplanckeel/thevaultbo/internal/store"
)

// memEvents records sync events in memory.
type memEvents struct{ events []domain.SyncEvent }

func (m *memEvents) Record(_ context.Context, e domain.SyncEvent) error {
	m.events = append(m.events, e)
	return nil
}
func (m *memEvents) List(_ context.Context, _ domain.SyncEventFilter) ([]domain.SyncEvent, error) {
	return m.events, nil
}
func (m *memEvents) Summary(_ context.Context, since time.Time) (domain.SyncSummary, error) {
	return domain.SyncSummary{Since: since}, nil
}

// memAdmin implements store.AdminItemStore on top of mockItemStore.
type memAdmin struct {
	items  *mockItemStore
	merged [2]string
}

func (m *memAdmin) Update(_ context.Context, id string, p domain.ItemPatch) (domain.Item, error) {
	for k, it := range m.items.items {
		if it.ID == id {
			if p.Title != nil {
				it.Title = *p.Title
			}
			it.AdminEdited = true
			m.items.items[k] = it
			return it, nil
		}
	}
	return domain.Item{}, store.ErrNotFound
}
func (m *memAdmin) Delete(_ context.Context, id, _ string) (domain.Item, error) {
	for k, it := range m.items.items {
		if it.ID == id {
			delete(m.items.items, k)
			return it, nil
		}
	}
	return domain.Item{}, store.ErrNotFound
}
func (m *memAdmin) Merge(_ context.Context, source, target string) (domain.Item, error) {
	m.merged = [2]string{source, target}
	return m.items.GetByID(context.Background(), target)
}
func (m *memAdmin) OwnerCounts(_ context.Context) (map[string]int, error) { return map[string]int{}, nil }
func (m *memAdmin) PricedItemIDs(_ context.Context) (map[string]bool, error) {
	return map[string]bool{}, nil
}

func newAdminFixture() (*handlers.AdminHandler, *memAdmin, *memEvents, http.Handler) {
	items := newMockItemStore()
	items.items["ext-1"] = domain.Item{ID: "1", ExternalID: "ext-1", Type: domain.ItemTypeGame, Title: "Halo"}
	items.items["ext-2"] = domain.Item{ID: "2", ExternalID: "ext-2", Type: domain.ItemTypeGame, Title: "halo"}
	admin := &memAdmin{items: items}
	events := &memEvents{}
	h := handlers.NewAdminHandler(items, admin, events, nil, nil)

	r := chi.NewRouter()
	r.Use(func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, req *http.Request) {
			ctx := handlers.WithCaller(req.Context(), handlers.Caller{Source: domain.SourceAdmin, Actor: "admin@test"})
			next.ServeHTTP(w, req.WithContext(ctx))
		})
	})
	r.Get("/admin/items", h.ListItems)
	r.Put("/admin/items/{id}", h.UpdateItem)
	r.Delete("/admin/items/{id}", h.DeleteItem)
	r.Post("/admin/items/{id}/merge", h.MergeItem)
	r.Post("/admin/items/{id}/igdb", h.RefreshIGDB)
	return h, admin, events, r
}

func do(t *testing.T, h http.Handler, method, path, body string) *httptest.ResponseRecorder {
	t.Helper()
	req := httptest.NewRequest(method, path, strings.NewReader(body))
	rec := httptest.NewRecorder()
	h.ServeHTTP(rec, req)
	return rec
}

func TestAdmin_ListItems_FlagsDuplicates(t *testing.T) {
	_, _, _, r := newAdminFixture()
	rec := do(t, r, http.MethodGet, "/admin/items", "")
	if rec.Code != http.StatusOK {
		t.Fatalf("status = %d", rec.Code)
	}
	if !strings.Contains(rec.Body.String(), `"duplicate"`) {
		t.Errorf("expected duplicate issue in %s", rec.Body.String())
	}
}

func TestAdmin_UpdateItem(t *testing.T) {
	_, _, events, r := newAdminFixture()

	if rec := do(t, r, http.MethodPut, "/admin/items/1", `{"title":"  "}`); rec.Code != http.StatusBadRequest {
		t.Errorf("empty title: status = %d, want 400", rec.Code)
	}
	if rec := do(t, r, http.MethodPut, "/admin/items/404", `{"title":"X"}`); rec.Code != http.StatusNotFound {
		t.Errorf("unknown item: status = %d, want 404", rec.Code)
	}
	rec := do(t, r, http.MethodPut, "/admin/items/1", `{"title":"Halo: Combat Evolved"}`)
	if rec.Code != http.StatusOK || !strings.Contains(rec.Body.String(), `"admin_edited":true`) {
		t.Fatalf("update: status = %d body = %s", rec.Code, rec.Body.String())
	}
	if len(events.events) != 1 || events.events[0].Action != domain.ActionEdited || events.events[0].Actor != "admin@test" {
		t.Errorf("audit log = %+v", events.events)
	}
}

func TestAdmin_DeleteItem(t *testing.T) {
	_, _, events, r := newAdminFixture()
	if rec := do(t, r, http.MethodDelete, "/admin/items/1", ""); rec.Code != http.StatusNoContent {
		t.Fatalf("status = %d", rec.Code)
	}
	if len(events.events) != 1 || events.events[0].Action != domain.ActionDeleted || events.events[0].ItemID != nil {
		t.Errorf("deleted event must not reference the removed row: %+v", events.events)
	}
}

func TestAdmin_MergeItem(t *testing.T) {
	_, admin, events, r := newAdminFixture()
	if rec := do(t, r, http.MethodPost, "/admin/items/1/merge", `{"target_id":"1"}`); rec.Code != http.StatusBadRequest {
		t.Errorf("self merge: status = %d, want 400", rec.Code)
	}
	rec := do(t, r, http.MethodPost, "/admin/items/2/merge", `{"target_id":"1"}`)
	if rec.Code != http.StatusOK {
		t.Fatalf("merge: status = %d", rec.Code)
	}
	if admin.merged != [2]string{"2", "1"} {
		t.Errorf("merged = %v", admin.merged)
	}
	if len(events.events) != 1 || !strings.Contains(events.events[0].Detail, "ext-2") {
		t.Errorf("merge event should mention the source: %+v", events.events)
	}
}

func TestAdmin_RefreshIGDB_NotConfigured(t *testing.T) {
	_, _, _, r := newAdminFixture()
	if rec := do(t, r, http.MethodPost, "/admin/items/1/igdb", ""); rec.Code != http.StatusNotImplemented {
		t.Errorf("status = %d, want 501", rec.Code)
	}
}
