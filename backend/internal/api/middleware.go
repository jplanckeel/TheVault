package api

import (
	"context"
	"net/http"
	"strings"

	"github.com/golang-jwt/jwt/v5"
	"github.com/jplanckeel/thevaultbo/internal/api/handlers"
	"github.com/jplanckeel/thevaultbo/internal/domain"
	"github.com/jplanckeel/thevaultbo/internal/store"
)

func jwtSecret() []byte { return handlers.JWTSecret() }

// bearerSubject returns the subject of a valid HS256 bearer token, or "".
func bearerSubject(r *http.Request, secret []byte) string {
	auth := r.Header.Get("Authorization")
	if !strings.HasPrefix(auth, "Bearer ") {
		return ""
	}
	token, err := jwt.Parse(strings.TrimPrefix(auth, "Bearer "), func(t *jwt.Token) (interface{}, error) {
		return secret, nil
	}, jwt.WithValidMethods([]string{"HS256"}), jwt.WithExpirationRequired())
	if err != nil || !token.Valid {
		return ""
	}
	sub, _ := token.Claims.GetSubject()
	return sub
}

// UserAuthMiddleware extracts user_id from JWT and injects it into context.
func UserAuthMiddleware(next http.Handler) http.Handler {
	secret := jwtSecret()
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		sub := bearerSubject(r, secret)
		if sub == "" {
			http.Error(w, "unauthorized", http.StatusUnauthorized)
			return
		}
		ctx := context.WithValue(r.Context(), handlers.UserIDKey, sub)
		ctx = handlers.WithCaller(ctx, handlers.Caller{Source: domain.SourceWeb, UserID: sub, Actor: sub})
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// AnyAuthMiddleware accepts the mobile app (X-API-Key) or any logged-in user (JWT),
// and records which one in the request context for the sync audit log.
func AnyAuthMiddleware(next http.Handler) http.Handler {
	secret := jwtSecret()

	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if handlers.APIKeyMatches(r.Header.Get("X-API-Key")) {
			ctx := handlers.WithCaller(r.Context(), handlers.Caller{Source: domain.SourceMobile, Actor: "api-key"})
			next.ServeHTTP(w, r.WithContext(ctx))
			return
		}
		if sub := bearerSubject(r, secret); sub != "" {
			ctx := handlers.WithCaller(r.Context(), handlers.Caller{Source: domain.SourceWeb, UserID: sub, Actor: sub})
			next.ServeHTTP(w, r.WithContext(ctx))
			return
		}
		http.Error(w, "unauthorized", http.StatusUnauthorized)
	})
}

// AdminMiddleware only lets through users whose account has the admin role.
// The role is read from the database on every request so revoking it is immediate.
func AdminMiddleware(users store.UserStore) func(http.Handler) http.Handler {
	secret := jwtSecret()
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			sub := bearerSubject(r, secret)
			if sub == "" {
				http.Error(w, "unauthorized", http.StatusUnauthorized)
				return
			}
			user, err := users.GetByID(r.Context(), sub)
			if err != nil {
				http.Error(w, "unauthorized", http.StatusUnauthorized)
				return
			}
			if !user.IsAdmin {
				http.Error(w, "forbidden", http.StatusForbidden)
				return
			}
			ctx := context.WithValue(r.Context(), handlers.UserIDKey, user.ID)
			ctx = handlers.WithCaller(ctx, handlers.Caller{Source: domain.SourceAdmin, UserID: user.ID, Actor: user.Email})
			next.ServeHTTP(w, r.WithContext(ctx))
		})
	}
}
