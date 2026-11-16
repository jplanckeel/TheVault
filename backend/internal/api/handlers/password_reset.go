package handlers

import (
	"context"
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"time"

	"github.com/jplanckeel/thevaultbo/internal/store"
	"golang.org/x/crypto/bcrypt"
)

// ResetLinkTTL is how long a password reset link works.
const ResetLinkTTL = time.Hour

// HashResetToken returns the value stored for a reset token (the token itself is never stored).
func HashResetToken(token string) string {
	sum := sha256.Sum256([]byte(token))
	return hex.EncodeToString(sum[:])
}

// NewResetLink records a one-time reset link for the account e-mail and returns its
// token, to put in the link: <site>/reset-password#token=<token>.
func NewResetLink(ctx context.Context, users store.UserStore, resets store.PasswordResetStore, email string, now time.Time) (string, store.User, error) {
	user, err := users.GetByEmail(ctx, email)
	if err != nil {
		return "", store.User{}, fmt.Errorf("no account for %q", email)
	}
	raw := make([]byte, 32)
	if _, err := rand.Read(raw); err != nil {
		return "", store.User{}, fmt.Errorf("random: %w", err)
	}
	token := base64.RawURLEncoding.EncodeToString(raw)
	if err := resets.CreateReset(ctx, user.ID, HashResetToken(token), now.Add(ResetLinkTTL)); err != nil {
		return "", store.User{}, err
	}
	return token, user, nil
}

// WithResets enables POST /auth/reset-password.
func (h *UserAuthHandler) WithResets(resets store.PasswordResetStore) *UserAuthHandler {
	h.resets = resets
	return h
}

// ResetPassword handles POST /api/v1/auth/reset-password {token, password}: sets the new
// password of a valid link and signs the user in.
func (h *UserAuthHandler) ResetPassword(w http.ResponseWriter, r *http.Request) {
	if h.resets == nil {
		http.Error(w, "password reset not configured", http.StatusNotImplemented)
		return
	}
	var body struct {
		Token    string `json:"token"`
		Password string `json:"password"`
	}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil || body.Token == "" {
		http.Error(w, "invalid body", http.StatusBadRequest)
		return
	}
	if msg := validatePassword(body.Password); msg != "" {
		http.Error(w, msg, http.StatusBadRequest)
		return
	}
	hash, err := bcrypt.GenerateFromPassword([]byte(body.Password), bcrypt.DefaultCost)
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}
	user, err := h.resets.ConsumeReset(r.Context(), HashResetToken(body.Token), string(hash))
	if errors.Is(err, store.ErrResetInvalid) {
		http.Error(w, "lien invalide, déjà utilisé ou expiré", http.StatusBadRequest)
		return
	}
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}
	h.promoteIfListed(r, &user)
	token, _ := h.signToken(user.ID)
	writeJSON(w, http.StatusOK, map[string]any{"token": token, "user": userJSON(user)})
}
