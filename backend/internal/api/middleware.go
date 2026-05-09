package api

import (
	"net/http"
	"os"
	"strings"

	"github.com/golang-jwt/jwt/v5"
)

// AuthMiddleware rejects requests that don't carry a valid JWT (Authorization: Bearer <jwt>)
// or a valid API key (X-API-Key header).
func AuthMiddleware(next http.Handler) http.Handler {
	secret := []byte(os.Getenv("JWT_SECRET"))
	if len(secret) == 0 {
		secret = []byte("thevaultbo-default-secret")
	}
	apiKey := os.Getenv("API_KEY")

	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// Check X-API-Key (mobile)
		if key := r.Header.Get("X-API-Key"); key != "" && apiKey != "" && key == apiKey {
			next.ServeHTTP(w, r)
			return
		}

		// Check Authorization: Bearer <jwt> (BO frontend)
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
