package handlers

import (
	"strings"
	"testing"
)

func TestCheckSecrets(t *testing.T) {
	t.Setenv("JWT_SECRET", "")
	t.Setenv("API_KEY", "")
	if err := CheckSecrets(); err == nil || !strings.Contains(err.Error(), "JWT_SECRET") {
		t.Errorf("missing JWT_SECRET must be rejected, got %v", err)
	}
	t.Setenv("JWT_SECRET", strings.Repeat("x", 32))
	if err := CheckSecrets(); err != nil {
		t.Errorf("valid config rejected: %v", err)
	}
	t.Setenv("API_KEY", "short")
	if err := CheckSecrets(); err == nil {
		t.Error("short API_KEY must be rejected")
	}
}

func TestAPIKeyMatches(t *testing.T) {
	t.Setenv("API_KEY", "")
	if APIKeyMatches("") || APIKeyMatches("anything") {
		t.Error("an empty API_KEY must never match")
	}
	t.Setenv("API_KEY", "0123456789abcdef")
	if !APIKeyMatches("0123456789abcdef") || APIKeyMatches("0123456789abcdeX") {
		t.Error("constant-time comparison returned a wrong result")
	}
}

func TestValidateRegistration(t *testing.T) {
	cases := map[string]struct {
		email, password, name string
		ok                    bool
	}{
		"valid":          {"a@b.fr", "longenough", "Jérémy", true},
		"bad email":      {"not-an-email", "longenough", "", false},
		"short password": {"a@b.fr", "short", "", false},
		"long password":  {"a@b.fr", strings.Repeat("p", 73), "", false},
		"long name":      {"a@b.fr", "longenough", strings.Repeat("é", 101), false},
	}
	for name, c := range cases {
		if got := validateRegistration(c.email, c.password, c.name) == ""; got != c.ok {
			t.Errorf("%s: valid = %v, want %v", name, got, c.ok)
		}
	}
}
