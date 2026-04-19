package api

import (
	"net/http"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
	"github.com/go-chi/cors"
	"github.com/jplanckeel/thevaultbo/internal/api/handlers"
	"github.com/jplanckeel/thevaultbo/internal/igdb"
	"github.com/jplanckeel/thevaultbo/internal/store"
)

// NewRouter builds and returns the chi router with all routes mounted.
func NewRouter(
	items store.ItemStore,
	prices store.PriceStore,
	jobs store.JobStore,
	settings store.SettingsStore, //nolint:unparam // wired in a later task
	ebayRunner func(jobID, triggeredBy string, itemIDs ...string),
	igdbClient *igdb.Client,
	staticDir string,
) http.Handler {
	r := chi.NewRouter()

	r.Use(middleware.Logger)
	r.Use(middleware.Recoverer)
	r.Use(cors.Handler(cors.Options{
		AllowedOrigins: []string{"*"},
		AllowedMethods: []string{"GET", "POST", "PUT", "DELETE", "OPTIONS"},
		AllowedHeaders: []string{"Accept", "Content-Type"},
	}))

	ih := handlers.NewItemsHandler(items, prices)
	ph := handlers.NewPricesHandler(prices)
	sh := handlers.NewScrapeHandler(jobs, ebayRunner)
	gh := handlers.NewIGDBHandler(items, jobs, igdbClient)

	r.Get("/healthz", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("ok"))
	})

	// Serve frontend SPA (only when a static dir is configured)
	if staticDir != "" {
		fs := http.FileServer(http.Dir(staticDir))
		r.Get("/*", func(w http.ResponseWriter, req *http.Request) {
			// If the requested file exists, serve it directly.
			// Otherwise fall back to index.html for client-side routing.
			if _, err := http.Dir(staticDir).Open(req.URL.Path); err != nil {
				http.ServeFile(w, req, staticDir+"/index.html")
				return
			}
			fs.ServeHTTP(w, req)
		})
	}

	r.Route("/api/v1", func(r chi.Router) {
		// Items
		r.Post("/items", ih.UpsertItem)
		r.Get("/items", ih.ListItems)
		r.Post("/items/sync-igdb", gh.SyncIGDB)
		r.Get("/items/{id}", ih.GetItem)
		r.Get("/items/{id}/prices", ih.GetItemPrices)

		// Prices
		r.Get("/prices/latest", ph.GetLatestPrices)

		// Scrape jobs
		r.Post("/scrape", sh.TriggerScrape)
		r.Get("/scrape", sh.ListJobs)
		r.Get("/scrape/{id}", sh.GetJob)
	})

	return r
}
