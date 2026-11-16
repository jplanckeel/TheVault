package api

import (
	"context"
	"net/http"
	"time"
)

// readyTimeout bounds the dependency check of /readyz.
const readyTimeout = 2 * time.Second

// WithReadiness serves GET /readyz in front of next: 200 when ping succeeds (the database
// answers), 503 otherwise. /healthz stays a liveness probe that never touches the database,
// so a short database outage does not restart the container.
func WithReadiness(next http.Handler, ping func(context.Context) error) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/readyz" {
			next.ServeHTTP(w, r)
			return
		}
		ctx, cancel := context.WithTimeout(r.Context(), readyTimeout)
		defer cancel()
		w.Header().Set("Cache-Control", "no-store")
		if err := ping(ctx); err != nil {
			http.Error(w, "database unavailable", http.StatusServiceUnavailable)
			return
		}
		_, _ = w.Write([]byte("ready"))
	})
}
