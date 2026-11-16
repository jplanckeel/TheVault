package api

import (
	"context"
	"errors"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestWithReadiness(t *testing.T) {
	next := http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) { w.WriteHeader(http.StatusTeapot) })
	cases := []struct {
		path string
		ping error
		want int
	}{
		{"/readyz", nil, http.StatusOK},
		{"/readyz", errors.New("down"), http.StatusServiceUnavailable},
		{"/healthz", errors.New("down"), http.StatusTeapot},
	}
	for _, c := range cases {
		h := WithReadiness(next, func(context.Context) error { return c.ping })
		w := httptest.NewRecorder()
		h.ServeHTTP(w, httptest.NewRequest(http.MethodGet, c.path, nil))
		if w.Code != c.want {
			t.Errorf("%s with ping %v: want %d, got %d", c.path, c.ping, c.want, w.Code)
		}
	}
}
