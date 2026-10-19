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
	settings store.SettingsStore,
	users store.UserStore,
	collections store.UserCollectionStore,
	ebayRunner func(jobID, triggeredBy string, itemIDs ...string),
	igdbRunner *igdb.Runner,
	igdbClient *igdb.Client,
	staticDir string,
	uploadDir string,
) http.Handler {
	r := chi.NewRouter()

	r.Use(middleware.Logger)
	r.Use(middleware.Recoverer)
	r.Use(cors.Handler(cors.Options{
		AllowedOrigins: []string{"*"},
		AllowedMethods: []string{"GET", "POST", "PUT", "DELETE", "OPTIONS"},
		AllowedHeaders: []string{"Accept", "Content-Type", "Authorization", "X-API-Key"},
	}))

	ih := handlers.NewItemsHandler(items, prices, uploadDir)
	ph := handlers.NewPricesHandler(prices)
	sh := handlers.NewScrapeHandler(jobs, ebayRunner)
	gh := handlers.NewIGDBHandler(igdbRunner)
	ch := handlers.NewConsolesHandler(items)
	ah := handlers.NewAccessoriesHandler(items)
	lh := handlers.NewLoginHandler()
	srch := handlers.NewSearchHandler(igdbClient)
	uah := handlers.NewUserAuthHandler(users)
	uch := handlers.NewUserCollectionHandler(collections)

	r.Get("/healthz", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("ok"))
	})

	// Always serve uploaded covers
	r.Handle("/covers/*", http.StripPrefix("/covers/", http.FileServer(http.Dir(uploadDir+"/covers"))))

	// Serve frontend SPA (only when a static dir is configured)
	if staticDir != "" {
		fs := http.FileServer(http.Dir(staticDir))
		r.Get("/*", func(w http.ResponseWriter, req *http.Request) {
			if _, err := http.Dir(staticDir).Open(req.URL.Path); err != nil {
				http.ServeFile(w, req, staticDir+"/index.html")
				return
			}
			fs.ServeHTTP(w, req)
		})
	}

	r.Route("/api/v1", func(r chi.Router) {
		// Public routes (consultation)
		r.Post("/login", lh.Login)
		r.Post("/auth/register", uah.Register)
		r.Post("/auth/login", uah.Login)
		r.Get("/items", ih.ListItems)
		r.Get("/items/{id}", ih.GetItem)
		r.Get("/items/{id}/prices", ih.GetItemPrices)
		r.Get("/consoles", ch.ListConsoles)
		r.Get("/consoles/{id}", ch.GetConsole)
		r.Get("/consoles/{id}/games", ch.GetConsoleGames)
		r.Get("/accessories", ah.ListAccessories)
		r.Get("/accessories/{id}", ah.GetAccessory)
		r.Get("/prices/latest", ph.GetLatestPrices)
		r.Get("/prices/top-movers", ph.GetTopMovers)

		// User-authenticated routes
		r.Group(func(r chi.Router) {
			r.Use(UserAuthMiddleware)
			r.Get("/auth/me", uah.Me)
			r.Post("/collection", uch.AddItem)
			r.Delete("/collection/{itemId}", uch.RemoveItem)
			r.Get("/collection", uch.ListCollection)
			r.Get("/wishlist", uch.ListWishlist)
		})

		// Shared catalogue mutations — any authenticated caller can contribute
		// (logged-in user, admin, or mobile app via X-API-Key).
		r.Group(func(r chi.Router) {
			r.Use(AnyAuthMiddleware)
			r.Post("/items", ih.UpsertItem)
			r.Get("/search", srch.Search)
		})

		// Admin-protected routes
		r.Group(func(r chi.Router) {
			r.Use(AuthMiddleware)
			r.Post("/items/sync-igdb", gh.SyncIGDB)
			r.Put("/items/{id}/cover", ih.UpdateCover)
			r.Post("/items/{id}/cover/upload", ih.UploadCover)
			r.Post("/scrape", sh.TriggerScrape)
			r.Get("/scrape", sh.ListJobs)
			r.Get("/scrape/{id}", sh.GetJob)
		})
	})

	return r
}
