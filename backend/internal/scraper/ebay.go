package scraper

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"os"
	"sort"
	"strconv"
	"strings"
	"time"

	"github.com/jplanckeel/thevaultbo/internal/domain"
	"github.com/jplanckeel/thevaultbo/internal/store"
)

const (
	defaultBaseURL      = "https://api.ebay.com"
	defaultMarketplaces = "EBAY_FR,EBAY_DE"
	// palSource tags the snapshots pooled from the PAL marketplaces.
	palSource = "ebay_pal"
)

// Option configures the eBay runner (for testing).
type Option func(*ebayRunner)

// Defaults keep a scheduled hourly run within eBay's Browse API quota (5 000 calls a
// day, two marketplaces per item): 80 items × 2 calls × 24 runs = 3 840 calls.
const (
	defaultBatch   = 80
	defaultRefresh = 24 * time.Hour
)

// WithBaseURL overrides the eBay API base URL (used in tests).
func WithBaseURL(u string) Option {
	return func(r *ebayRunner) { r.baseURL = u }
}

// WithCredentials sets the eBay OAuth credentials directly (overrides settings store).
func WithCredentials(clientID, clientSecret string) Option {
	return func(r *ebayRunner) {
		r.clientID = clientID
		r.clientSecret = clientSecret
	}
}

type ebayRunner struct {
	items        store.ItemStore
	prices       store.PriceStore
	jobs         store.JobStore
	settings     store.SettingsStore
	baseURL      string
	client       *http.Client
	clientID     string
	clientSecret string
	// batch and refresh bound scheduled runs: at most batch items per run, each item
	// quoted again once its latest snapshot is older than refresh (eBay daily quota).
	batch   int
	refresh time.Duration
}

// WithBatch limits a scheduled run to the batch stalest items, re-quoted after refresh.
func WithBatch(batch int, refresh time.Duration) Option {
	return func(r *ebayRunner) {
		if batch > 0 {
			r.batch = batch
		}
		if refresh > 0 {
			r.refresh = refresh
		}
	}
}

// NewEbayRunner returns the runner func used by ScrapeHandler.
func NewEbayRunner(
	items store.ItemStore,
	prices store.PriceStore,
	jobs store.JobStore,
	settings store.SettingsStore,
	opts ...Option,
) func(jobID, triggeredBy string, itemIDs ...string) {
	r := &ebayRunner{
		items:    items,
		prices:   prices,
		jobs:     jobs,
		settings: settings,
		baseURL:  defaultBaseURL,
		client:   &http.Client{Timeout: 30 * time.Second},
		batch:    defaultBatch,
		refresh:  defaultRefresh,
	}
	for _, o := range opts {
		o(r)
	}
	return r.run
}

func (r *ebayRunner) run(jobID, triggeredBy string, itemIDs ...string) {
	ctx := context.Background()

	// Mark job as running
	r.updateJob(ctx, jobID, triggeredBy, "running", 0, 0, "")

	// Resolve items to scrape
	items, err := r.resolveItems(ctx, itemIDs)
	if err != nil {
		r.updateJob(ctx, jobID, triggeredBy, "failed", 0, 0, fmt.Sprintf("resolve items: %v", err))
		return
	}

	// Get OAuth token
	token, err := r.getToken(ctx)
	if err != nil {
		r.updateJob(ctx, jobID, triggeredBy, "failed", len(items), 0, fmt.Sprintf("oauth: %v", err))
		return
	}

	markets := r.marketplaces(ctx)

	total := len(items)
	done := 0
	r.updateJob(ctx, jobID, triggeredBy, "running", total, done, "")

	var itemErrors []string
	for _, item := range items {
		snaps, err := r.scrapeItem(ctx, token, markets, item)
		if err != nil {
			itemErrors = append(itemErrors, fmt.Sprintf("%s: %v", item.ID, err))
		} else {
			for _, s := range snaps {
				if _, err := r.prices.Insert(ctx, s); err != nil {
					itemErrors = append(itemErrors, fmt.Sprintf("%s insert: %v", item.ID, err))
				}
			}
		}
		done++
		r.updateJob(ctx, jobID, triggeredBy, "running", total, done, "")
	}

	errMsg := ""
	if len(itemErrors) > 0 {
		errMsg = strings.Join(itemErrors, "; ")
	}
	r.updateJob(ctx, jobID, triggeredBy, "done", total, done, errMsg)
}

func (r *ebayRunner) resolveItems(ctx context.Context, itemIDs []string) ([]domain.Item, error) {
	if len(itemIDs) == 0 {
		return r.items.ListForPricing(ctx, r.refresh, r.batch)
	}
	result := make([]domain.Item, 0, len(itemIDs))
	for _, id := range itemIDs {
		item, err := r.items.GetByID(ctx, id)
		if err != nil {
			return nil, fmt.Errorf("get item %s: %w", id, err)
		}
		result = append(result, item)
	}
	return result, nil
}

