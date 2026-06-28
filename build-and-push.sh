#!/usr/bin/env bash
# Build Docker images locally and push to GitHub Container Registry (GHCR)
# Usage: bash build-and-push.sh
set -euo pipefail

GREEN='\033[0;32m'; RED='\033[0;31m'; NC='\033[0m'
REGISTRY="ghcr.io/open-code-studio"

log() { echo -e "${GREEN}[✓]${NC} $1"; }
err() { echo -e "${RED}[✗]${NC} $1"; exit 1; }

# Check Docker login
if ! docker pull "$REGISTRY/jmcl-backend:latest" &>/dev/null; then
    echo "Please login to GHCR first:"
    echo "  1. Create PAT at: https://github.com/settings/tokens"
    echo "     Scopes: write:packages, read:packages, delete:packages"
    echo "  2. Run: echo 'YOUR_PAT' | docker login ghcr.io -u Open-code-Studio --password-stdin"
    echo ""
    err "Not logged in to GHCR"
fi
log "GHCR login verified"

# Build backend
log "Building backend image..."
docker build -t "$REGISTRY/jmcl-backend:latest" ./backend-core

# Build frontend
log "Building frontend image..."
docker build -t "$REGISTRY/jmcl-frontend:latest" ./frontend

# Push
log "Pushing backend..."
docker push "$REGISTRY/jmcl-backend:latest"

log "Pushing frontend..."
docker push "$REGISTRY/jmcl-frontend:latest"

echo ""
log "Done! Images available at:"
log "  $REGISTRY/jmcl-backend:latest"
log "  $REGISTRY/jmcl-frontend:latest"
echo ""

# Make public (optional)
echo "To make images public, visit:"
echo "  https://github.com/Open-code-Studio/JMCL-for-servers/pkgs/container/jmcl-backend/settings"
echo "  https://github.com/Open-code-Studio/JMCL-for-servers/pkgs/container/jmcl-frontend/settings"
