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
	defaultBaseURL     = "https://api.ebay.com"
	defaultMarketplace = "EBAY_FR"
)

// Option configures the eBay runner (for testing).
type Option func(*ebayRunner)

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

	marketplace := r.settingOrDefault(ctx, "ebay_marketplace_id", defaultMarketplace)

	total := len(items)
	done := 0
	r.updateJob(ctx, jobID, triggeredBy, "running", total, done, "")

	var itemErrors []string
	for _, item := range items {
		snaps, err := r.scrapeItem(ctx, token, marketplace, item)
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
		return r.items.List(ctx)
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
	Price struct {
		Value    string `json:"value"`
		Currency string `json:"currency"`
	} `json:"price"`
	Condition string `json:"condition"`
}

func (r *ebayRunner) scrapeItem(ctx context.Context, token, marketplace string, item domain.Item) ([]domain.PriceSnapshot, error) {
	query := url.QueryEscape(item.Title + " " + item.Platform)
	endpoint := fmt.Sprintf("%s/buy/browse/v1/item_summary/search?q=%s&filter=buyingOptions:{FIXED_PRICE}&limit=50", r.baseURL, query)

	req, err := http.NewRequestWithContext(ctx, http.MethodGet, endpoint, nil)
	if err != nil {
		return nil, fmt.Errorf("build search request: %w", err)
	}
	req.Header.Set("Authorization", "Bearer "+token)
	req.Header.Set("X-EBAY-C-MARKETPLACE-ID", marketplace)
	req.Header.Set("X-EBAY-C-ENDUSERCTX", "contextualLocation=country=FR")

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
		Total         int               `json:"total"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return nil, fmt.Errorf("decode search: %w", err)
	}

	fmt.Fprintf(os.Stderr, "scraper: item %s — eBay returned %d results (total=%d)\n", item.ID, len(result.ItemSummaries), result.Total)

	// Group prices by condition
	type priceEntry struct {
		price    float64
		currency string
	}
	byCondition := make(map[domain.PriceCondition][]priceEntry)

	for _, s := range result.ItemSummaries {
		price, err := strconv.ParseFloat(s.Price.Value, 64)
		if err != nil {
			continue
		}
		cond := mapCondition(s.Condition)
		byCondition[cond] = append(byCondition[cond], priceEntry{price: price, currency: s.Price.Currency})
	}

	now := time.Now().UTC()
	snaps := make([]domain.PriceSnapshot, 0, len(byCondition))

	for cond, entries := range byCondition {
		if len(entries) == 0 {
			continue
		}

		prices := make([]float64, len(entries))
		currency := "EUR"
		for i, e := range entries {
			prices[i] = e.price
			if e.currency != "" {
				currency = e.currency
			}
		}

		stats := computeStats(prices)

		snaps = append(snaps, domain.PriceSnapshot{
			ItemID:      item.ID,
			Source:      "ebay",
			Condition:   cond,
			PriceMin:    stats.Min,
			PriceMax:    stats.Max,
			PriceMedian: stats.Median,
			SampleCount: stats.Count,
			Currency:    currency,
			ScrapedAt:   now,
		})
	}

	return snaps, nil
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
// "Like New" maps to CIB as the closest eBay equivalent of "complete in box".
// "New" maps to New. Everything else (Used, Good, Acceptable, etc.) maps to Loose.
func mapCondition(ebayCondition string) domain.PriceCondition {
	switch strings.ToLower(ebayCondition) {
	case "new", "brand new":
		return domain.ConditionNew
	case "like new":
		return domain.ConditionCIB
	default:
		return domain.ConditionLoose
	}
}
