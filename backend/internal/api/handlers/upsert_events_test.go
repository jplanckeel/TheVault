package handlers_test

import (
	"context"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"github.com/jplanckeel/thevaultbo/internal/api/handlers"
	"github.com/jplanckeel/thevaultbo/internal/domain"
	"github.com/jplanckeel/thevaultbo/internal/store"
)

// tombstoneStore rejects every upsert, like an item deleted by an admin.
type tombstoneStore struct{ *mockItemStore }

func (tombstoneStore) Upsert(context.Context, domain.Item) (domain.Item, error) {
	return domain.Item{}, store.ErrTombstoned
}

// datedStore stamps items like Postgres: created_at == updated_at on insert only.
type datedStore struct {
	*mockItemStore
	seen map[string]time.Time
}

func (d *datedStore) Upsert(ctx context.Context, item domain.Item) (domain.Item, error) {
	now := time.Now()
	created, ok := d.seen[item.ExternalID]
	if !ok {
		created = now
		d.seen[item.ExternalID] = now
	}
	item.ID, item.CreatedAt, item.UpdatedAt = item.ExternalID, created, now
	return item, nil
}

func postItem(h *handlers.ItemsHandler, source string) *httptest.ResponseRecorder {
	req := httptest.NewRequest(http.MethodPost, "/items", strings.NewReader(`{"external_id":"halo-xbox","title":"Halo","type":"game"}`))
	req = req.WithContext(handlers.WithCaller(req.Context(), handlers.Caller{Source: source, Actor: "api-key"}))
	rec := httptest.NewRecorder()
	h.UpsertItem(rec, req)
	return rec
}

func TestUpsertItem_RecordsCreatedThenUpdated(t *testing.T) {
	events := &memEvents{}
	h := handlers.NewItemsHandler(&datedStore{newMockItemStore(), map[string]time.Time{}}, &mockPriceStore{}).WithSyncEvents(events)

	postItem(h, domain.SourceMobile)
	time.Sleep(time.Millisecond)
	postItem(h, domain.SourceMobile)

	if len(events.events) != 2 {
		t.Fatalf("events = %+v", events.events)
	}
	if events.events[0].Action != domain.ActionCreated || events.events[1].Action != domain.ActionUpdated {
		t.Errorf("actions = %s, %s", events.events[0].Action, events.events[1].Action)
	}
	if events.events[0].Source != domain.SourceMobile {
		t.Errorf("source = %s, want mobile", events.events[0].Source)
	}
}

func TestUpsertItem_RejectsTombstonedItem(t *testing.T) {
	events := &memEvents{}
	h := handlers.NewItemsHandler(tombstoneStore{newMockItemStore()}, &mockPriceStore{}).WithSyncEvents(events)

	rec := postItem(h, domain.SourceMobile)
	if rec.Code != http.StatusGone {
		t.Fatalf("status = %d, want 410", rec.Code)
	}
	if len(events.events) != 1 || events.events[0].Action != domain.ActionRejected {
		t.Errorf("events = %+v", events.events)
	}
}