func (r *ebayRunner) getToken(ctx context.Context) (string, error) {
	clientID := r.clientID
	clientSecret := r.clientSecret

	if clientID == "" {
		var err error
		clientID, err = r.settings.Get(ctx, "ebay_client_id")
		if err != nil {
			return "", fmt.Errorf("ebay_client_id setting: %w", err)
		}
	}
	if clientSecret == "" {
		var err error
		clientSecret, err = r.settings.Get(ctx, "ebay_client_secret")
		if err != nil {
			return "", fmt.Errorf("ebay_client_secret setting: %w", err)
		}
	}

	creds := base64.StdEncoding.EncodeToString([]byte(clientID + ":" + clientSecret))
	body := strings.NewReader("grant_type=client_credentials&scope=https%3A%2F%2Fapi.ebay.com%2Foauth%2Fapi_scope")

	req, err := http.NewRequestWithContext(ctx, http.MethodPost,
		r.baseURL+"/identity/v1/oauth2/token", body)
	if err != nil {
		return "", fmt.Errorf("build token request: %w", err)
	}
	req.Header.Set("Content-Type", "application/x-www-form-urlencoded")
	req.Header.Set("Authorization", "Basic "+creds)

	resp, err := r.client.Do(req)
	if err != nil {
		return "", fmt.Errorf("token request: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		b, _ := io.ReadAll(resp.Body)
		return "", fmt.Errorf("token response %d: %s", resp.StatusCode, b)
	}

	var tok struct {
		AccessToken string `json:"access_token"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&tok); err != nil {
		return "", fmt.Errorf("decode token: %w", err)
	}
	return tok.AccessToken, nil
}

// ebayItemSummary is a single result from the Browse API search.
type ebayItemSummary struct {
	Title string `json:"title"`
	Price struct {
		Value    string `json:"value"`
		Currency string `json:"currency"`
	} `json:"price"`
	Condition string `json:"condition"`
}

// marketplace is an eBay site queried for PAL prices, with the rate converting its
// currency to EUR (1 for euro sites).
type marketplace struct {
	ID       string
	Currency string
	ToEUR    float64
}

// marketplaces returns the sites to query: ebay_marketplace_ids (comma separated,
// default FR + DE). EBAY_GB is only used when an fx_gbp_eur rate is configured, so
// that no conversion rate is ever made up.
func (r *ebayRunner) marketplaces(ctx context.Context) []marketplace {
	ids := r.settingOrDefault(ctx, "ebay_marketplace_ids", r.settingOrDefault(ctx, "ebay_marketplace_id", defaultMarketplaces))
	gbp, _ := strconv.ParseFloat(r.settingOrDefault(ctx, "fx_gbp_eur", ""), 64)
	var out []marketplace
	for _, id := range strings.Split(ids, ",") {
		id = strings.TrimSpace(strings.ToUpper(id))
		switch {
		case id == "":
		case id == "EBAY_GB" && gbp > 0:
			out = append(out, marketplace{ID: id, Currency: "GBP", ToEUR: gbp})
		case id == "EBAY_GB":
			fmt.Fprintln(os.Stderr, "scraper: EBAY_GB skipped, set fx_gbp_eur to use it")
		default:
			out = append(out, marketplace{ID: id, Currency: "EUR", ToEUR: 1})
		}
	}
	if len(out) == 0 {
		out = []marketplace{{ID: "EBAY_FR", Currency: "EUR", ToEUR: 1}}
	}
	return out
}

func (r *ebayRunner) scrapeItem(ctx context.Context, token string, markets []marketplace, item domain.Item) ([]domain.PriceSnapshot, error) {
	byCondition := make(map[domain.PriceCondition][]float64)
	var lastErr error
	queried := 0
	for _, m := range markets {
		summaries, err := r.search(ctx, token, m.ID, item)
		if err != nil {
			lastErr = err
			continue
		}
		queried++
		for _, s := range summaries {
			if excludedListing(s.Title, s.Condition) || s.Price.Currency != m.Currency {
				continue
			}
			price, err := strconv.ParseFloat(s.Price.Value, 64)
			if err != nil || price <= 0 {
				continue
			}
			cond := classifyListing(s.Title, s.Condition)
			byCondition[cond] = append(byCondition[cond], price*m.ToEUR)
		}
	}
	if queried == 0 {
		return nil, lastErr
	}

	now := time.Now().UTC()
	snaps := make([]domain.PriceSnapshot, 0, len(byCondition))
	for cond, prices := range byCondition {
		stats := computeStats(prices)
		snaps = append(snaps, domain.PriceSnapshot{
			ItemID:      item.ID,
			Source:      palSource,
			Condition:   cond,
			PriceMin:    stats.Min,
			PriceMax:    stats.Max,
			PriceMedian: stats.Median,
			SampleCount: stats.Count,
			Currency:    "EUR",
			ScrapedAt:   now,
		})
	}
	return snaps, nil
}

// search returns the fixed-price listings of one marketplace for an item.
func (r *ebayRunner) search(ctx context.Context, token, marketplaceID string, item domain.Item) ([]ebayItemSummary, error) {
	query := url.QueryEscape(item.Title + " " + item.Platform)
	endpoint := fmt.Sprintf("%s/buy/browse/v1/item_summary/search?q=%s&filter=buyingOptions:{FIXED_PRICE}&limit=50", r.baseURL, query)

	req, err := http.NewRequestWithContext(ctx, http.MethodGet, endpoint, nil)
	if err != nil {
		return nil, fmt.Errorf("build search request: %w", err)
	}
	req.Header.Set("Authorization", "Bearer "+token)
	req.Header.Set("X-EBAY-C-MARKETPLACE-ID", marketplaceID)

	resp, err := r.client.Do(req)
	if err != nil {
		return nil, fmt.Errorf("search request: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		b, _ := io.ReadAll(resp.Body)
		return nil, fmt.Errorf("search response %d: %s", resp.StatusCode, b)
	}

	var result struct {
		ItemSummaries []ebayItemSummary `json:"itemSummaries"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return nil, fmt.Errorf("decode search: %w", err)
	}
	return result.ItemSummaries, nil
}

// computeStats calculates min, max, and median for a slice of prices.
func computeStats(prices []float64) domain.PriceStats {
	if len(prices) == 0 {
		return domain.PriceStats{}
	}

	sorted := make([]float64, len(prices))
	copy(sorted, prices)
	sort.Float64s(sorted)

	n := len(sorted)
	var median float64
	if n%2 == 0 {
		median = (sorted[n/2-1] + sorted[n/2]) / 2
	} else {
		median = sorted[n/2]
	}

	return domain.PriceStats{
		Min:    sorted[0],
		Max:    sorted[n-1],
		Median: median,
		Count:  n,
	}
}

func (r *ebayRunner) updateJob(ctx context.Context, jobID, triggeredBy, status string, total, done int, errMsg string) {
	now := time.Now().UTC()
	job := domain.ScrapeJob{
		ID:          jobID,
		TriggeredBy: triggeredBy,
		Status:      status,
		ItemsTotal:  total,
		ItemsDone:   done,
		Error:       errMsg,
	}
	if status == "done" || status == "failed" {
		job.FinishedAt = &now
	}
	if err := r.jobs.Update(ctx, job); err != nil {
		fmt.Fprintf(os.Stderr, "scraper: update job %s: %v\n", jobID, err)
	}
}

func (r *ebayRunner) settingOrDefault(ctx context.Context, key, def string) string {
	v, err := r.settings.Get(ctx, key)
	if err != nil {
		return def
	}
	return v
}

// mapCondition maps eBay condition strings to our domain conditions.
//
// New  : new/sealed items — never opened, shrink-wrap intact.
// Loose: item sold without original box or manual (disc/cart only, for parts…).
// CIB  : default — complete or assumed complete (very good → acceptable, used…).
func mapCondition(ebayCondition string) domain.PriceCondition {
	c := strings.ToLower(strings.TrimSpace(ebayCondition))

	// ── New / Sealed ─────────────────────────────────────────────────────────
	// Exact eBay condition labels (EN + FR) and common seller descriptions.
	newTerms := []string{
		"new", "brand new",
		"sealed", "factory sealed", "shrink wrapped",
		"mint", "mint condition", "mint sealed",
		"new in box", "new in package", "new in wrap",
		"new with box", "new with tags",
		"neuf", "neuf scellé", "neuf autre",
		"scellé", "scelle",
		"neu", "versiegelt", // DE
	}
	for _, t := range newTerms {
		if c == t {
			return domain.ConditionNew
		}
	}
	// Partial matches for sealed/mint descriptions
	if strings.Contains(c, "sealed") || strings.Contains(c, "scellé") ||
		strings.Contains(c, "shrink") || strings.Contains(c, "mint") {
		return domain.ConditionNew
	}

	// ── Loose / disc or cartridge only ───────────────────────────────────────
	// Items explicitly missing box, manual or both.
	looseTerms := []string{
		"loose",
		"cd only", "cd uniquement",
		"disc only", "disk only", "disque seul",
		"cartridge only", "cart only", "cartouche seule", "cartouche uniquement",
		"game only", "jeu seul", "jeu uniquement",
		"new without box", "neuf sans boite", "neuf sans boîte",
		"sans boite", "sans boîte", "sans étui",
		"no box", "no case", "no manual", "sans notice",
		"for parts", "pour pièces", "pour pieces",
		"not working", "ne fonctionne pas",
		"defective", "défectueux", "defectueux",
		"untested", "non testé", "non teste",
	}
	for _, t := range looseTerms {
		if strings.Contains(c, t) {
			return domain.ConditionLoose
		}
	}

	// ── CIB (default) ────────────────────────────────────────────────────────
	// Like New, Very Good, Good, Acceptable, Used, Très bon état, Bon état,
	// État correct, Complet, Complete, CIB, Refurbished, etc.
	return domain.ConditionCIB
}
