package api

import (
	"context"
	"net/http"
	"os"
	"strings"

	"github.com/golang-jwt/jwt/v5"
	"github.com/jplanckeel/thevaultbo/internal/api/handlers"
)

func jwtSecret() []byte {
	secret := []byte(os.Getenv("JWT_SECRET"))
	if len(secret) == 0 {
		secret = []byte("thevaultbo-default-secret")
	}
	return secret
}

// AuthMiddleware rejects requests that don't carry a valid JWT or API key (admin routes).
func AuthMiddleware(next http.Handler) http.Handler {
	secret := jwtSecret()
	apiKey := os.Getenv("API_KEY")

	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if key := r.Header.Get("X-API-Key"); key != "" && apiKey != "" && key == apiKey {
			next.ServeHTTP(w, r)
			return
		}

		auth := r.Header.Get("Authorization")
		if strings.HasPrefix(auth, "Bearer ") {
			tokenStr := strings.TrimPrefix(auth, "Bearer ")
			token, err := jwt.Parse(tokenStr, func(t *jwt.Token) (interface{}, error) {
				return secret, nil
			}, jwt.WithValidMethods([]string{"HS256"}))
			if err == nil && token.Valid {
				next.ServeHTTP(w, r)
				return
			}
		}

		http.Error(w, "unauthorized", http.StatusUnauthorized)
	})
}

// UserAuthMiddleware extracts user_id from JWT and injects it into context.
func UserAuthMiddleware(next http.Handler) http.Handler {
	secret := jwtSecret()

	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		auth := r.Header.Get("Authorization")
		if !strings.HasPrefix(auth, "Bearer ") {
			http.Error(w, "unauthorized", http.StatusUnauthorized)
			return
		}

		tokenStr := strings.TrimPrefix(auth, "Bearer ")
		token, err := jwt.Parse(tokenStr, func(t *jwt.Token) (interface{}, error) {
			return secret, nil
		}, jwt.WithValidMethods([]string{"HS256"}))
		if err != nil || !token.Valid {
			http.Error(w, "unauthorized", http.StatusUnauthorized)
			return
		}

		claims, ok := token.Claims.(jwt.MapClaims)
		if !ok {
			http.Error(w, "unauthorized", http.StatusUnauthorized)
			return
		}

		sub, _ := claims.GetSubject()
		if sub == "" {
			http.Error(w, "unauthorized", http.StatusUnauthorized)
			return
		}

		ctx := context.WithValue(r.Context(), handlers.UserIDKey, sub)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// AnyAuthMiddleware accepts any authenticated caller: X-API-Key (mobile),
// admin JWT (web admin), or user JWT (web user). It does NOT inject a user_id
// into context, so it should only be used for routes that don't depend on
// the caller's identity (e.g. shared catalogue mutations).
func AnyAuthMiddleware(next http.Handler) http.Handler {
	secret := jwtSecret()
	apiKey := os.Getenv("API_KEY")

	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// 1) X-API-Key (mobile app)
		if key := r.Header.Get("X-API-Key"); key != "" && apiKey != "" && key == apiKey {
			next.ServeHTTP(w, r)
			return
		}

		// 2) Bearer JWT — admin or user, both valid
		auth := r.Header.Get("Authorization")
		if strings.HasPrefix(auth, "Bearer ") {
			tokenStr := strings.TrimPrefix(auth, "Bearer ")
			token, err := jwt.Parse(tokenStr, func(t *jwt.Token) (interface{}, error) {
				return secret, nil
			}, jwt.WithValidMethods([]string{"HS256"}))
			if err == nil && token.Valid {
				next.ServeHTTP(w, r)
				return
			}
		}

		http.Error(w, "unauthorized", http.StatusUnauthorized)
	})
}
