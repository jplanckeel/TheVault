package scraper_test

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/jplanckeel/thevaultbo/internal/domain"
	"github.com/jplanckeel/thevaultbo/internal/scraper"
)

func TestEbayImages_Candidates(t *testing.T) {
	mux := http.NewServeMux()
	mux.HandleFunc("/identity/v1/oauth2/token", func(w http.ResponseWriter, _ *http.Request) {
		_ = json.NewEncoder(w).Encode(map[string]any{"access_token": "t"})
	})
	mux.HandleFunc("/buy/browse/v1/item_summary/search", func(w http.ResponseWriter, _ *http.Request) {
		listing := func(title, img string) map[string]any {
			return map[string]any{"title": title, "image": map[string]any{"imageUrl": img}, "itemWebUrl": "https://www.ebay.fr/itm/1"}
		}
		_ = json.NewEncoder(w).Encode(map[string]any{"itemSummaries": []map[string]any{
			listing("Zelda Collector", "https://i.ebayimg.com/images/g/a/s-l225.jpg"),
			listing("Zelda Collector bis", "https://i.ebayimg.com/images/g/a/s-l500.jpg"), // same photo, other size
			listing("No photo", ""),
			listing("Zelda", "https://i.ebayimg.com/images/g/b/s-l225.jpg"),
		}})
	})
	srv := httptest.NewServer(mux)
	defer srv.Close()

	src := scraper.NewEbayImages(&memSettingsStore{data: map[string]string{}},
		scraper.WithBaseURL(srv.URL), scraper.WithCredentials("id", "secret"))
	got, err := src.Candidates(context.Background(), domain.Item{Title: "Zelda", Platform: "GameCube"})
	if err != nil {
		t.Fatal(err)
	}
	if len(got) != 2 || got[0].URL != "https://i.ebayimg.com/images/g/a/s-l800.jpg" || got[0].Source != "ebay" || got[1].Label != "Zelda" {
		t.Errorf("candidates = %+v", got)
	}
}
