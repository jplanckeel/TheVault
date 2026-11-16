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
	"github.com/jplanckeel/thevaultbo/internal/images"
	"github.com/jplanckeel/thevaultbo/internal/scheduler"
	"github.com/jplanckeel/thevaultbo/internal/scraper"
	"github.com/jplanckeel/thevaultbo/internal/store"
	"github.com/jplanckeel/thevaultbo/migrations"
)

func main() {
	databaseURL := getenv("DATABASE_URL", "postgres://postgres:postgres@localhost:5432/thevaultbo?sslmode=disable")
	ctx := context.Background()

	// `server reset-password <email>`: prints a one-time link to choose a new password.
	if len(os.Args) > 1 && os.Args[1] == "reset-password" {
		os.Exit(resetPasswordCommand(ctx, databaseURL, os.Args[2:]))
	}

	// JSON logs (also used by the standard log package and the request logger).
	slog.SetDefault(slog.New(slog.NewJSONHandler(os.Stdout, nil)))

	port := getenv("PORT", "8080")

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
	if len(adminEmails) == 0 {
		slog.Warn("admin bootstrap: ADMIN_EMAILS is empty, nobody can open the back-office")
	} else if n, err := users.PromoteAdmins(ctx, adminEmails); err != nil {
		slog.Error("admin bootstrap", "err", err)
	} else {
		// Accounts are promoted at startup if they exist, otherwise at their next sign-in.
		slog.Info("admin bootstrap", "admin_emails", len(adminEmails), "promoted_now", n)
	}
	if v, _ := strconv.ParseBool(getenv("TRUST_PROXY", "")); !v {
		slog.Warn("TRUST_PROXY is not set: behind a reverse proxy every visitor shares the proxy's rate limits (sign-in: 10 per minute)")
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
	router := api.NewRouter(items, prices, jobs, settings, users, collections, store.NewVaultStore(pool), store.NewPasswordResetStore(pool), items, events, adminEmails,
		ebayRunner, igdbRunner, igdbClient, staticDir, uploadDir,
		igdbClient, scraper.NewEbayImages(settings, opts...))

	// Consoles: a free-licence photo of the model instead of the IGDB logo, at startup
	// and after each IGDB run (new consoles get the logo first).
	consolePhotos := images.NewConsolePhotos(items, uploadDir)
	go runConsolePhotos(consolePhotos, time.Hour)

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

// runConsolePhotos applies the console photos now, then every interval.
func runConsolePhotos(job *images.ConsolePhotos, interval time.Duration) {
	for {
		n, err := job.Run(context.Background())
		if err != nil {
			slog.Error("console photos", "err", err)
		} else if n > 0 {
			slog.Info("console photos applied", "updated", n)
		}
		time.Sleep(interval)
	}
}

// resetPasswordCommand prints a one-time password reset link for an account. It runs in
// the server container (docker exec … ./server reset-password <email>), so only someone
// with access to the server can reset a password.
func resetPasswordCommand(ctx context.Context, databaseURL string, args []string) int {
	if len(args) != 1 {
		fmt.Fprintln(os.Stderr, "usage: server reset-password <email>")
		return 2
	}
	if err := store.RunMigrations(migrations.FS, databaseURL); err != nil {
		fmt.Fprintf(os.Stderr, "migrations: %v\n", err)
		return 1
	}
	pool, err := store.OpenDB(ctx, databaseURL)
	if err != nil {
		fmt.Fprintf(os.Stderr, "db: %v\n", err)
		return 1
	}
	defer pool.Close()
	token, user, err := handlers.NewResetLink(ctx, store.NewUserStore(pool), store.NewPasswordResetStore(pool), strings.TrimSpace(args[0]), time.Now())
	if err != nil {
		fmt.Fprintf(os.Stderr, "reset-password: %v\n", err)
		return 1
	}
	site := strings.TrimRight(getenv("PUBLIC_URL", ""), "/")
	if site == "" {
		site = "https://<adresse du site>"
	}
	fmt.Printf("Lien de réinitialisation pour %s (valable %.0f h, utilisable une fois) :\n%s/reset-password#token=%s\n",
		user.Email, handlers.ResetLinkTTL.Hours(), site, token)
	return 0
}
