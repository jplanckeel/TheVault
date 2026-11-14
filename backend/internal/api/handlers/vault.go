package handlers

import (
	"crypto/rand"
	"encoding/base64"
	"encoding/json"
	"errors"
	"io"
	"net/http"
	"regexp"

	"github.com/go-chi/chi/v5"
	"github.com/jplanckeel/thevaultbo/internal/domain"
	"github.com/jplanckeel/thevaultbo/internal/store"
)

var tokenPattern = regexp.MustCompile(`^[A-Za-z0-9_-]{20,64}$`)

// VaultHandler serves the mobile backup and the public share links.
type VaultHandler struct {
	vaults store.VaultStore
}

// NewVaultHandler creates a VaultHandler.
func NewVaultHandler(vaults store.VaultStore) *VaultHandler {
	return &VaultHandler{vaults: vaults}
}

// PutBackup handles PUT /api/v1/backup — stores the collection backup of the user.
func (h *VaultHandler) PutBackup(w http.ResponseWriter, r *http.Request) {
	userID := r.Context().Value(UserIDKey).(string)
	payload, err := io.ReadAll(r.Body)
	if err != nil {
		http.Error(w, "invalid body", http.StatusBadRequest)
		return
	}
	if err := domain.ValidateBackup(payload); err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}
	at, err := h.vaults.SaveBackup(r.Context(), userID, payload)
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}
	writeJSON(w, http.StatusOK, map[string]any{"updated_at": at})
}

// GetBackup handles GET /api/v1/backup — returns the backup of the user.
func (h *VaultHandler) GetBackup(w http.ResponseWriter, r *http.Request) {
	userID := r.Context().Value(UserIDKey).(string)
	payload, _, err := h.vaults.GetBackup(r.Context(), userID)
	if errors.Is(err, store.ErrNotFound) {
		http.Error(w, "no backup", http.StatusNotFound)
		return
	}
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}
	w.Header().Set("Content-Type", "application/json")
	w.Write(payload)
}

// CreateShare handles POST /api/v1/shares — creates a read-only public link.
func (h *VaultHandler) CreateShare(w http.ResponseWriter, r *http.Request) {
	userID := r.Context().Value(UserIDKey).(string)
	var body struct {
		Scope      string `json:"scope"`
		Title      string `json:"title"`
		HidePrices bool   `json:"hide_prices"`
	}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		http.Error(w, "invalid body", http.StatusBadRequest)
		return
	}
	if err := domain.ValidateShare(body.Scope, body.Title); err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}
	if _, _, err := h.vaults.GetBackup(r.Context(), userID); err != nil {
		http.Error(w, "upload a backup first", http.StatusConflict)
		return
	}
	token, err := newToken()
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}
	share, err := h.vaults.CreateShare(r.Context(), domain.Share{Token: token, UserID: userID, Scope: body.Scope, Title: body.Title, HidePrices: body.HidePrices})
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}
	writeJSON(w, http.StatusCreated, share)
}

// ListShares handles GET /api/v1/shares.
func (h *VaultHandler) ListShares(w http.ResponseWriter, r *http.Request) {
	userID := r.Context().Value(UserIDKey).(string)
	shares, err := h.vaults.ListShares(r.Context(), userID)
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}
	writeJSON(w, http.StatusOK, shares)
}

// RevokeShare handles DELETE /api/v1/shares/{token}.
func (h *VaultHandler) RevokeShare(w http.ResponseWriter, r *http.Request) {
	userID := r.Context().Value(UserIDKey).(string)
	err := h.vaults.RevokeShare(r.Context(), userID, chi.URLParam(r, "token"))
	if errors.Is(err, store.ErrNotFound) {
		http.Error(w, "not found", http.StatusNotFound)
		return
	}
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

// PublicShare handles GET /api/v1/public/shares/{token} — no authentication.
func (h *VaultHandler) PublicShare(w http.ResponseWriter, r *http.Request) {
	token := chi.URLParam(r, "token")
	if !tokenPattern.MatchString(token) {
		http.Error(w, "not found", http.StatusNotFound)
		return
	}
	share, err := h.vaults.GetShare(r.Context(), token)
	if err != nil {
		http.Error(w, "not found", http.StatusNotFound)
		return
	}
	payload, at, err := h.vaults.GetBackup(r.Context(), share.UserID)
	if err != nil {
		http.Error(w, "not found", http.StatusNotFound)
		return
	}
	view, err := domain.BuildPublicView(share, payload, at)
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}
	w.Header().Set("Cache-Control", "public, max-age=300")
	writeJSON(w, http.StatusOK, view)
}

// newToken returns 24 random bytes in URL-safe base64 (32 characters).
func newToken() (string, error) {
	b := make([]byte, 24)
	if _, err := rand.Read(b); err != nil {
		return "", err
	}
	return base64.RawURLEncoding.EncodeToString(b), nil
}
