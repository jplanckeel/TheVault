package handlers

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"log"
	"net/http"
	"strconv"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/jplanckeel/thevaultbo/internal/domain"
	"github.com/jplanckeel/thevaultbo/internal/store"
)

// IGDBSyncer enriches a set of items and tracks progress on a job (implemented by igdb.Runner).
type IGDBSyncer interface {
	Sync(ctx context.Context, job domain.ScrapeJob, items []domain.Item)
}

// AdminHandler serves the back-office API (/api/v1/admin/*).
type AdminHandler struct {
	items  store.ItemStore
	admin  store.AdminItemStore
	events store.SyncEventStore
	jobs   store.JobStore
	igdb   IGDBSyncer
}

// NewAdminHandler creates an AdminHandler. igdb may be nil (single-item refresh disabled).
func NewAdminHandler(items store.ItemStore, admin store.AdminItemStore, events store.SyncEventStore, jobs store.JobStore, igdb IGDBSyncer) *AdminHandler {
	return &AdminHandler{items: items, admin: admin, events: events, jobs: jobs, igdb: igdb}
}

func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(v)
}

// writeStoreError maps store errors to HTTP statuses.
func writeStoreError(w http.ResponseWriter, err error) {
	if errors.Is(err, store.ErrNotFound) {
		http.Error(w, "not found", http.StatusNotFound)
		return
	}
	log.Printf("admin: %v", err)
	http.Error(w, "internal error", http.StatusInternalServerError)
}

// record logs an admin action in the sync audit log.
func (h *AdminHandler) record(r *http.Request, action string, item domain.Item, detail string) {
	caller := CallerFrom(r.Context())
	e := domain.SyncEvent{
		Source: domain.SourceAdmin, Action: action, ExternalID: item.ExternalID,
		Title: item.Title, Actor: caller.Actor, Detail: detail,
	}
	if item.ID != "" && action != domain.ActionDeleted {
		e.ItemID = &item.ID
	}
	if err := h.events.Record(r.Context(), e); err != nil {
		log.Printf("admin audit: %v", err)
	}
}

// ListItems handles GET /admin/items — every item with its data quality issues.
func (h *AdminHandler) ListItems(w http.ResponseWriter, r *http.Request) {
	items, err := h.items.List(r.Context())
	if err != nil {
		writeStoreError(w, err)
		return
	}
	priced, err := h.admin.PricedItemIDs(r.Context())
	if err != nil {
		writeStoreError(w, err)
		return
	}
	owners, err := h.admin.OwnerCounts(r.Context())
	if err != nil {
		writeStoreError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, domain.BuildAdminItems(items, priced, owners))
}

// UpdateItem handles PUT /admin/items/{id} — partial correction of catalogue fields.
func (h *AdminHandler) UpdateItem(w http.ResponseWriter, r *http.Request) {
	var patch domain.ItemPatch
	if err := json.NewDecoder(r.Body).Decode(&patch); err != nil {
		http.Error(w, "invalid body", http.StatusBadRequest)
		return
	}
	if err := patch.Validate(); err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}
	item, err := h.admin.Update(r.Context(), chi.URLParam(r, "id"), patch)
	if err != nil {
		writeStoreError(w, err)
		return
	}
	h.record(r, domain.ActionEdited, item, "")
	writeJSON(w, http.StatusOK, item)
}

// DeleteItem handles DELETE /admin/items/{id}. Later syncs of the same item are rejected.
func (h *AdminHandler) DeleteItem(w http.ResponseWriter, r *http.Request) {
	item, err := h.admin.Delete(r.Context(), chi.URLParam(r, "id"), CallerFrom(r.Context()).Actor)
	if err != nil {
		writeStoreError(w, err)
		return
	}
	h.record(r, domain.ActionDeleted, item, "")
	w.WriteHeader(http.StatusNoContent)
}

// MergeItem handles POST /admin/items/{id}/merge {"target_id": "…"} — folds {id} into target.
func (h *AdminHandler) MergeItem(w http.ResponseWriter, r *http.Request) {
	var body struct {
		TargetID string `json:"target_id"`
	}
	sourceID := chi.URLParam(r, "id")
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil || body.TargetID == "" || body.TargetID == sourceID {
		http.Error(w, "target_id is required and must differ from the item", http.StatusBadRequest)
		return
	}
	source, err := h.items.GetByID(r.Context(), sourceID)
	if err != nil {
		writeStoreError(w, err)
		return
	}
	target, err := h.admin.Merge(r.Context(), sourceID, body.TargetID)
	if err != nil {
		writeStoreError(w, err)
		return
	}
	h.record(r, domain.ActionMerged, target, fmt.Sprintf("fusion de « %s » (%s)", source.Title, source.ExternalID))
	writeJSON(w, http.StatusOK, target)
}

// RefreshIGDB handles POST /admin/items/{id}/igdb — re-runs IGDB enrichment for one item.
func (h *AdminHandler) RefreshIGDB(w http.ResponseWriter, r *http.Request) {
	if h.igdb == nil {
		http.Error(w, "IGDB is not configured", http.StatusNotImplemented)
		return
	}
	item, err := h.items.GetByID(r.Context(), chi.URLParam(r, "id"))
	if err != nil {
		writeStoreError(w, err)
		return
	}
	job, err := h.jobs.Create(r.Context(), domain.ScrapeJob{
		JobType: "igdb", TriggeredBy: CallerFrom(r.Context()).Actor, Status: "running", ItemsTotal: 1,
	})
	if err != nil {
		writeStoreError(w, err)
		return
	}
	go h.igdb.Sync(context.WithoutCancel(r.Context()), job, []domain.Item{item})
	writeJSON(w, http.StatusAccepted, job)
}

// ListSyncEvents handles GET /admin/sync-events?source=&action=&item_id=&limit=.
func (h *AdminHandler) ListSyncEvents(w http.ResponseWriter, r *http.Request) {
	q := r.URL.Query()
	limit, _ := strconv.Atoi(q.Get("limit"))
	events, err := h.events.List(r.Context(), domain.SyncEventFilter{
		Source: q.Get("source"), Action: q.Get("action"), ItemID: q.Get("item_id"), Limit: limit,
	})
	if err != nil {
		writeStoreError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, events)
}

// SyncSummary handles GET /admin/sync-summary?hours=24 — counts per source and action.
func (h *AdminHandler) SyncSummary(w http.ResponseWriter, r *http.Request) {
	hours, _ := strconv.Atoi(r.URL.Query().Get("hours"))
	if hours <= 0 {
		hours = 24
	}
	summary, err := h.events.Summary(r.Context(), time.Now().Add(-time.Duration(hours)*time.Hour))
	if err != nil {
		writeStoreError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, summary)
}
