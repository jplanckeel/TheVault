package domain

import "time"

// ItemType represents the category of a collectible.
type ItemType string

const (
	ItemTypeGame      ItemType = "game"
	ItemTypeConsole   ItemType = "console"
	ItemTypeAccessory ItemType = "accessory"
)

// PriceCondition represents the physical state used for price lookup.
type PriceCondition string

const (
	ConditionLoose PriceCondition = "loose"
	ConditionCIB   PriceCondition = "cib"
	ConditionNew   PriceCondition = "new"
)

// Item is a game, console, or accessory synced from the mobile app.
type Item struct {
	ID          string    `json:"id"`
	ExternalID  string    `json:"external_id"`
	Type        ItemType  `json:"type"`
	Title       string    `json:"title"`
	Platform    string    `json:"platform"`
	Region      string    `json:"region"`
	Condition   string    `json:"condition"`
	Status      string    `json:"status"`
	CoverURL    string    `json:"cover_url"`
	IgdbID      string    `json:"igdb_id"`
	Summary     string    `json:"summary"`
	Genres      string    `json:"genres"`
	Developer   string    `json:"developer"`
	Publisher   string    `json:"publisher"`
	ReleaseYear int       `json:"release_year"`
	AdminEdited bool      `json:"admin_edited"`
	CreatedAt   time.Time `json:"created_at"`
	UpdatedAt   time.Time `json:"updated_at"`
}

// IGDBData holds metadata fetched from IGDB for an item.
type IGDBData struct {
	IgdbID      string
	CoverURL    string
	Summary     string
	Genres      string
	Developer   string
	Publisher   string
	ReleaseYear int
}

// PriceSnapshot is one scrape result for a given item + condition.
type PriceSnapshot struct {
	ID          string         `json:"id"`
	ItemID      string         `json:"item_id"`
	Source      string         `json:"source"`
	Condition   PriceCondition `json:"condition"`
	PriceMin    float64        `json:"price_min"`
	PriceMax    float64        `json:"price_max"`
	PriceMedian float64        `json:"price_median"`
	SampleCount int            `json:"sample_count"`
	Currency    string         `json:"currency"`
	ScrapedAt   time.Time      `json:"scraped_at"`
}

// ScrapeJob tracks a scraping or sync run (cron or manual).
type ScrapeJob struct {
	ID          string     `json:"id"`
	JobType     string     `json:"job_type"`
	TriggeredBy string     `json:"triggered_by"`
	Status      string     `json:"status"`
	ItemsTotal  int        `json:"items_total"`
	ItemsDone   int        `json:"items_done"`
	Error       string     `json:"error,omitempty"`
	StartedAt   time.Time  `json:"started_at"`
	FinishedAt  *time.Time `json:"finished_at,omitempty"`
}

// PriceStats holds computed statistics for a set of prices.
type PriceStats struct {
	Min    float64
	Max    float64
	Median float64
	Count  int
}

// TopMover holds the price evolution of an item over a period.
type TopMover struct {
	ItemID    string  `json:"item_id"`
	Condition string  `json:"condition"`
	PriceOld  float64 `json:"price_old"`
	PriceNew  float64 `json:"price_new"`
	DeltaAbs  float64 `json:"delta_abs"`
	DeltaPct  float64 `json:"delta_pct"`
}

// ExternalQuote is the latest eBay quote of an item for one condition, addressed by
// the external_id the apps know it by (POST /prices/quotes).
type ExternalQuote struct {
	ExternalID  string    `json:"external_id"`
	Condition   string    `json:"condition"`
	PriceMin    float64   `json:"price_min"`
	PriceMax    float64   `json:"price_max"`
	PriceMedian float64   `json:"price_median"`
	SampleCount int       `json:"sample_count"`
	ScrapedAt   time.Time `json:"scraped_at"`
	// PricePast is the median quoted `days` ago (the latest snapshot at or before that
	// date), or nil when the item has no snapshot that old.
	PricePast *float64 `json:"price_past"`
}

// CatalogEntry is the shared catalogue sheet of an item, addressed by the external_id
// an app knows it by (POST /items/lookup). A merged item answers with its target's sheet.
type CatalogEntry struct {
	ExternalID  string    `json:"external_id"`
	ItemID      string    `json:"item_id"`
	Type        string    `json:"type"`
	Title       string    `json:"title"`
	Platform    string    `json:"platform"`
	CoverURL    string    `json:"cover_url"`
	IgdbID      string    `json:"igdb_id"`
	Summary     string    `json:"summary"`
	Genres      string    `json:"genres"`
	Developer   string    `json:"developer"`
	Publisher   string    `json:"publisher"`
	ReleaseYear int       `json:"release_year"`
	AdminEdited bool      `json:"admin_edited"`
	UpdatedAt   time.Time `json:"updated_at"`
}

// ImageCandidate is an image the back-office can pick as an item cover.
type ImageCandidate struct {
	// URL of the image (an allowed host: eBay, IGDB or Wikimedia Commons).
	URL string `json:"url"`
	// Source is "ebay", "igdb" or "commons".
	Source string `json:"source"`
	// Label describes the image (listing title, IGDB game or edition name…).
	Label string `json:"label"`
	// PageURL links to the page the image comes from (listing, Commons file page).
	PageURL string `json:"page_url,omitempty"`
}
