package handlers

import (
	"context"
	"encoding/json"
	"errors"
	"log/slog"
	"net/http"
	"sync"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/jplanckeel/thevaultbo/internal/domain"
	"github.com/jplanckeel/thevaultbo/internal/images"
	"github.com/jplanckeel/thevaultbo/internal/store"
)

// ImageSource suggests covers for an item (eBay listings, IGDB editions…).
type ImageSource interface {
	Name() string
	Candidates(ctx context.Context, item domain.Item) ([]domain.ImageCandidate, error)
}

// CoverImporter downloads an image from an allowed host and stores it as a cover.
type CoverImporter interface {
	Import(ctx context.Context, name, rawURL string) (string, error)
}

// ConsolePhotoJob applies the free photos of console models (images.ConsolePhotos).
type ConsolePhotoJob interface {
	Run(ctx context.Context) (images.ConsolePhotosResult, error)
}

// ImagesHandler serves the cover suggestions of the back-office.
type ImagesHandler struct {
	items         store.ItemStore
	sources       []ImageSource
	importer      CoverImporter
	consolePhotos ConsolePhotoJob
}

// WithConsolePhotos enables POST /admin/console-photos.
func (h *ImagesHandler) WithConsolePhotos(job ConsolePhotoJob) *ImagesHandler {
	h.consolePhotos = job
	return h
}

// RunConsolePhotos handles POST /api/v1/admin/console-photos — applies the console photos
// now instead of waiting for the hourly run, and returns what changed.
func (h *ImagesHandler) RunConsolePhotos(w http.ResponseWriter, r *http.Request) {
	if h.consolePhotos == nil {
		http.Error(w, "console photos not configured", http.StatusNotImplemented)
		return
	}
	ctx, cancel := context.WithTimeout(r.Context(), consolePhotosTimeout)
	defer cancel()
	res, err := h.consolePhotos.Run(ctx)
	if err != nil {
		slog.Error("console photos", "err", err)
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}
	writeJSON(w, http.StatusOK, res)
}

// consolePhotosTimeout bounds a manual run (a few dozen downloads at most).
const consolePhotosTimeout = 2 * time.Minute

// NewImagesHandler creates an ImagesHandler.
func NewImagesHandler(items store.ItemStore, importer CoverImporter, sources ...ImageSource) *ImagesHandler {
	return &ImagesHandler{items: items, sources: sources, importer: importer}
}

type candidatesResponse struct {
	Candidates []domain.ImageCandidate `json:"candidates"`
	// Unavailable lists the sources that failed (not configured, quota, network).
	Unavailable []string `json:"unavailable"`
}

// Candidates handles GET /api/v1/admin/items/{id}/images — cover suggestions, queried
// from every source in parallel. A failing source is reported, not fatal.
func (h *ImagesHandler) Candidates(w http.ResponseWriter, r *http.Request) {
	item, ok := h.item(w, r)
	if !ok {
		return
	}
	results := make([][]domain.ImageCandidate, len(h.sources))
	failed := make([]bool, len(h.sources))
	var wg sync.WaitGroup
	for i, src := range h.sources {
		wg.Add(1)
		go func() {
			defer wg.Done()
			c, err := src.Candidates(r.Context(), item)
			if err != nil {
				slog.Warn("image candidates", "source", src.Name(), "item", item.ID, "err", err)
				failed[i] = true
				return
			}
			results[i] = c
		}()
	}
	wg.Wait()

	resp := candidatesResponse{Candidates: []domain.ImageCandidate{}, Unavailable: []string{}}
	// Commons first (free licence), then IGDB, then eBay: the order of the sources.
	if photo, ok := images.PhotoForConsole(item.Title, item.Platform); ok && item.Type == domain.ItemTypeConsole {
		resp.Candidates = append(resp.Candidates, domain.ImageCandidate{
			URL: photo.URL(), Source: "commons", Label: photo.File + " (domaine public)", PageURL: photo.PageURL(),
		})
	}
	for i, src := range h.sources {
		if failed[i] {
			resp.Unavailable = append(resp.Unavailable, src.Name())
		}
		resp.Candidates = append(resp.Candidates, results[i]...)
	}
	writeJSON(w, http.StatusOK, resp)
}

type importCoverRequest struct {
	URL string `json:"url"`
}

// ImportCover handles POST /api/v1/admin/items/{id}/cover/import {url} — downloads a
// suggested image (allowed hosts only), stores it and makes it the item cover. Like an
// upload, the cover is then protected from syncs and IGDB.
func (h *ImagesHandler) ImportCover(w http.ResponseWriter, r *http.Request) {
	item, ok := h.item(w, r)
	if !ok {
		return
	}
	var req importCoverRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil || !images.AllowedURL(req.URL) {
		http.Error(w, "url must be an https image from eBay, IGDB or Wikimedia Commons", http.StatusBadRequest)
		return
	}
	cover, err := h.importer.Import(r.Context(), item.ID, req.URL)
	switch {
	case errors.Is(err, images.ErrUnsupportedType), errors.Is(err, images.ErrTooLarge):
		http.Error(w, err.Error(), http.StatusUnprocessableEntity)
		return
	case err != nil:
		slog.Warn("cover import", "item", item.ID, "err", err)
		http.Error(w, "download failed", http.StatusBadGateway)
		return
	}
	if err := h.items.UpdateCoverURL(r.Context(), item.ID, cover); err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}
	writeJSON(w, http.StatusOK, map[string]string{"cover_url": cover})
}

func (h *ImagesHandler) item(w http.ResponseWriter, r *http.Request) (domain.Item, bool) {
	id := chi.URLParam(r, "id")
	if !domain.IsUUID(id) {
		http.Error(w, "invalid item id", http.StatusBadRequest)
		return domain.Item{}, false
	}
	item, err := h.items.GetByID(r.Context(), id)
	if err != nil {
		writeStoreError(w, err)
		return domain.Item{}, false
	}
	return item, true
}
