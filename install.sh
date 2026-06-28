#!/usr/bin/env bash
set -euo pipefail
exec 2>/tmp/jmcl-install.log

# ═══════════════════════════════════════════
#  JMCL Server Manager - Remote Installer
#  Usage:
#    curl ... | sudo bash
#    curl ... | sudo bash -s -- --mirror   # China mirror
# ═══════════════════════════════════════════

RC='\033[0m'; RD='\033[0;31m'; GN='\033[0;32m'; YW='\033[1;33m'; CY='\033[0;36m'; BL='\033[0;34m'; MG='\033[0;35m'

# Flags
USE_MIRROR=false
for arg in "$@"; do
    case $arg in
        --mirror|--china|-m) USE_MIRROR=true ;;
    esac
done

# UI Helpers
clear_screen() { printf "\033[2J\033[H"; }
cursor_hide()  { printf "\033[?25l"; }
cursor_show()  { printf "\033[?25h"; }
draw_line()    { printf "%${1:-60}s\n" | tr ' ' "${2:-━}"; }
log()   { echo -e "${GN} ✓${RC} $1"; }
warn()  { echo -e "${YW} ⚠${RC} $1"; }
err()   { echo -e "${RD} ✗${RC} $1"; exit 1; }
info()  { echo -e "${CY} ▶${RC} $1"; }

