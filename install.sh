#!/usr/bin/env bash
set -euo pipefail

# ═══════════════════════════════════════════
#  JMCL Server Manager - Remote Installer
#  Usage:
#    curl ... | sudo bash
#    curl ... | sudo bash -s -- --mirror
# ═══════════════════════════════════════════

RC='\033[0m'; RD='\033[0;31m'; GN='\033[0;32m'; YW='\033[1;33m'; CY='\033[0;36m'; MG='\033[0;35m'

# Flags
USE_MIRROR=false
for arg in "$@"; do
    case $arg in
        --mirror|--china|-m) USE_MIRROR=true ;;
    esac
done

log()   { echo -e "${GN}[OK]${RC} $1"; }
warn()  { echo -e "${YW}[WARN]${RC} $1"; }
err()   { echo -e "${RD}[FAIL]${RC} $1"; exit 1; }
info()  { echo -e "${CY}[>>]${RC} $1"; }
step()  { echo -e "\n${CY}--- Step $1/$2: $3 ---${RC}\n"; }

# Config
INSTALL_DIR="/opt/jmcl-servers"
DATA_DIR="/var/lib/jmcl-servers"
TOTAL_STEPS=5
OS="$(uname -s)"
ARCH="$(uname -m)"
SERVER_IP=$(hostname -I 2>/dev/null | awk '{print $1}' || echo "YOUR_IP")

# Registry mirrors for base images (Docker Hub)
if $USE_MIRROR; then
    DOCKER_MIRRORS='"https://docker.1ms.run","https://docker.xuanyuan.me"'
else
    DOCKER_MIRRORS=''
fi

echo ""
echo "  JMCL Server Manager Installer v1.0.0"
$USE_MIRROR && echo "  => China Mirror Mode"
echo ""
echo "  System:   $OS / $ARCH"
echo "  Install:  $INSTALL_DIR"
echo "  Data:     $DATA_DIR"
echo ""

# ─── Step 1: Prerequisites ────────────────
step 1 $TOTAL_STEPS "Checking prerequisites"

if [ "$OS" != "Linux" ]; then
    err "Only Linux supported"
fi

log "OS: $OS ($ARCH)"

DISK_FREE=$(df -BG /opt 2>/dev/null | tail -1 | awk '{print $4}' | tr -d 'G' || echo "0")
if [ "$DISK_FREE" -lt 5 ] 2>/dev/null; then
    warn "Low disk: ${DISK_FREE}GB free (need 5GB+)"
else
    log "Disk: ${DISK_FREE}GB free"
fi

log "Memory: $(free -m 2>/dev/null | awk '/Mem:/{print $2}' || echo 'N/A')MB"

# ─── Step 2: Docker ────────────────────────
step 2 $TOTAL_STEPS "Installing Docker"

if command -v docker &>/dev/null; then
    log "Docker: $(docker --version 2>&1)"
else
    info "Downloading Docker installer from get.docker.com..."
    info "(This may take 2-5 minutes, please wait...)"
    if curl -fsSL --connect-timeout 60 --retry 3 https://get.docker.com -o /tmp/get-docker.sh 2>/dev/null; then
        info "Running Docker installer..."
        sh /tmp/get-docker.sh > /tmp/docker-install.log 2>&1 || warn "Docker install may have issues, continuing anyway"
        rm -f /tmp/get-docker.sh
        systemctl start docker 2>/dev/null || true
        systemctl enable docker 2>/dev/null || true
        log "Docker installed"
    else
        warn "Cannot reach get.docker.com, trying apt..."
        if command -v apt-get &>/dev/null; then
            apt-get update -qq 2>/dev/null
            apt-get install -y -qq docker.io 2>/dev/null && log "Docker installed via apt" || err "Docker install failed"
        else
            err "Cannot install Docker. Please install manually."
        fi
    fi
fi

# Docker registry mirrors (speed up base image pulls)
if [ -n "$DOCKER_MIRRORS" ] && [ -f /etc/docker/daemon.json ]; then
    if ! grep -q "registry-mirrors" /etc/docker/daemon.json 2>/dev/null; then
        mkdir -p /etc/docker
        echo "{\"registry-mirrors\":[${DOCKER_MIRRORS}],\"log-driver\":\"json-file\",\"log-opts\":{\"max-size\":\"10m\",\"max-file\":\"3\"}}" > /etc/docker/daemon.json
        systemctl restart docker 2>/dev/null || true
        log "Docker mirror configured"
    fi
fi

