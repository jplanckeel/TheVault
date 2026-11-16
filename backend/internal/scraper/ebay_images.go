package scraper

import (
	"context"
	"net/http"
	"regexp"
	"time"

	"github.com/jplanckeel/thevaultbo/internal/domain"
	"github.com/jplanckeel/thevaultbo/internal/store"
)

// maxEbayImages caps the photos suggested for one item.
const maxEbayImages = 24

// ebaySize matches the size suffix of eBay image URLs (…/s-l225.jpg).
var ebaySize = regexp.MustCompile(`/s-l\d+\.`)

// EbayImages suggests the photos of the eBay listings matching an item (one Browse API
// call on the first configured marketplace).
type EbayImages struct {
	r *ebayRunner
}

// NewEbayImages returns the eBay photo source; options are the runner ones.
func NewEbayImages(settings store.SettingsStore, opts ...Option) *EbayImages {
	r := &ebayRunner{settings: settings, baseURL: defaultBaseURL, client: &http.Client{Timeout: 30 * time.Second}}
	for _, o := range opts {
		o(r)
	}
	return &EbayImages{r: r}
}

// Name identifies the source in the back-office.
func (e *EbayImages) Name() string { return "ebay" }

// Candidates returns the distinct listing photos, enlarged to 800 px.
func (e *EbayImages) Candidates(ctx context.Context, item domain.Item) ([]domain.ImageCandidate, error) {
	token, err := e.r.getToken(ctx)
	if err != nil {
		return nil, err
	}
	summaries, err := e.r.search(ctx, token, e.r.marketplaces(ctx)[0].ID, item)
	if err != nil {
		return nil, err
	}
	seen := map[string]bool{}
	var out []domain.ImageCandidate
	for _, s := range summaries {
		u := ebaySize.ReplaceAllString(s.Image.ImageURL, "/s-l800.")
		if u == "" || seen[u] {
			continue
		}
		seen[u] = true
		out = append(out, domain.ImageCandidate{URL: u, Source: "ebay", Label: s.Title, PageURL: s.ItemWebURL})
		if len(out) == maxEbayImages {
			break
		}
	}
	return out, nil
}
