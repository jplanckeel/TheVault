package handlers

import (
	"encoding/json"
	"errors"
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"

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
	if err := item.ValidateSync(); err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
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
	_ = json.NewEncoder(w).Encode(saved)
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
	_ = json.NewEncoder(w).Encode(items)
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
	_ = json.NewEncoder(w).Encode(item)
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
	_ = json.NewEncoder(w).Encode(snaps)
}

// UpdateCover handles PUT /api/v1/items/{id}/cover — manual cover URL update.
func (h *ItemsHandler) UpdateCover(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")
	var body struct {
		CoverURL string `json:"cover_url"`
	}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil || body.CoverURL == "" || !domain.ValidCoverURL(body.CoverURL) {
		http.Error(w, "cover_url must be an http(s) URL", http.StatusBadRequest)
		return
	}

	if err := h.items.UpdateCoverURL(r.Context(), id, body.CoverURL); err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(map[string]string{"cover_url": body.CoverURL})
}

// allowedCoverTypes maps sniffed MIME types to the extension used on disk.
var allowedCoverTypes = map[string]string{"image/jpeg": ".jpg", "image/png": ".png", "image/webp": ".webp"}

const maxCoverBytes = 8 << 20

// maxMultipartOverhead leaves room for the multipart boundaries and headers.
const maxMultipartOverhead = 64 << 10

// UploadCover handles POST /api/v1/items/{id}/cover/upload — file upload.
//
// The file name is derived from the item UUID (never from user input) and the image type
// is detected from the content, not from the client-provided extension.
func (h *ItemsHandler) UploadCover(w http.ResponseWriter, r *http.Request) {
	if h.uploadDir == "" {
		http.Error(w, "file upload not configured", http.StatusNotImplemented)
		return
	}

	id := chi.URLParam(r, "id")
	if !domain.IsUUID(id) {
		http.Error(w, "invalid item id", http.StatusBadRequest)
		return
	}
	if _, err := h.items.GetByID(r.Context(), id); err != nil {
		http.Error(w, "not found", http.StatusNotFound)
		return
	}

	r.Body = http.MaxBytesReader(w, r.Body, maxCoverBytes+maxMultipartOverhead)
	// #nosec G120 -- the body is capped by http.MaxBytesReader just above.
	if err := r.ParseMultipartForm(maxCoverBytes); err != nil {
		http.Error(w, "file too large", http.StatusRequestEntityTooLarge)
		return
	}
	file, header, err := r.FormFile("cover")
	if err != nil {
		http.Error(w, "cover file required", http.StatusBadRequest)
		return
	}
	defer file.Close()
	if header.Size > maxCoverBytes {
		http.Error(w, "file too large", http.StatusRequestEntityTooLarge)
		return
	}

	data, err := io.ReadAll(io.LimitReader(file, maxCoverBytes+1))
	if err != nil || len(data) > maxCoverBytes {
		http.Error(w, "file too large", http.StatusRequestEntityTooLarge)
		return
	}
	ext, ok := allowedCoverTypes[http.DetectContentType(data)]
	if !ok {
		http.Error(w, "unsupported file type (jpeg, png or webp)", http.StatusBadRequest)
		return
	}

	coversDir := filepath.Join(h.uploadDir, "covers")
	if err := os.MkdirAll(coversDir, 0o750); err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}
	// Writes are confined to the covers directory, whatever the file name.
	root, err := os.OpenRoot(coversDir)
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}
	defer root.Close()
	// Remove a previous cover with another extension so it is not left behind.
	for _, other := range allowedCoverTypes {
		if other != ext {
			_ = root.Remove(id + other)
		}
	}
	if err := root.WriteFile(id+ext, data, 0o600); err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}

	coverURL := "/covers/" + id + ext
	if err := h.items.UpdateCoverURL(r.Context(), id, coverURL); err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(map[string]string{"cover_url": coverURL})
}
