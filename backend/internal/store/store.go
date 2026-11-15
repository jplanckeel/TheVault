package store

import (
	"context"
	"errors"
	"time"

	"github.com/jplanckeel/thevaultbo/internal/domain"
)

// ErrNotFound is returned when an update or delete targets a row that does not exist.
var ErrNotFound = errors.New("not found")

// ErrTombstoned is returned when a sync targets an item deleted by an admin.
var ErrTombstoned = errors.New("item deleted by an admin")

// ItemStore defines persistence operations for items.
type ItemStore interface {
	Upsert(ctx context.Context, item domain.Item) (domain.Item, error)
	List(ctx context.Context) ([]domain.Item, error)
	GetByID(ctx context.Context, id string) (domain.Item, error)
	ListWithoutIGDB(ctx context.Context) ([]domain.Item, error)
	UpdateIGDB(ctx context.Context, id string, data domain.IGDBData) error
	UpdateCoverURL(ctx context.Context, id string, coverURL string) error
	// LookupByExternalIDs returns the catalogue sheets of the items known by these
	// external ids (directly or through a merge alias).
	LookupByExternalIDs(ctx context.Context, externalIDs []string) ([]domain.CatalogEntry, error)
	// ListForPricing returns at most limit items whose latest price snapshot is older
	// than olderThan (never quoted first, then the stalest).
	ListForPricing(ctx context.Context, olderThan time.Duration, limit int) ([]domain.Item, error)
}

// PriceStore defines persistence operations for price snapshots.
type PriceStore interface {
	Insert(ctx context.Context, snapshot domain.PriceSnapshot) (domain.PriceSnapshot, error)
	ListByItemID(ctx context.Context, itemID string) ([]domain.PriceSnapshot, error)
	LatestPerItem(ctx context.Context) ([]domain.PriceSnapshot, error)
	TopMovers(ctx context.Context, days, limit int) ([]domain.TopMover, error)
	QuotesByExternalIDs(ctx context.Context, externalIDs []string, days int) ([]domain.ExternalQuote, error)
	HistoryByExternalID(ctx context.Context, externalID string, days int) ([]domain.PriceSnapshot, error)
}

// JobStore defines persistence operations for scrape jobs.
type JobStore interface {
	Create(ctx context.Context, job domain.ScrapeJob) (domain.ScrapeJob, error)
	Update(ctx context.Context, job domain.ScrapeJob) error
	GetByID(ctx context.Context, id string) (domain.ScrapeJob, error)
	List(ctx context.Context) ([]domain.ScrapeJob, error)
}

// SettingsStore defines read/write access to key-value settings.
type SettingsStore interface {
	Get(ctx context.Context, key string) (string, error)
	Set(ctx context.Context, key, value string) error
	GetAll(ctx context.Context) (map[string]string, error)
}
