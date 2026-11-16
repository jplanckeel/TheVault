package handlers_test

import (
	"context"
	"encoding/json"
	"errors"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/go-chi/chi/v5"
	"github.com/jplanckeel/thevaultbo/internal/api/handlers"
	"github.com/jplanckeel/thevaultbo/internal/domain"
)

type fakeSource struct {
	name string
	out  []domain.ImageCandidate
	err  error
}

func (f fakeSource) Name() string { return f.name }
func (f fakeSource) Candidates(context.Context, domain.Item) ([]domain.ImageCandidate, error) {
	return f.out, f.err
}

type fakeImporter struct{ got string }

func (f *fakeImporter) Import(_ context.Context, name, rawURL string) (string, error) {
	f.got = rawURL
	return "/covers/" + name + ".jpg?v=1", nil
}

const consoleID = "1b4e28ba-2fa1-11d2-883f-0016d3cca427"

func imagesFixture() (*mockItemStore, *fakeImporter, http.Handler) {
	items := newMockItemStore()
	items.items["ext"] = domain.Item{ID: consoleID, ExternalID: "ext", Type: domain.ItemTypeConsole, Title: "GameCube Indigo", Platform: "GameCube"}
	imp := &fakeImporter{}
	h := handlers.NewImagesHandler(items, imp,
		fakeSource{name: "igdb", out: []domain.ImageCandidate{{URL: "https://images.igdb.com/a.jpg", Source: "igdb"}}},
		fakeSource{name: "ebay", err: errors.New("no credentials")},
	)
	r := chi.NewRouter()
	r.Get("/items/{id}/images", h.Candidates)
	r.Post("/items/{id}/cover/import", h.ImportCover)
	return items, imp, r
}

func TestImageCandidates(t *testing.T) {
	_, _, r := imagesFixture()
	rec := httptest.NewRecorder()
	r.ServeHTTP(rec, httptest.NewRequest(http.MethodGet, "/items/"+consoleID+"/images", nil))
	if rec.Code != http.StatusOK {
		t.Fatalf("status %d", rec.Code)
	}
	var resp struct {
		Candidates  []domain.ImageCandidate `json:"candidates"`
		Unavailable []string                `json:"unavailable"`
	}
	if err := json.NewDecoder(rec.Body).Decode(&resp); err != nil {
		t.Fatal(err)
	}
	// Commons photo of the model first, then the sources; the failing one is reported.
	if len(resp.Candidates) != 2 || resp.Candidates[0].Source != "commons" || resp.Candidates[1].Source != "igdb" {
		t.Errorf("candidates = %+v", resp.Candidates)
	}
	if len(resp.Unavailable) != 1 || resp.Unavailable[0] != "ebay" {
		t.Errorf("unavailable = %v", resp.Unavailable)
	}
}

func TestImportCover(t *testing.T) {
	items, imp, r := imagesFixture()
	post := func(body string) *httptest.ResponseRecorder {
		rec := httptest.NewRecorder()
		r.ServeHTTP(rec, httptest.NewRequest(http.MethodPost, "/items/"+consoleID+"/cover/import", strings.NewReader(body)))
		return rec
	}
	for _, bad := range []string{`{"url":"http://i.ebayimg.com/a.jpg"}`, `{"url":"https://169.254.169.254/latest"}`, `{"url":"https://evil.example/a.jpg"}`, `nope`} {
		if rec := post(bad); rec.Code != http.StatusBadRequest {
			t.Errorf("%s: status %d, want 400", bad, rec.Code)
		}
	}
	rec := post(`{"url":"https://i.ebayimg.com/images/g/x/s-l800.jpg"}`)
	if rec.Code != http.StatusOK || imp.got != "https://i.ebayimg.com/images/g/x/s-l800.jpg" {
		t.Fatalf("status %d, imported %q", rec.Code, imp.got)
	}
	if got := items.items["ext"].CoverURL; got != "/covers/"+consoleID+".jpg?v=1" {
		t.Errorf("cover = %q", got)
	}
}
