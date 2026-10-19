package handlers

import (
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strings"

	"github.com/go-chi/chi/v5"
	"github.com/jplanckeel/thevaultbo/internal/domain"
	"github.com/jplanckeel/thevaultbo/internal/store"
)

// ItemsHandler handles HTTP requests for items.
type ItemsHandler struct {
	items     store.ItemStore
	prices    store.PriceStore
	events    store.SyncEventStore // optional: audit log of syncs
	uploadDir string               // absolute path where uploaded covers are stored
}

// WithSyncEvents enables the sync audit log on UpsertItem.
func (h *ItemsHandler) WithSyncEvents(events store.SyncEventStore) *ItemsHandler {
	h.events = events
	return h
}

// recordSync logs a catalogue write; failures are logged but never fail the request.
func (h *ItemsHandler) recordSync(r *http.Request, action string, item domain.Item, detail string) {
	if h.events == nil {
		return
	}
	caller := CallerFrom(r.Context())
	e := domain.SyncEvent{
		Source: caller.Source, Action: action, ExternalID: item.ExternalID,
		Title: item.Title, Actor: caller.Actor, Detail: detail,
	}
	if item.ID != "" {
		e.ItemID = &item.ID
	}
	if err := h.events.Record(r.Context(), e); err != nil {
		log.Printf("sync event: %v", err)
	}
}

// NewItemsHandler creates a new ItemsHandler.
func NewItemsHandler(items store.ItemStore, prices store.PriceStore, uploadDir ...string) *ItemsHandler {
	dir := ""
	if len(uploadDir) > 0 {
		dir = uploadDir[0]
	}
	return &ItemsHandler{items: items, prices: prices, uploadDir: dir}
}

// UpsertItem handles POST /api/v1/items — upsert from mobile app.
func (h *ItemsHandler) UpsertItem(w http.ResponseWriter, r *http.Request) {
	var item domain.Item
	if err := json.NewDecoder(r.Body).Decode(&item); err != nil {
		http.Error(w, "invalid body", http.StatusBadRequest)
		return
	}
	if item.ExternalID == "" {
		http.Error(w, "external_id is required", http.StatusBadRequest)
		return
	}

	saved, err := h.items.Upsert(r.Context(), item)
	if errors.Is(err, store.ErrTombstoned) {
		h.recordSync(r, domain.ActionRejected, item, "item supprimé par un admin")
		http.Error(w, "item deleted by an admin", http.StatusGone)
		return
	}
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}
	action := domain.ActionUpdated
	if saved.CreatedAt.Equal(saved.UpdatedAt) {
		action = domain.ActionCreated
	}
	h.recordSync(r, action, saved, "")

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(saved)
}

// ListItems handles GET /api/v1/items.
func (h *ItemsHandler) ListItems(w http.ResponseWriter, r *http.Request) {
	items, err := h.items.List(r.Context())
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}
	if items == nil {
		items = []domain.Item{}
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(items)
}

// GetItem handles GET /api/v1/items/{id}.
func (h *ItemsHandler) GetItem(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")
	item, err := h.items.GetByID(r.Context(), id)
	if err != nil {
		if errors.Is(err, store.ErrNotFound) {
			http.Error(w, "not found", http.StatusNotFound)
		} else {
			http.Error(w, "internal error", http.StatusInternalServerError)
		}
		return
	}
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(item)
}

// GetItemPrices handles GET /api/v1/items/{id}/prices.
func (h *ItemsHandler) GetItemPrices(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")
	snaps, err := h.prices.ListByItemID(r.Context(), id)
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}
	if snaps == nil {
		snaps = []domain.PriceSnapshot{}
	}
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(snaps)
}

// UpdateCover handles PUT /api/v1/items/{id}/cover — manual cover URL update.
func (h *ItemsHandler) UpdateCover(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")
	var body struct {
		CoverURL string `json:"cover_url"`
	}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil || body.CoverURL == "" {
		http.Error(w, "cover_url is required", http.StatusBadRequest)
		return
	}

	if err := h.items.UpdateCoverURL(r.Context(), id, body.CoverURL); err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]string{"cover_url": body.CoverURL})
}

// UploadCover handles POST /api/v1/items/{id}/cover/upload — file upload.
func (h *ItemsHandler) UploadCover(w http.ResponseWriter, r *http.Request) {
	if h.uploadDir == "" {
		http.Error(w, "file upload not configured", http.StatusNotImplemented)
		return
	}

	id := chi.URLParam(r, "id")

	if err := r.ParseMultipartForm(8 << 20); err != nil { // 8 MB limit
		http.Error(w, "file too large", http.StatusBadRequest)
		return
	}

	file, header, err := r.FormFile("cover")
	if err != nil {
		http.Error(w, "cover file required", http.StatusBadRequest)
		return
	}
	defer file.Close()

	ext := strings.ToLower(filepath.Ext(header.Filename))
	allowed := map[string]bool{".jpg": true, ".jpeg": true, ".png": true, ".webp": true}
	if !allowed[ext] {
		http.Error(w, "unsupported file type", http.StatusBadRequest)
		return
	}

	coversDir := filepath.Join(h.uploadDir, "covers")
	if err := os.MkdirAll(coversDir, 0755); err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}

	// Use item ID as filename to allow replacement without orphaned files.
	dest := filepath.Join(coversDir, fmt.Sprintf("%s%s", id, ext))
	out, err := os.Create(dest)
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}
	defer out.Close()

	if _, err := io.Copy(out, file); err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}

	coverURL := fmt.Sprintf("/covers/%s%s", id, ext)
	if err := h.items.UpdateCoverURL(r.Context(), id, coverURL); err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]string{"cover_url": coverURL})
}
