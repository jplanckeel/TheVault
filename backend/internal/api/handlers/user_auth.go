package handlers

import (
	"encoding/json"
	"net/http"
	"os"
	"strings"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"github.com/jplanckeel/thevaultbo/internal/store"
	"golang.org/x/crypto/bcrypt"
)

// UserAuthHandler handles user registration, login, and profile.
type UserAuthHandler struct {
	users       store.UserStore
	secret      []byte
	adminEmails []string
}

// NewUserAuthHandler creates the handler. adminEmails (lower-case) get the admin role
// when they register or log in — see ADMIN_EMAILS.
func NewUserAuthHandler(users store.UserStore, adminEmails ...string) *UserAuthHandler {
	secret := os.Getenv("JWT_SECRET")
	if secret == "" {
		secret = "thevaultbo-default-secret"
	}
	return &UserAuthHandler{users: users, secret: []byte(secret), adminEmails: adminEmails}
}

// promoteIfListed grants the admin role when the user's e-mail is in ADMIN_EMAILS.
func (h *UserAuthHandler) promoteIfListed(r *http.Request, user *store.User) {
	if user.IsAdmin {
		return
	}
	for _, e := range h.adminEmails {
		if strings.EqualFold(e, user.Email) {
			if n, err := h.users.PromoteAdmins(r.Context(), []string{strings.ToLower(user.Email)}); err == nil && n > 0 {
				user.IsAdmin = true
			}
			return
		}
	}
}

func userJSON(u store.User) map[string]any {
	return map[string]any{"id": u.ID, "email": u.Email, "name": u.Name, "is_admin": u.IsAdmin}
}

func (h *UserAuthHandler) Register(w http.ResponseWriter, r *http.Request) {
	var body struct {
		Email    string `json:"email"`
		Password string `json:"password"`
		Name     string `json:"name"`
	}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		http.Error(w, "invalid body", http.StatusBadRequest)
		return
	}
	if body.Email == "" || body.Password == "" {
		http.Error(w, "email and password required", http.StatusBadRequest)
		return
	}

	hash, err := bcrypt.GenerateFromPassword([]byte(body.Password), bcrypt.DefaultCost)
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}

	user, err := h.users.Create(r.Context(), body.Email, string(hash), body.Name)
	if err != nil {
		if strings.Contains(err.Error(), "duplicate") || strings.Contains(err.Error(), "unique") {
			http.Error(w, "email already registered", http.StatusConflict)
			return
		}
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}

	h.promoteIfListed(r, &user)
	token, _ := h.signToken(user.ID)
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(map[string]any{"token": token, "user": userJSON(user)})
}

func (h *UserAuthHandler) Login(w http.ResponseWriter, r *http.Request) {
	var body struct {
		Email    string `json:"email"`
		Password string `json:"password"`
	}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		http.Error(w, "invalid body", http.StatusBadRequest)
		return
	}

	user, err := h.users.GetByEmail(r.Context(), body.Email)
	if err != nil {
		http.Error(w, "invalid credentials", http.StatusUnauthorized)
		return
	}

	if bcrypt.CompareHashAndPassword([]byte(user.PasswordHash), []byte(body.Password)) != nil {
		http.Error(w, "invalid credentials", http.StatusUnauthorized)
		return
	}

	h.promoteIfListed(r, &user)
	token, _ := h.signToken(user.ID)
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]any{"token": token, "user": userJSON(user)})
}

func (h *UserAuthHandler) Me(w http.ResponseWriter, r *http.Request) {
	userID := r.Context().Value(UserIDKey).(string)
	user, err := h.users.GetByID(r.Context(), userID)
	if err != nil {
		http.Error(w, "not found", http.StatusNotFound)
		return
	}
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(userJSON(user))
}

func (h *UserAuthHandler) signToken(userID string) (string, error) {
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
		"sub": userID,
		"exp": time.Now().Add(7 * 24 * time.Hour).Unix(),
	})
	return token.SignedString(h.secret)
}

// ContextKey for user ID in request context.
type contextKey string

const UserIDKey contextKey = "user_id"
