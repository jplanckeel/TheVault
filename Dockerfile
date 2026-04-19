# ─── Stage 1: Build frontend ─────────────────────────────────────────────────
FROM node:alpine AS frontend-builder
WORKDIR /app
COPY frontend/package*.json ./
RUN npm install --prefer-offline
COPY frontend/ ./
RUN npm run build

# ─── Stage 2: Build backend ───────────────────────────────────────────────────
FROM golang:alpine AS backend-builder
WORKDIR /app
# Download dependencies first for better layer caching
COPY backend/go.mod backend/go.sum ./
RUN go mod download
COPY backend/ ./
RUN CGO_ENABLED=0 GOOS=linux go build -ldflags="-s -w" -o server ./cmd/server

# ─── Stage 3: Final minimal image ────────────────────────────────────────────
FROM alpine:latest
RUN apk add --no-cache ca-certificates tzdata
WORKDIR /app

COPY --from=backend-builder /app/server .
COPY --from=frontend-builder /app/dist ./static

# Static dir picked up by STATIC_DIR env var
ENV STATIC_DIR=/app/static

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s \
  CMD wget -qO- http://localhost:8080/healthz || exit 1

CMD ["./server"]
