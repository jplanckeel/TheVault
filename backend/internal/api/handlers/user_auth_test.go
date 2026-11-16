package handlers_test

import (
	"context"
	"encoding/json"
	"errors"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/jplanckeel/thevaultbo/internal/api/handlers"
	"github.com/jplanckeel/thevaultbo/internal/store"
	"golang.org/x/crypto/bcrypt"
)

// memUsers is an in-memory UserStore with the same case-insensitive lookup as Postgres.
type memUsers struct{ users []store.User }

func (m *memUsers) Create(_ context.Context, email, hash, name string) (store.User, error) {
	u := store.User{ID: "u1", Email: email, PasswordHash: hash, Name: name}
	m.users = append(m.users, u)
	return u, nil
}

func (m *memUsers) GetByEmail(_ context.Context, email string) (store.User, error) {
	for _, u := range m.users {
		if strings.EqualFold(u.Email, email) {
			return u, nil
		}
	}
	return store.User{}, errors.New("no rows")
}

func (m *memUsers) GetByID(_ context.Context, id string) (store.User, error) {
	for _, u := range m.users {
		if u.ID == id {
			return u, nil
		}
	}
	return store.User{}, errors.New("no rows")
}

func (m *memUsers) PromoteAdmins(_ context.Context, emails []string) (int64, error) {
	var n int64
	for i, u := range m.users {
		for _, e := range emails {
			if !u.IsAdmin && strings.EqualFold(u.Email, e) {
				m.users[i].IsAdmin = true
				n++
			}
		}
	}
	return n, nil
}

func TestLogin_TrimsEmailAndGrantsAdmin(t *testing.T) {
	t.Setenv("JWT_SECRET", strings.Repeat("s", 32))
	hash, _ := bcrypt.GenerateFromPassword([]byte("motdepasse-solide"), bcrypt.MinCost)
	users := &memUsers{users: []store.User{{ID: "u1", Email: "Collector@Example.com", PasswordHash: string(hash)}}}
	h := handlers.NewUserAuthHandler(users, "collector@example.com")

	// Keyboard autocompletion: trailing space, other case.
	body := `{"email":"collector@example.com ","password":"motdepasse-solide"}`
	rec := httptest.NewRecorder()
	h.Login(rec, httptest.NewRequest(http.MethodPost, "/auth/login", strings.NewReader(body)))
	if rec.Code != http.StatusOK {
		t.Fatalf("status %d: %s", rec.Code, rec.Body)
	}
	var resp struct {
		User struct {
			IsAdmin bool `json:"is_admin"`
		} `json:"user"`
	}
	if err := json.NewDecoder(rec.Body).Decode(&resp); err != nil || !resp.User.IsAdmin {
		t.Errorf("is_admin = %v, err %v; want an admin listed in ADMIN_EMAILS", resp.User.IsAdmin, err)
	}

	rec = httptest.NewRecorder()
	h.Login(rec, httptest.NewRequest(http.MethodPost, "/auth/login", strings.NewReader(`{"email":"collector@example.com","password":"faux"}`)))
	if rec.Code != http.StatusUnauthorized {
		t.Errorf("wrong password: status %d, want 401", rec.Code)
	}
}
