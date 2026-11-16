package main

import (
	"context"
	"fmt"
	"log"
	"log/slog"
	"net/http"
	"os"
	"strconv"
	"strings"
	"time"

	"github.com/jplanckeel/thevaultbo/internal/api"
	"github.com/jplanckeel/thevaultbo/internal/api/handlers"
	"github.com/jplanckeel/thevaultbo/internal/igdb"
	"github.com/jplanckeel/thevaultbo/internal/scheduler"
	"github.com/jplanckeel/thevaultbo/internal/scraper"
	"github.com/jplanckeel/thevaultbo/internal/store"
	"github.com/jplanckeel/thevaultbo/migrations"
)

func main() {
	// JSON logs (also used by the standard log package and the request logger).
	slog.SetDefault(slog.New(slog.NewJSONHandler(os.Stdout, nil)))

	databaseURL := getenv("DATABASE_URL", "postgres://postgres:postgres@localhost:5432/thevaultbo?sslmode=disable")
	port := getenv("PORT", "8080")

	ctx := context.Background()

	if err := handlers.CheckSecrets(); err != nil {
		log.Fatalf("configuration: %v", err)
	}

	// Run migrations
	if err := store.RunMigrations(migrations.FS, databaseURL); err != nil {
		log.Fatalf("migrations: %v", err)
	}

	// Open DB pool
	pool, err := store.OpenDB(ctx, databaseURL)
	if err != nil {
		log.Fatalf("db: %v", err)
	}
	defer pool.Close()

	// Build stores
	items := store.NewItemStore(pool)
	prices := store.NewPriceStore(pool)
	jobs := store.NewJobStore(pool)
	settings := store.NewSettingsStore(pool)
	users := store.NewUserStore(pool)
	collections := store.NewUserCollectionStore(pool)
	events := store.NewSyncEventStore(pool)

	// Admin role bootstrap: accounts listed in ADMIN_EMAILS become admins.
	adminEmails := parseEmails(getenv("ADMIN_EMAILS", ""))
	if n, err := users.PromoteAdmins(ctx, adminEmails); err != nil {
		log.Printf("admin bootstrap: %v", err)
	} else if n > 0 {
		log.Printf("admin bootstrap: %d account(s) promoted", n)
	}

	// Build eBay runner
	ebayClientID := getenv("EBAY_CLIENT_ID", "")
	ebayClientSecret := getenv("EBAY_CLIENT_SECRET", "")
	ebayBaseURL := getenv("EBAY_BASE_URL", "")
	opts := []scraper.Option{
		scraper.WithCredentials(ebayClientID, ebayClientSecret),
		scraper.WithBatch(atoiOr(getenv("EBAY_BATCH_SIZE", ""), 0), time.Duration(atoiOr(getenv("EBAY_REFRESH_HOURS", ""), 0))*time.Hour),
	}
	if ebayBaseURL != "" {
		opts = append(opts, scraper.WithBaseURL(ebayBaseURL))
	}
	ebayRunner := scraper.NewEbayRunner(items, prices, jobs, settings, opts...)

	// Build IGDB client and runner
	igdbClientID := getenv("IGDB_CLIENT_ID", "")
	igdbClientSecret := getenv("IGDB_CLIENT_SECRET", "")
	igdbClient := igdb.New(igdbClientID, igdbClientSecret)
	igdbRunner := igdb.NewRunner(items, jobs, igdbClient)

	// Start scheduler
	ebayCron := getenv("EBAY_CRON", "0 * * * *") // default: every hour
	// Hourly: new items get their cover quickly; unknown ones are retried weekly.
	igdbCron := getenv("IGDB_CRON", "15 * * * *")
	schedOpts := []scheduler.Option{
		scheduler.WithEbaySchedule(ebayCron),
		scheduler.WithIGDB(igdbRunner, igdbCron),
	}
	sched := scheduler.New(jobs, ebayRunner, schedOpts...)
	if err := sched.Start(); err != nil {
		log.Fatalf("scheduler: %v", err)
	}
	defer sched.Stop()

	// Build router
	staticDir := getenv("STATIC_DIR", "")
	uploadDir := getenv("UPLOAD_DIR", staticDir)
	if uploadDir == "" {
		uploadDir = "./uploads"
	}
	router := api.NewRouter(items, prices, jobs, settings, users, collections, store.NewVaultStore(pool), items, events, adminEmails,
		ebayRunner, igdbRunner, igdbClient, staticDir, uploadDir)

	srv := &http.Server{
		Addr:              fmt.Sprintf(":%s", port),
		Handler:           api.WithReadiness(router, pool.Ping),
		ReadHeaderTimeout: 5 * time.Second,
		ReadTimeout:       30 * time.Second, // cover uploads up to 8 MiB
		WriteTimeout:      60 * time.Second,
		IdleTimeout:       120 * time.Second,
		MaxHeaderBytes:    1 << 16,
	}
	log.Printf("listening on %s", srv.Addr)
	if err := srv.ListenAndServe(); err != nil {
		log.Fatalf("server: %v", err)
	}
}

func getenv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}

// parseEmails splits a comma-separated list into trimmed, lower-case e-mails.
func parseEmails(raw string) []string {
	var out []string
	for _, e := range strings.Split(raw, ",") {
		if e = strings.ToLower(strings.TrimSpace(e)); e != "" {
			out = append(out, e)
		}
	}
	return out
}

// atoiOr parses s, returning def when it is empty or not a number.
func atoiOr(s string, def int) int {
	n, err := strconv.Atoi(s)
	if err != nil {
		return def
	}
	return n
}
