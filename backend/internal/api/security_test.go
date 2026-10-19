package api

import (
	"io"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"
)

func TestMain(m *testing.M) {
	_ = os.Setenv("JWT_SECRET", strings.Repeat("s", 32))
	os.Exit(m.Run())
}

func TestSecurityHeaders(t *testing.T) {
	rec := httptest.NewRecorder()
	SecurityHeaders(http.NotFoundHandler()).ServeHTTP(rec, httptest.NewRequest(http.MethodGet, "/", nil))
	for _, h := range []string{"Content-Security-Policy", "X-Content-Type-Options", "X-Frame-Options", "Referrer-Policy"} {
		if rec.Header().Get(h) == "" {
			t.Errorf("missing %s", h)
		}
	}
	if rec.Header().Get("Strict-Transport-Security") != "" {
		t.Error("HSTS must only be sent over HTTPS")
	}
	req := httptest.NewRequest(http.MethodGet, "/", nil)
	req.Header.Set("X-Forwarded-Proto", "https")
	rec = httptest.NewRecorder()
	SecurityHeaders(http.NotFoundHandler()).ServeHTTP(rec, req)
	if rec.Header().Get("Strict-Transport-Security") == "" {
		t.Error("HSTS expected behind an HTTPS proxy")
	}
}

func TestRateLimiter(t *testing.T) {
	l := NewRateLimiter(2, time.Minute)
	now := time.Now()
	if ok, _ := l.Allow("1.1.1.1", now); !ok {
		t.Fatal("first request refused")
	}
	l.Allow("1.1.1.1", now)
	if ok, retry := l.Allow("1.1.1.1", now); ok || retry <= 0 {
		t.Errorf("third request should be refused with a retry delay, got ok=%v retry=%v", ok, retry)
	}
	if ok, _ := l.Allow("2.2.2.2", now); !ok {
		t.Error("limits must be per IP")
	}
	if ok, _ := l.Allow("1.1.1.1", now.Add(time.Minute+time.Second)); !ok {
		t.Error("window should reset")
	}
}

func TestRateLimiterMiddleware_Returns429(t *testing.T) {
	l := NewRateLimiter(1, time.Minute)
	h := l.Middleware(http.HandlerFunc(func(http.ResponseWriter, *http.Request) {}))
	codes := []int{}
	for range 2 {
		rec := httptest.NewRecorder()
		h.ServeHTTP(rec, httptest.NewRequest(http.MethodPost, "/auth/login", nil))
		codes = append(codes, rec.Code)
	}
	if codes[1] != http.StatusTooManyRequests {
		t.Errorf("codes = %v, want second request 429", codes)
	}
}

func TestClientIP_IgnoresForwardedForUnlessTrusted(t *testing.T) {
	req := httptest.NewRequest(http.MethodGet, "/", nil)
	req.RemoteAddr = "10.0.0.5:1234"
	req.Header.Set("X-Forwarded-For", "6.6.6.6, 10.0.0.1")
	if got := ClientIP(req, false); got != "10.0.0.5" {
		t.Errorf("untrusted: got %s, want remote address", got)
	}
	if got := ClientIP(req, true); got != "6.6.6.6" {
		t.Errorf("trusted proxy: got %s, want first forwarded address", got)
	}
}

func TestNoListingFS(t *testing.T) {
	dir := t.TempDir()
	if err := os.WriteFile(filepath.Join(dir, "a.jpg"), []byte("x"), 0o600); err != nil {
		t.Fatal(err)
	}
	srv := http.FileServer(noListingFS{http.Dir(dir)})
	for path, want := range map[string]int{"/": http.StatusNotFound, "/a.jpg": http.StatusOK} {
		rec := httptest.NewRecorder()
		srv.ServeHTTP(rec, httptest.NewRequest(http.MethodGet, path, nil))
		if rec.Code != want {
			t.Errorf("%s: status %d, want %d", path, rec.Code, want)
		}
	}
}

func TestMaxBodySize(t *testing.T) {
	var readErr error
	h := MaxBodySize(4)(http.HandlerFunc(func(_ http.ResponseWriter, r *http.Request) {
		_, readErr = io.ReadAll(r.Body)
	}))
	h.ServeHTTP(httptest.NewRecorder(), httptest.NewRequest(http.MethodPost, "/", strings.NewReader("too long")))
	if readErr == nil {
		t.Error("body over the limit should fail to read")
	}
}
