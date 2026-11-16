// Package images stores catalogue covers on disk and imports them from the few image
// hosts the back-office suggests (eBay listings, IGDB, Wikimedia Commons).
package images

import (
	"context"
	"errors"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"os"
	"path/filepath"
	"regexp"
	"strings"
	"time"
)

// MaxCoverBytes caps an uploaded or imported cover.
const MaxCoverBytes = 8 << 20

// coverTypes maps sniffed MIME types to the extension used on disk.
var coverTypes = map[string]string{"image/jpeg": ".jpg", "image/png": ".png", "image/webp": ".webp"}

// ErrUnsupportedType is returned for content that is not a JPEG, PNG or WebP image.
var ErrUnsupportedType = errors.New("unsupported file type (jpeg, png or webp)")

// ErrTooLarge is returned for an image above MaxCoverBytes.
var ErrTooLarge = errors.New("file too large")

// ErrHostNotAllowed is returned when an import URL is not on an allowed image host.
var ErrHostNotAllowed = errors.New("image host not allowed")

// safeName restricts file names to what the callers generate (UUIDs, console-<slug>).
var safeName = regexp.MustCompile(`^[A-Za-z0-9-]{1,64}$`)

// SaveCover writes data under <uploadDir>/covers/<name>.<ext> (the extension comes from
// the sniffed content) and returns its public path, versioned so clients reload it.
func SaveCover(uploadDir, name string, data []byte) (string, error) {
	if !safeName.MatchString(name) {
		return "", fmt.Errorf("invalid cover name %q", name)
	}
	if len(data) > MaxCoverBytes {
		return "", ErrTooLarge
	}
	ext, ok := coverTypes[http.DetectContentType(data)]
	if !ok {
		return "", ErrUnsupportedType
	}
	dir := filepath.Join(uploadDir, "covers")
	if err := os.MkdirAll(dir, 0o750); err != nil {
		return "", fmt.Errorf("covers dir: %w", err)
	}
	// Writes are confined to the covers directory, whatever the file name.
	root, err := os.OpenRoot(dir)
	if err != nil {
		return "", fmt.Errorf("covers dir: %w", err)
	}
	defer root.Close()
	// Remove a previous cover with another extension so it is not left behind.
	for _, other := range coverTypes {
		if other != ext {
			_ = root.Remove(name + other)
		}
	}
	if err := root.WriteFile(name+ext, data, 0o600); err != nil {
		return "", fmt.Errorf("write cover: %w", err)
	}
	return fmt.Sprintf("/covers/%s%s?v=%d", name, ext, time.Now().Unix()), nil
}

// existingCover returns the public path of <name>.<ext> when it is already on disk.
func existingCover(uploadDir, name string) (string, bool) {
	for _, ext := range coverTypes {
		if _, err := os.Stat(filepath.Join(uploadDir, "covers", name+ext)); err == nil {
			return "/covers/" + name + ext, true
		}
	}
	return "", false
}

// allowedHosts are the image hosts covers may be imported from. Anything else is
// refused, so the import cannot be used to reach internal services.
var allowedHosts = []string{"i.ebayimg.com", "images.igdb.com", "upload.wikimedia.org", "commons.wikimedia.org"}

// AllowedURL reports whether raw is an https URL on an allowed image host.
func AllowedURL(raw string) bool {
	u, err := url.Parse(raw)
	if err != nil || u.Scheme != "https" || u.User != nil || u.Port() != "" {
		return false
	}
	host := strings.ToLower(u.Hostname())
	for _, h := range allowedHosts {
		if host == h {
			return true
		}
	}
	return false
}

// Importer downloads images from the allowed hosts into the covers directory.
type Importer struct {
	uploadDir string
	client    *http.Client
}

// NewImporter returns an Importer writing under uploadDir.
func NewImporter(uploadDir string) *Importer {
	return &Importer{
		uploadDir: uploadDir,
		client: &http.Client{
			Timeout: 20 * time.Second,
			// Redirects (Commons FilePath → upload.wikimedia.org) must stay on allowed hosts.
			CheckRedirect: func(req *http.Request, via []*http.Request) error {
				if len(via) >= 5 || !AllowedURL(req.URL.String()) {
					return ErrHostNotAllowed
				}
				return nil
			},
		},
	}
}

// Import downloads rawURL and stores it as the cover <name>. Returns its public path.
func (im *Importer) Import(ctx context.Context, name, rawURL string) (string, error) {
	if !AllowedURL(rawURL) {
		return "", ErrHostNotAllowed
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, rawURL, nil)
	if err != nil {
		return "", fmt.Errorf("build request: %w", err)
	}
	// Wikimedia asks automated clients for a descriptive User-Agent.
	req.Header.Set("User-Agent", "TheVault/1.0 (catalogue covers)")
	resp, err := im.client.Do(req)
	if err != nil {
		return "", fmt.Errorf("download: %w", err)
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		return "", fmt.Errorf("download: HTTP %d", resp.StatusCode)
	}
	data, err := io.ReadAll(io.LimitReader(resp.Body, MaxCoverBytes+1))
	if err != nil {
		return "", fmt.Errorf("download: %w", err)
	}
	return SaveCover(im.uploadDir, name, data)
}
