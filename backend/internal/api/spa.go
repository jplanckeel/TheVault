package api

import (
	"net/http"
	"path/filepath"
)

// spaHandler serves the built web app from dir: existing files as they are, and
// index.html for everything else (client-side routes, "/" and any directory, which
// are never listed).
func spaHandler(dir string) http.HandlerFunc {
	files := noListingFS{http.Dir(dir)}
	fileServer := http.FileServer(files)
	index := filepath.Join(dir, "index.html")
	return func(w http.ResponseWriter, r *http.Request) {
		f, err := files.Open(r.URL.Path)
		if err != nil {
			http.ServeFile(w, r, index)
			return
		}
		_ = f.Close()
		fileServer.ServeHTTP(w, r)
	}
}
