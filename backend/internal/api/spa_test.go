package api

import (
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func TestSPAHandler(t *testing.T) {
	dir := t.TempDir()
	must := func(err error) {
		t.Helper()
		if err != nil {
			t.Fatal(err)
		}
	}
	must(os.WriteFile(filepath.Join(dir, "index.html"), []byte("<!doctype html>app"), 0o600))
	must(os.Mkdir(filepath.Join(dir, "assets"), 0o750))
	must(os.WriteFile(filepath.Join(dir, "assets", "app.js"), []byte("console.log(1)"), 0o600))

	h := spaHandler(dir)
	for path, want := range map[string]string{
		"/":              "app",            // root: the app, not a 404
		"/collection":    "app",            // client-side route
		"/assets/":       "app",            // directory: never listed
		"/assets/app.js": "console.log(1)", // real file
	} {
		rec := httptest.NewRecorder()
		h.ServeHTTP(rec, httptest.NewRequest(http.MethodGet, path, nil))
		if rec.Code != http.StatusOK || !strings.Contains(rec.Body.String(), want) {
			t.Errorf("%s: status %d, body %q, want 200 containing %q", path, rec.Code, rec.Body.String(), want)
		}
	}
}
