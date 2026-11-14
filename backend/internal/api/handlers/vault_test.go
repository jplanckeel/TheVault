package handlers_test

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/jplanckeel/thevaultbo/internal/api/handlers"
	"github.com/jplanckeel/thevaultbo/internal/domain"
	"github.com/jplanckeel/thevaultbo/internal/store"
)

type memVault struct {
	backups map[string][]byte
	shares  map[string]domain.Share
}

func newMemVault() *memVault {
	return &memVault{backups: map[string][]byte{}, shares: map[string]domain.Share{}}
}

func (m *memVault) SaveBackup(_ context.Context, userID string, p []byte) (time.Time, error) {
	m.backups[userID] = p
	return time.Now(), nil
}

func (m *memVault) GetBackup(_ context.Context, userID string) ([]byte, time.Time, error) {
	p, ok := m.backups[userID]
	if !ok {
		return nil, time.Time{}, store.ErrNotFound
	}
	return p, time.Now(), nil
}

func (m *memVault) CreateShare(_ context.Context, s domain.Share) (domain.Share, error) {
	m.shares[s.Token] = s
	return s, nil
}

func (m *memVault) ListShares(_ context.Context, userID string) ([]domain.Share, error) {
	out := []domain.Share{}
	for _, s := range m.shares {
		if s.UserID == userID {
			out = append(out, s)
		}
	}
	return out, nil
}

func (m *memVault) RevokeShare(_ context.Context, userID, token string) error {
	if s, ok := m.shares[token]; !ok || s.UserID != userID {
		return store.ErrNotFound
	}
	delete(m.shares, token)
	return nil
}

func (m *memVault) GetShare(_ context.Context, token string) (domain.Share, error) {
	s, ok := m.shares[token]
	if !ok {
		return domain.Share{}, store.ErrNotFound
	}
	return s, nil
}

func asUser(req *http.Request, id string) *http.Request {
	return req.WithContext(context.WithValue(req.Context(), handlers.UserIDKey, id))
}

func TestVault_BackupShareAndPublicView(t *testing.T) {
	vault := newMemVault()
	h := handlers.NewVaultHandler(vault)

	w := httptest.NewRecorder()
	h.CreateShare(w, asUser(httptest.NewRequest(http.MethodPost, "/shares", strings.NewReader(`{"scope":"collection"}`)), "u1"))
	if w.Code != http.StatusConflict {
		t.Fatalf("share without backup: want 409, got %d", w.Code)
	}

	body := `{"items":[{"title":"Zelda","status":"OWNED","value":40,"notes":"secret"},{"title":"Pikmin","status":"WISHLIST"}]}`
	w = httptest.NewRecorder()
	h.PutBackup(w, asUser(httptest.NewRequest(http.MethodPut, "/backup", strings.NewReader(body)), "u1"))
	if w.Code != http.StatusOK {
		t.Fatalf("backup: want 200, got %d", w.Code)
	}

	w = httptest.NewRecorder()
	h.CreateShare(w, asUser(httptest.NewRequest(http.MethodPost, "/shares", strings.NewReader(`{"scope":"collection","hide_prices":true}`)), "u1"))
	if w.Code != http.StatusCreated {
		t.Fatalf("share: want 201, got %d", w.Code)
	}
	var share domain.Share
	json.NewDecoder(w.Body).Decode(&share)

	r := chi.NewRouter()
	r.Get("/public/shares/{token}", h.PublicShare)
	w = httptest.NewRecorder()
	r.ServeHTTP(w, httptest.NewRequest(http.MethodGet, "/public/shares/"+share.Token, nil))
	if w.Code != http.StatusOK {
		t.Fatalf("public: want 200, got %d", w.Code)
	}
	if strings.Contains(w.Body.String(), "secret") || strings.Contains(w.Body.String(), "Pikmin") || strings.Contains(w.Body.String(), `"value":40`) {
		t.Fatalf("public view leaks data: %s", w.Body.String())
	}

	w = httptest.NewRecorder()
	r.ServeHTTP(w, httptest.NewRequest(http.MethodGet, "/public/shares/short", nil))
	if w.Code != http.StatusNotFound {
		t.Fatalf("bad token: want 404, got %d", w.Code)
	}
}

func TestVault_RejectsInvalidBackup(t *testing.T) {
	h := handlers.NewVaultHandler(newMemVault())
	w := httptest.NewRecorder()
	h.PutBackup(w, asUser(httptest.NewRequest(http.MethodPut, "/backup", strings.NewReader(`[1,2]`)), "u1"))
	if w.Code != http.StatusBadRequest {
		t.Fatalf("want 400, got %d", w.Code)
	}
}