# Docker Compose
if docker compose version &>/dev/null; then
    log "Docker Compose: $(docker compose version 2>&1)"
elif command -v docker-compose &>/dev/null; then
    log "Docker Compose: $(docker-compose --version 2>&1)"
else
    info "Installing Docker Compose..."
    COMPOSE_OK=false

    # Try apt first
    if command -v apt-get &>/dev/null; then
        apt-get update -qq 2>/dev/null
        apt-get install -y -qq docker-compose-plugin 2>/dev/null && COMPOSE_OK=true && log "Docker Compose installed via apt"
    fi

    # Fallback: download
    if [ "$COMPOSE_OK" != "true" ]; then
        DCONF=${DOCKER_CONFIG:-/usr/local/lib/docker/cli-plugins}
        mkdir -p "$DCONF"
        COMPOSE_URL="https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-linux-${ARCH}"
        info "Downloading: docker-compose-linux-${ARCH}..."
        if curl -fSL --connect-timeout 60 --retry 3 "$COMPOSE_URL" -o "$DCONF/docker-compose" 2>/dev/null; then
            chmod +x "$DCONF/docker-compose"
            log "Docker Compose installed (v2.24.0)"
        else
            warn "Could not install Docker Compose. Check Docker version."
        fi
    fi
fi

# Add user to docker group
if [ -n "${SUDO_USER:-}" ]; then
    usermod -aG docker "$SUDO_USER" 2>/dev/null || true
fi

# ─── Step 3: Setup ────────────────────────
step 3 $TOTAL_STEPS "Setting up directories"

mkdir -p "$INSTALL_DIR" "$DATA_DIR/instances" "$DATA_DIR/servers"
log "Created: $INSTALL_DIR"
log "Created: $DATA_DIR"

cat > "$INSTALL_DIR/docker-compose.yml" << 'DOCKEREOF'
version: '3.8'
services:
  backend-core:
    image: ghcr.io/open-code-studio/jmcl-backend:latest
    container_name: jmcl-core
    ports:
      - "25541:25541"
    volumes:
      - jmcl_data:/data
      - /var/run/docker.sock:/var/run/docker.sock
    environment:
      - SERVER_PORT=25541
      - JMCL_DATA_DIR=/data
      - JMCL_JAVA_HOME=/usr/lib/jvm/java-21-openjdk
    restart: unless-stopped
    networks:
      - jmcl_net

  frontend:
    image: ghcr.io/open-code-studio/jmcl-frontend:latest
    container_name: jmcl-frontend
    ports:
      - "25540:25540"
    environment:
      - API_BASE_URL=http://backend-core:25541
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
log "docker-compose.yml ready"

# ─── Step 4: Auto-start ───────────────────
step 4 $TOTAL_STEPS "Configuring auto-start"

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
systemctl daemon-reload 2>/dev/null || true
systemctl enable jmcl-server-manager.service 2>/dev/null || true
log "systemd service enabled"

# ─── Step 5: Pull & Start ─────────────────
step 5 $TOTAL_STEPS "Pulling images and starting"

cd "$INSTALL_DIR"

info "Pulling backend-core (this may take a while)..."
if docker compose pull backend-core 2>&1; then
    log "Backend image pulled"
else
    warn "Backend pull may have issues"
fi

info "Pulling frontend..."
if docker compose pull frontend 2>&1; then
    log "Frontend image pulled"
else
    warn "Frontend pull may have issues"
fi

info "Starting containers..."
docker compose up -d 2>&1 || warn "Container start may have issues"
sleep 3

# Verify
B_OK=false; F_OK=false
docker ps 2>/dev/null | grep -q jmcl-core && B_OK=true
docker ps 2>/dev/null | grep -q jmcl-frontend && F_OK=true

echo ""
echo "  ========================================="
echo "  Backend:  $($B_OK && echo -e "${GN}Running${RC}" || echo -e "${RD}Failed${RC}")"
echo "  Frontend: $($F_OK && echo -e "${GN}Running${RC}" || echo -e "${RD}Failed${RC}")"
echo "  -----------------------------------------"
echo "  UI:   http://${SERVER_IP}:25540"
echo "  API:  http://${SERVER_IP}:25541"
echo "  ========================================="

if $B_OK && $F_OK; then
    echo ""
    echo "  JMCL Server Manager is ready!"
else
    echo ""
    warn "Check logs: docker compose logs"
fi

echo ""
echo "  Commands:"
echo "    systemctl start/stop jmcl-server-manager"
echo "    docker compose -f $INSTALL_DIR/docker-compose.yml logs -f"
echo ""
