package scraper_test

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/jplanckeel/thevaultbo/internal/domain"
	"github.com/jplanckeel/thevaultbo/internal/scraper"
	"github.com/jplanckeel/thevaultbo/internal/store"
)

// ---- minimal in-memory stores ----

type memItemStore struct{ items []domain.Item }

func (m *memItemStore) Upsert(_ context.Context, item domain.Item) (domain.Item, error) {
	m.items = append(m.items, item)
	return item, nil
}
func (m *memItemStore) List(_ context.Context) ([]domain.Item, error) { return m.items, nil }
func (m *memItemStore) GetByID(_ context.Context, id string) (domain.Item, error) {
	for _, it := range m.items {
		if it.ID == id {
			return it, nil
		}
	}
	return domain.Item{}, store.ErrNotFound
}

type memPriceStore struct{ snaps []domain.PriceSnapshot }

func (m *memPriceStore) Insert(_ context.Context, s domain.PriceSnapshot) (domain.PriceSnapshot, error) {
	m.snaps = append(m.snaps, s)
	return s, nil
}
func (m *memPriceStore) ListByItemID(_ context.Context, _ string) ([]domain.PriceSnapshot, error) {
	return m.snaps, nil
}
func (m *memPriceStore) LatestPerItem(_ context.Context) ([]domain.PriceSnapshot, error) {
	return m.snaps, nil
}

type memJobStore struct{ jobs map[string]domain.ScrapeJob }

func newMemJobStore() *memJobStore { return &memJobStore{jobs: make(map[string]domain.ScrapeJob)} }
func (m *memJobStore) Create(_ context.Context, job domain.ScrapeJob) (domain.ScrapeJob, error) {
	job.ID = "job-1"
	m.jobs[job.ID] = job
	return job, nil
}
func (m *memJobStore) Update(_ context.Context, job domain.ScrapeJob) error {
	m.jobs[job.ID] = job
	return nil
}
func (m *memJobStore) GetByID(_ context.Context, id string) (domain.ScrapeJob, error) {
	j, ok := m.jobs[id]
	if !ok {
		return domain.ScrapeJob{}, store.ErrNotFound
	}
	return j, nil
}
func (m *memJobStore) List(_ context.Context) ([]domain.ScrapeJob, error) {
	result := make([]domain.ScrapeJob, 0, len(m.jobs))
	for _, v := range m.jobs {
		result = append(result, v)
	}
	return result, nil
}

type memSettingsStore struct{ data map[string]string }

func (m *memSettingsStore) Get(_ context.Context, key string) (string, error) {
	v, ok := m.data[key]
	if !ok {
		return "", store.ErrNotFound
	}
	return v, nil
}
func (m *memSettingsStore) Set(_ context.Context, key, value string) error {
	m.data[key] = value
	return nil
}
func (m *memSettingsStore) GetAll(_ context.Context) (map[string]string, error) {
	return m.data, nil
}

// ---- helpers ----

// fakeEbay returns an httptest.Server that handles:
//   - POST /identity/v1/oauth2/token → client-credentials token
//   - GET /buy/browse/v1/item_summary/search → one completed listing
func fakeEbay(t *testing.T) *httptest.Server {
	t.Helper()
	mux := http.NewServeMux()
	mux.HandleFunc("/identity/v1/oauth2/token", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]any{
			"access_token": "fake-token",
			"expires_in":   3600,
			"token_type":   "Application Access Token",
		})
	})
	mux.HandleFunc("/buy/browse/v1/item_summary/search", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]any{
			"itemSummaries": []map[string]any{
				{
					"itemId": "v1|123|0",
					"price":  map[string]any{"value": "25.00", "currency": "EUR"},
					"condition": "Used",
				},
			},
			"total": 1,
		})
	})
	return httptest.NewServer(mux)
}

// ---- tests ----

func TestRunner_ScrapesItems_SavesPrices(t *testing.T) {
	srv := fakeEbay(t)
	defer srv.Close()

	items := &memItemStore{items: []domain.Item{
		{ID: "item-1", ExternalID: "ext-1", Title: "Super Mario 64", Platform: "Nintendo 64"},
	}}
	prices := &memPriceStore{}
	jobs := newMemJobStore()
	jobs.jobs["job-1"] = domain.ScrapeJob{ID: "job-1", Status: "pending"}
	settings := &memSettingsStore{data: map[string]string{
		"ebay_client_id":      "id",
		"ebay_client_secret":  "secret",
		"ebay_marketplace_id": "EBAY_FR",
	}}

	runner := scraper.NewEbayRunner(items, prices, jobs, settings,
		scraper.WithBaseURL(srv.URL),
	)
	runner("job-1", "test")

	if len(prices.snaps) == 0 {
		t.Error("want at least one price snapshot saved")
	}
	for _, s := range prices.snaps {
		if s.Source != "ebay" {
			t.Errorf("want source 'ebay', got %q", s.Source)
		}
		if s.ItemID != "item-1" {
			t.Errorf("want item_id 'item-1', got %q", s.ItemID)
		}
		if s.ScrapedAt.IsZero() {
			t.Error("want non-zero scraped_at")
		}
	}

	j, _ := jobs.GetByID(context.Background(), "job-1")
	if j.Status != "done" {
		t.Errorf("want job status 'done', got %q", j.Status)
	}
	_ = time.Now() // ensure time import used
}

func TestRunner_SpecificItems(t *testing.T) {
	srv := fakeEbay(t)
	defer srv.Close()

	items := &memItemStore{items: []domain.Item{
		{ID: "item-1", ExternalID: "ext-1", Title: "Zelda", Platform: "N64"},
		{ID: "item-2", ExternalID: "ext-2", Title: "Mario", Platform: "N64"},
	}}
	prices := &memPriceStore{}
	jobs := newMemJobStore()
	jobs.jobs["job-1"] = domain.ScrapeJob{ID: "job-1", Status: "pending"}
	settings := &memSettingsStore{data: map[string]string{
		"ebay_client_id":     "id",
		"ebay_client_secret": "secret",
	}}

	runner := scraper.NewEbayRunner(items, prices, jobs, settings,
		scraper.WithBaseURL(srv.URL),
	)
	// Only scrape item-1
	runner("job-1", "test", "item-1")

	for _, s := range prices.snaps {
		if s.ItemID != "item-1" {
			t.Errorf("want only item-1 scraped, got item %q", s.ItemID)
		}
	}
}
