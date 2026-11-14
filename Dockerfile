# Images are pinned to a minor version: rebuild regularly to pick up patch releases.

# ─── Stage 1: Build web app ─────────────────────────────────────────────────
FROM node:24-alpine AS web-builder
WORKDIR /app
COPY web/package.json web/package-lock.json ./
# Reproducible install from the lockfile; no install scripts from dependencies.
RUN npm ci --ignore-scripts --no-audit --no-fund
COPY web/ ./
RUN npm run build

# ─── Stage 2: Build backend ───────────────────────────────────────────────────
FROM golang:1.27-alpine AS backend-builder
WORKDIR /app
# Download dependencies first for better layer caching
COPY backend/go.mod backend/go.sum ./
RUN go mod download && go mod verify
COPY backend/ ./
RUN CGO_ENABLED=0 GOOS=linux go build -trimpath -ldflags="-s -w" -o server ./cmd/server

# ─── Stage 3: Final minimal image ────────────────────────────────────────────
FROM alpine:3.24
RUN apk add --no-cache ca-certificates tzdata \
 && addgroup -S vault && adduser -S -G vault -H -s /sbin/nologin vault \
 && mkdir -p /app/uploads && chown vault:vault /app/uploads
WORKDIR /app

COPY --from=backend-builder /app/server .
COPY --from=web-builder /app/dist ./static

# Static files are read-only; uploaded covers go to a dedicated (volume) directory.
ENV STATIC_DIR=/app/static \
    UPLOAD_DIR=/app/uploads

# Never run the server as root.
USER vault

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s \
  CMD wget -qO- http://localhost:8080/healthz || exit 1

CMD ["./server"]