spinner() {
    local pid=$1 msg="${2:-Working...}"
    local spin=('⠋' '⠙' '⠹' '⠸' '⠼' '⠴' '⠦' '⠧' '⠇' '⠏')
    local i=0
    printf "${CY} %s${RC} %s" "${spin[0]}" "$msg"
    while kill -0 "$pid" 2>/dev/null; do
        printf "\r${CY} %s${RC} %s" "${spin[$i]}" "$msg"
        i=$(( (i + 1) % ${#spin[@]} ))
        sleep 0.1
    done
    wait "$pid" 2>/dev/null
    local ec=$?
    printf "\r\033[K"
    return $ec
}

progress_bar() {
    local cur=$1 tot=$2 label="${3:-}"
    local pct=$(( cur * 100 / tot ))
    local w=30 f=$(( pct * w / 100 ))
    printf "\r  ${BL}[${RC}"
    printf "%${f}s" | tr ' ' '█'
    printf "%$((w-f))s" | tr ' ' '░'
    printf "${BL}]${RC} %3d%%  %s" "$pct" "$label"
}

# Config
INSTALL_DIR="/opt/jmcl-servers"
DATA_DIR="/var/lib/jmcl-servers"
TOTAL_STEPS=5; CURRENT_STEP=0
OS="$(uname -s)"; ARCH="$(uname -m)"
SERVER_IP=$(hostname -I 2>/dev/null | awk '{print $1}' || echo "YOUR_IP")
trap cursor_show EXIT

# Mirror URLs
if $USE_MIRROR; then
    DOCKER_REGISTRY_MIRRORS='
    "https://docker.1ms.run",
    "https://docker.xuanyuan.me"
'
else
    DOCKER_REGISTRY_MIRRORS=''
fi

# ═══════════════════════════════════════════
clear_screen
echo ""
echo -e "${MG}  ╔═══════════════════════════════════╗${RC}"
echo -e "${MG}  ║   JMCL Server Manager Installer   ║${RC}"
echo -e "${MG}  ║        ${CY}v1.0.0${MG}                     ║${RC}"
echo -e "${MG}  ╚═══════════════════════════════════╝${RC}"
if $USE_MIRROR; then
    echo -e "                ${YW}🚀 China Mirror Mode${RC}"
fi
echo ""

printf "  %-20s %s\n" "System:"   "${OS} / ${ARCH}"
printf "  %-20s %s\n" "Install:"  "${INSTALL_DIR}"
printf "  %-20s %s\n\n" "Data:"   "${DATA_DIR}"
draw_line 50 '─'

# ─── Step 1: Prerequisites ────────────────
((CURRENT_STEP++))
echo ""
progress_bar "$CURRENT_STEP" "$TOTAL_STEPS" "Checking prerequisites..."
echo ""; echo ""
[ "$OS" != "Linux" ] && err "Only Linux supported."

log "OS: $OS ($ARCH)"
DISK_FREE=$(df -BG /opt 2>/dev/null | tail -1 | awk '{print $4}' | tr -d 'G' || echo "0")
[ "$DISK_FREE" -lt 5 ] 2>/dev/null && warn "Low disk: ${DISK_FREE}GB (need 5GB+)" || log "Disk: ${DISK_FREE}GB free"
log "Memory: $(free -m 2>/dev/null | awk '/Mem:/{print $2}' || echo 'N/A')MB total"
draw_line 50 '─'

# ─── Step 2: Docker ────────────────────────
((CURRENT_STEP++))
echo ""
progress_bar "$CURRENT_STEP" "$TOTAL_STEPS" "Installing Docker..."
echo ""; echo ""

if command -v docker &>/dev/null; then
    log "Docker: $(docker --version 2>&1 | head -1)"
else
    info "Installing Docker..."
    curl -fsSL https://get.docker.com -o /tmp/get-docker.sh 2>/dev/null &
    spinner $! "Downloading installer..."
    sh /tmp/get-docker.sh > /tmp/docker-install.log 2>&1 &
    spinner $! "Running installer..."
    systemctl start docker &>/dev/null
    systemctl enable docker &>/dev/null
    rm -f /tmp/get-docker.sh
    log "Docker installed"
fi

# Docker registry mirrors (speed up pulls in China)
if [ -n "$DOCKER_REGISTRY_MIRRORS" ]; then
    mkdir -p /etc/docker
    if ! grep -q "registry-mirrors" /etc/docker/daemon.json 2>/dev/null; then
        cat > /etc/docker/daemon.json << MIRROREOF
{
  "registry-mirrors": [${DOCKER_REGISTRY_MIRRORS}],
  "log-driver": "json-file",
  "log-opts": { "max-size": "10m", "max-file": "3" }
}
MIRROREOF
        systemctl restart docker &>/dev/null
        log "Docker mirror configured"
    fi
fi

# Docker Compose (Docker Engine v2+ already has it built-in)
if docker compose version &>/dev/null; then
    log "Docker Compose: $(docker compose version 2>&1)"
elif command -v docker-compose &>/dev/null; then
    log "Docker Compose: $(docker-compose --version 2>&1)"
else
    info "Installing Docker Compose..."
    DCONF=${DOCKER_CONFIG:-/usr/local/lib/docker/cli-plugins}
    mkdir -p "$DCONF"
    # Try plugin install via apt first (fastest)
    if command -v apt-get &>/dev/null; then
        apt-get update -qq 2>/dev/null
        apt-get install -y -qq docker-compose-plugin 2>/dev/null && \
            log "Docker Compose installed via apt" && COMPOSE_INSTALLED=true
    fi
    # Fallback: download binary if apt didn't work
    if [ "${COMPOSE_INSTALLED:-false}" != "true" ]; then
        COMPOSE_URL="https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-linux-${ARCH}"
        curl -fSL --connect-timeout 30 --retry 3 "$COMPOSE_URL" -o "$DCONF/docker-compose" 2>/tmp/compose-dl.log &
        spinner $! "Downloading Docker Compose..."
        chmod +x "$DCONF/docker-compose"
        log "Docker Compose installed"
    fi
fi

# Add current user to docker group
if [ -n "${SUDO_USER:-}" ] && ! groups "$SUDO_USER" 2>/dev/null | grep -q docker; then
    usermod -aG docker "$SUDO_USER" 2>/dev/null && log "User $SUDO_USER added to docker group"
fi

draw_line 50 '─'

# ─── Step 3: Setup ────────────────────────
((CURRENT_STEP++))
echo ""
progress_bar "$CURRENT_STEP" "$TOTAL_STEPS" "Setting up directories..."
echo ""; echo ""

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
draw_line 50 '─'

# ─── Step 4: Auto-start ───────────────────
((CURRENT_STEP++))
echo ""
progress_bar "$CURRENT_STEP" "$TOTAL_STEPS" "Configuring auto-start..."
echo ""; echo ""

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
systemctl daemon-reload &>/dev/null
systemctl enable jmcl-server-manager.service &>/dev/null
log "systemd service enabled"
draw_line 50 '─'

# ─── Step 5: Pull & Start ─────────────────
((CURRENT_STEP++))
echo ""
progress_bar "$CURRENT_STEP" "$TOTAL_STEPS" "Pulling images & starting..."
echo ""; echo ""

cd "$INSTALL_DIR"

info "Pulling backend-core..."
docker compose pull backend-core > /tmp/jmcl-pull.log 2>&1 &
spinner $! "Pulling backend-core..."
log "Backend image pulled"

info "Pulling frontend..."
docker compose pull frontend > /tmp/jmcl-pull2.log 2>&1 &
spinner $! "Pulling frontend..."
log "Frontend image pulled"

info "Starting containers..."
docker compose up -d > /tmp/jmcl-up.log 2>&1 &
spinner $! "Launching..."
sleep 3

# Verify
B_OK=false; F_OK=false
docker ps 2>/dev/null | grep -q jmcl-core && B_OK=true
docker ps 2>/dev/null | grep -q jmcl-frontend && F_OK=true

echo ""
echo -e "  ${MG}┌─────────────────────────────────────┐${RC}"
echo -e "  ${MG}│${RC}  Backend:  $($B_OK && echo "${GN}Running ✓${RC}" || echo "${RD}Failed ✗${RC}")  ${MG}                  │${RC}"
echo -e "  ${MG}│${RC}  Frontend: $($F_OK && echo "${GN}Running ✓${RC}" || echo "${RD}Failed ✗${RC}")  ${MG}                  │${RC}"
echo -e "  ${MG}├─────────────────────────────────────┤${RC}"
echo -e "  ${MG}│${RC}  UI:   ${CY}http://${SERVER_IP}:25540${RC}     ${MG}│${RC}"
echo -e "  ${MG}│${RC}  API:  ${CY}http://${SERVER_IP}:25541${RC}     ${MG}│${RC}"
echo -e "  ${MG}└─────────────────────────────────────┘${RC}"
$B_OK && $F_OK && echo -e "\n  ${GN}⭐ JMCL Server Manager ready!${RC}" || { echo ""; warn "Check logs: docker compose logs"; }
echo ""; draw_line 50 '─'; echo ""
