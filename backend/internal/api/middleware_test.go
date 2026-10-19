package api

import (
	"context"
	"errors"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"github.com/jplanckeel/thevaultbo/internal/api/handlers"
	"github.com/jplanckeel/thevaultbo/internal/domain"
	"github.com/jplanckeel/thevaultbo/internal/store"
)

type fakeUsers struct{ users map[string]store.User }

func (f fakeUsers) Create(context.Context, string, string, string) (store.User, error) {
	return store.User{}, errors.New("not implemented")
}
func (f fakeUsers) GetByEmail(context.Context, string) (store.User, error) {
	return store.User{}, errors.New("not implemented")
}
func (f fakeUsers) GetByID(_ context.Context, id string) (store.User, error) {
	if u, ok := f.users[id]; ok {
		return u, nil
	}
	return store.User{}, store.ErrNotFound
}
func (f fakeUsers) PromoteAdmins(context.Context, []string) (int64, error) { return 0, nil }

func tokenFor(t *testing.T, sub string) string {
	t.Helper()
	s, err := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
		"sub": sub, "exp": time.Now().Add(time.Hour).Unix(),
	}).SignedString(jwtSecret())
	if err != nil {
		t.Fatal(err)
	}
	return s
}

func TestAdminMiddleware(t *testing.T) {
	users := fakeUsers{users: map[string]store.User{
		"u-admin": {ID: "u-admin", Email: "boss@test", IsAdmin: true},
		"u-user":  {ID: "u-user", Email: "user@test"},
	}}
	var caller handlers.Caller
	h := AdminMiddleware(users)(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		caller = handlers.CallerFrom(r.Context())
	}))

	cases := map[string]struct {
		auth string
		want int
	}{
		"no token":     {"", http.StatusUnauthorized},
		"bad token":    {"Bearer nope", http.StatusUnauthorized},
		"unknown user": {"Bearer " + tokenFor(t, "u-ghost"), http.StatusUnauthorized},
		"regular user": {"Bearer " + tokenFor(t, "u-user"), http.StatusForbidden},
		"admin":        {"Bearer " + tokenFor(t, "u-admin"), http.StatusOK},
	}
	for name, c := range cases {
		req := httptest.NewRequest(http.MethodGet, "/admin/items", nil)
		if c.auth != "" {
			req.Header.Set("Authorization", c.auth)
		}
		rec := httptest.NewRecorder()
		h.ServeHTTP(rec, req)
		if rec.Code != c.want {
			t.Errorf("%s: status = %d, want %d", name, rec.Code, c.want)
		}
	}
	if caller.Source != domain.SourceAdmin || caller.Actor != "boss@test" {
		t.Errorf("caller = %+v", caller)
	}
}

func TestAnyAuthMiddleware_IdentifiesMobile(t *testing.T) {
	t.Setenv("API_KEY", "k")
	var caller handlers.Caller
	h := AnyAuthMiddleware(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		caller = handlers.CallerFrom(r.Context())
	}))
	req := httptest.NewRequest(http.MethodPost, "/items", nil)
	req.Header.Set("X-API-Key", "k")
	h.ServeHTTP(httptest.NewRecorder(), req)
	if caller.Source != domain.SourceMobile {
		t.Errorf("source = %q, want mobile", caller.Source)
	}
}
