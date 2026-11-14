package api

import (
	"net/http"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
	"github.com/go-chi/cors"
	"github.com/jplanckeel/thevaultbo/internal/api/handlers"
	"github.com/jplanckeel/thevaultbo/internal/igdb"
	"github.com/jplanckeel/thevaultbo/internal/store"
)

// Request body limits.
const (
	maxJSONBody   = 1 << 20  // 1 MiB
	maxUploadBody = 10 << 20 // 10 MiB (covers are capped at 8 MiB)
)

// NewRouter builds and returns the chi router with all routes mounted.
func NewRouter(
	items store.ItemStore,
	prices store.PriceStore,
	jobs store.JobStore,
	settings store.SettingsStore,
	users store.UserStore,
	collections store.UserCollectionStore,
	adminItems store.AdminItemStore,
	events store.SyncEventStore,
	adminEmails []string,
	ebayRunner func(jobID, triggeredBy string, itemIDs ...string),
	igdbRunner *igdb.Runner,
	igdbClient *igdb.Client,
	staticDir string,
	uploadDir string,
) http.Handler {
	r := chi.NewRouter()

	r.Use(middleware.Logger)
	r.Use(middleware.Recoverer)
	r.Use(SecurityHeaders)
	if origins := corsOrigins(); len(origins) > 0 {
		r.Use(cors.Handler(cors.Options{
			AllowedOrigins: origins,
			AllowedMethods: []string{"GET", "POST", "PUT", "DELETE", "OPTIONS"},
			AllowedHeaders: []string{"Accept", "Content-Type", "Authorization", "X-API-Key"},
		}))
	}

	authLimit := NewRateLimiter(10, time.Minute)    // login / register: slows brute force
	searchLimit := NewRateLimiter(60, time.Minute)  // IGDB proxy: protects our IGDB quota
	writeLimit := NewRateLimiter(1000, time.Minute) // catalogue upserts: allows a full CSV import
	priceLimit := NewRateLimiter(120, time.Minute)  // quotes and price history read by the apps

	ih := handlers.NewItemsHandler(items, prices, uploadDir).WithSyncEvents(events)
	ph := handlers.NewPricesHandler(prices)
	sh := handlers.NewScrapeHandler(jobs, ebayRunner)
	gh := handlers.NewIGDBHandler(igdbRunner)
	ch := handlers.NewConsolesHandler(items)
	ah := handlers.NewAccessoriesHandler(items)
	srch := handlers.NewSearchHandler(igdbClient)
	uah := handlers.NewUserAuthHandler(users, adminEmails...)
	var igdbSyncer handlers.IGDBSyncer
	if igdbRunner != nil {
		igdbSyncer = igdbRunner
	}
	adm := handlers.NewAdminHandler(items, adminItems, events, jobs, igdbSyncer)
	uch := handlers.NewUserCollectionHandler(collections)

	r.Get("/healthz", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("ok"))
	})

	// Always serve uploaded covers (no directory listing).
	r.Handle("/covers/*", http.StripPrefix("/covers/", http.FileServer(noListingFS{http.Dir(uploadDir + "/covers")})))

	// Serve frontend SPA (only when a static dir is configured)
	if staticDir != "" {
		fs := http.FileServer(noListingFS{http.Dir(staticDir)})
		r.Get("/*", func(w http.ResponseWriter, req *http.Request) {
			if _, err := http.Dir(staticDir).Open(req.URL.Path); err != nil {
				http.ServeFile(w, req, staticDir+"/index.html")
				return
			}
			fs.ServeHTTP(w, req)
		})
	}

	r.Route("/api/v1", func(r chi.Router) {
		// Cover upload is the only route accepting large bodies.
		r.With(AdminMiddleware(users), MaxBodySize(maxUploadBody)).Post("/items/{id}/cover/upload", ih.UploadCover)

		r.Group(func(r chi.Router) {
			r.Use(MaxBodySize(maxJSONBody))

			// Public routes (consultation)
			r.With(authLimit.Middleware).Post("/auth/register", uah.Register)
			r.With(authLimit.Middleware).Post("/auth/login", uah.Login)
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

			// Shared catalogue mutations — logged-in users and the mobile app (X-API-Key).
			r.Group(func(r chi.Router) {
				r.Use(AnyAuthMiddleware)
				r.With(writeLimit.Middleware).Post("/items", ih.UpsertItem)
				r.With(searchLimit.Middleware).Get("/search", srch.Search)
				r.With(priceLimit.Middleware).Post("/prices/quotes", ph.GetQuotes)
				r.With(priceLimit.Middleware).Get("/prices/history", ph.GetHistory)
			})

			// Admin-only routes: the caller's account must have the admin role.
			r.Group(func(r chi.Router) {
				r.Use(AdminMiddleware(users))
				r.Post("/items/sync-igdb", gh.SyncIGDB)
				r.Put("/items/{id}/cover", ih.UpdateCover)
				r.Post("/scrape", sh.TriggerScrape)
				r.Get("/scrape", sh.ListJobs)
				r.Get("/scrape/{id}", sh.GetJob)

				r.Route("/admin", func(r chi.Router) {
					r.Get("/items", adm.ListItems)
					r.Put("/items/{id}", adm.UpdateItem)
					r.Delete("/items/{id}", adm.DeleteItem)
					r.Post("/items/{id}/merge", adm.MergeItem)
					r.Post("/items/{id}/igdb", adm.RefreshIGDB)
					r.Get("/sync-events", adm.ListSyncEvents)
					r.Get("/sync-summary", adm.SyncSummary)
				})
			})
		})
	})

	return r
}
