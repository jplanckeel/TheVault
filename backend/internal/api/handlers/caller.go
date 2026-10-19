package handlers

import (
	"context"

	"github.com/jplanckeel/thevaultbo/internal/domain"
)

// Caller identifies who performs a request, for the sync audit log.
type Caller struct {
	Source string // domain.SourceMobile | SourceWeb | SourceAdmin
	UserID string
	Actor  string // e-mail, user ID or "api-key"
}

type callerKeyType struct{}

// WithCaller stores the caller in the request context.
func WithCaller(ctx context.Context, c Caller) context.Context {
	return context.WithValue(ctx, callerKeyType{}, c)
}

// CallerFrom returns the caller stored by the auth middlewares (zero value if absent).
func CallerFrom(ctx context.Context) Caller {
	c, _ := ctx.Value(callerKeyType{}).(Caller)
	if c.Source == "" {
		c.Source = domain.SourceWeb
	}
	return c
}
