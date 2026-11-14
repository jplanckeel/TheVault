package handlers

import (
	"crypto/subtle"
	"errors"
	"fmt"
	"os"
)

// Minimum secret lengths enforced at startup.
const (
	minJWTSecretLen = 32
	minAPIKeyLen    = 16
)

// JWTSecret returns the HMAC key used to sign session tokens (JWT_SECRET).
// There is deliberately no fallback: CheckSecrets refuses to start without it.
func JWTSecret() []byte {
	return []byte(os.Getenv("JWT_SECRET"))
}

// CheckSecrets validates the secrets read from the environment. A missing or
// short JWT_SECRET would let anyone forge admin tokens, so the server must not start.
func CheckSecrets() error {
	var errs []error
	if n := len(os.Getenv("JWT_SECRET")); n < minJWTSecretLen {
		errs = append(errs, fmt.Errorf("JWT_SECRET must be at least %d characters (got %d) — generate one with `openssl rand -hex 32`", minJWTSecretLen, n))
	}
	if key := os.Getenv("API_KEY"); key != "" && len(key) < minAPIKeyLen {
		errs = append(errs, fmt.Errorf("API_KEY must be at least %d characters when set", minAPIKeyLen))
	}
	return errors.Join(errs...)
}

// APIKeyMatches compares the mobile API key in constant time. An empty API_KEY disables the key.
func APIKeyMatches(candidate string) bool {
	expected := os.Getenv("API_KEY")
	if expected == "" || candidate == "" {
		return false
	}
	return subtle.ConstantTimeCompare([]byte(candidate), []byte(expected)) == 1
}
