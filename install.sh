#!/usr/bin/env bash
set -euo pipefail

# ═══════════════════════════════════════════
#  JMCL Server Manager - Remote Installer
#  Usage: sudo bash install.sh
# ═══════════════════════════════════════════

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'

log()  { echo -e "${GREEN}[✓]${NC} $1"; }
warn() { echo -e "${YELLOW}[!]${NC} $1"; }
err()  { echo -e "${RED}[✗]${NC} $1"; exit 1; }
step() { echo -e "\n${CYAN}═══ $1 ═══${NC}"; }

INSTALL_DIR="/opt/jmcl-servers"
DATA_DIR="/var/lib/jmcl-servers"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# ─── Step 1: Check OS ─────────────────────
step "Step 1/5: Checking System"
OS="$(uname -s)"
ARCH="$(uname -m)"
log "OS: $OS, Arch: $ARCH"

if [ "$OS" != "Linux" ]; then
    err "This installer only supports Linux. macOS use: docker compose up -d"
fi

# Check disk
DISK_FREE=$(df -BG /opt 2>/dev/null | tail -1 | awk '{print $4}' | tr -d 'G' || echo "0")
[ "$DISK_FREE" -lt 5 ] 2>/dev/null && warn "Low disk space: ${DISK_FREE}GB free (<5GB recommended)"

# ─── Step 2: Install Docker ───────────────
step "Step 2/5: Installing Docker"
if command -v docker &>/dev/null && docker --version &>/dev/null; then
    log "Docker already installed: $(docker --version)"
else
    warn "Docker not found. Installing..."
    curl -fsSL https://get.docker.com | sh || err "Docker install failed"
    systemctl start docker
    systemctl enable docker
    log "Docker installed successfully"
fi

# Docker Compose plugin
if ! docker compose version &>/dev/null; then
    warn "Installing docker-compose plugin..."
    DOCKER_CONFIG=${DOCKER_CONFIG:-/usr/local/lib/docker/cli-plugins}
    mkdir -p "$DOCKER_CONFIG"
    COMPOSE_URL="https://github.com/docker/compose/releases/latest/download/docker-compose-linux-${ARCH}"
    curl -SL "$COMPOSE_URL" -o "$DOCKER_CONFIG/docker-compose" 2>/dev/null || \
    curl -SL "https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-linux-${ARCH}" \
        -o "$DOCKER_CONFIG/docker-compose"
    chmod +x "$DOCKER_CONFIG/docker-compose"
    log "Docker Compose installed"
fi

# ─── Step 3: Setup JMCL ──────────────────
step "Step 3/5: Setting up JMCL Server Manager"

# Create directories
mkdir -p "$INSTALL_DIR" "$DATA_DIR/instances" "$DATA_DIR/servers"
log "Created: $INSTALL_DIR"
log "Created: $DATA_DIR"

# Copy project files from script location
PROJECT_ROOT="$SCRIPT_DIR"
if [ -f "$PROJECT_ROOT/docker-compose.yml" ]; then
    cp "$PROJECT_ROOT/docker-compose.yml" "$INSTALL_DIR/"
    [ -d "$PROJECT_ROOT/backend-core" ] && cp -r "$PROJECT_ROOT/backend-core" "$INSTALL_DIR/"
    [ -d "$PROJECT_ROOT/frontend" ] && cp -r "$PROJECT_ROOT/frontend" "$INSTALL_DIR/"
    log "Project files copied"
fi

# Generate docker-compose.yml if not copied
if [ ! -f "$INSTALL_DIR/docker-compose.yml" ]; then
    cat > "$INSTALL_DIR/docker-compose.yml" << 'DOCKEREOF'
version: '3.8'
services:
  backend-core:
    build:
      context: ./backend-core
      dockerfile: Dockerfile
    container_name: jmcl-core
    ports:
      - "252541:252541"
    volumes:
      - jmcl_data:/data
      - /var/run/docker.sock:/var/run/docker.sock
    environment:
      - SERVER_PORT=252541
      - JMCL_DATA_DIR=/data
      - JMCL_JAVA_HOME=/usr/lib/jvm/java-21-openjdk
    restart: unless-stopped
    networks:
      - jmcl_net

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    container_name: jmcl-frontend
    ports:
      - "252540:252540"
    environment:
      - API_BASE_URL=http://backend-core:252541
    depends_on:
      - backend-core
    restart: unless-stopped
    networks:
      - jmcl_net

volumes:
  jmcl_data:
    driver: local

networks:
  jmcl_net:
    driver: bridge
DOCKEREOF
    log "Generated docker-compose.yml"
fi

# ─── Step 4: Configure Auto-start ─────────
step "Step 4/5: Configuring Auto-start"

cat > /etc/systemd/system/jmcl-server-manager.service << SERVICEEOF
[Unit]
Description=JMCL Minecraft Server Manager
After=docker.service network-online.target
Requires=docker.service
Wants=network-online.target

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=$INSTALL_DIR
ExecStart=/usr/bin/docker compose -f $INSTALL_DIR/docker-compose.yml up -d
ExecStop=/usr/bin/docker compose -f $INSTALL_DIR/docker-compose.yml down
ExecReload=/usr/bin/docker compose -f $INSTALL_DIR/docker-compose.yml restart
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
SERVICEEOF

chmod 644 /etc/systemd/system/jmcl-server-manager.service
systemctl daemon-reload
systemctl enable jmcl-server-manager.service
log "systemd service installed and enabled"

# ─── Step 5: Build & Start ────────────────
step "Step 5/5: Building and Starting Containers"
cd "$INSTALL_DIR"
docker compose up -d --build 2>&1 || warn "Build may have encountered issues"

# Wait for services
sleep 5

# Verify
if docker ps | grep -q jmcl-core; then
    log "JMCL Core container is running"
else
    warn "Core container may not be running. Check: docker compose logs backend-core"
fi

if docker ps | grep -q jmcl-frontend; then
    log "JMCL Frontend container is running"
else
    warn "Frontend container may not be running. Check: docker compose logs frontend"
fi

echo ""
echo "═══════════════════════════════════════"
echo "  JMCL Server Manager Installed!"
echo "───────────────────────────────────────"
echo "  Frontend UI:  http://$(hostname -I 2>/dev/null | awk '{print $1}' || echo 'SERVER_IP'):252540"
echo "  Backend API:  http://$(hostname -I 2>/dev/null | awk '{print $1}' || echo 'SERVER_IP'):252541"
echo "───────────────────────────────────────"
echo "  Manage:"
echo "    systemctl start jmcl-server-manager"
echo "    systemctl stop jmcl-server-manager"
echo "    docker compose -f $INSTALL_DIR/docker-compose.yml logs -f"
echo "═══════════════════════════════════════"
