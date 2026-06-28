#!/usr/bin/env bash
set -euo pipefail
exec 2>/tmp/jmcl-install.log

# ═══════════════════════════════════════════
#  JMCL Server Manager - Remote Installer
#  Usage: curl ... | sudo bash
# ═══════════════════════════════════════════

RC='\033[0m'; RD='\033[0;31m'; GN='\033[0;32m'; YW='\033[1;33m'; CY='\033[0;36m'; BL='\033[0;34m'; MG='\033[0;35m'

# UI Helpers
clear_screen() { printf "\033[2J\033[H"; }
cursor_hide()  { printf "\033[?25l"; }
cursor_show()  { printf "\033[?25h"; }
move_to()      { printf "\033[%d;%dH" "${1:-1}" "${2:-1}"; }
draw_line()    { printf "%${1:-60}s\n" | tr ' ' "${2:-━}"; }

log()   { echo -e "${GN} ✓${RC} $1"; }
warn()  { echo -e "${YW} ⚠${RC} $1"; }
err()   { echo -e "${RD} ✗${RC} $1"; exit 1; }
info()  { echo -e "${CY} ▶${RC} $1"; }
ok()    { echo -e "${GN} ✓${RC} $1"; }

# Spinner for background tasks
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
    local exit_code=$?
    printf "\r\033[K"
    return $exit_code
}

# Progress bar
progress_bar() {
    local current=$1 total=$2 label="${3:-}"
    local pct=$(( current * 100 / total ))
    local bar_width=30
    local filled=$(( pct * bar_width / 100 ))
    local empty=$(( bar_width - filled ))
    printf "\r  ${BL}[${RC}"
    printf "%${filled}s" | tr ' ' '█'
    printf "%${empty}s" | tr ' ' '░'
    printf "${BL}]${RC} %3d%%  %s" "$pct" "$label"
}

# Install variables
INSTALL_DIR="/opt/jmcl-servers"
DATA_DIR="/var/lib/jmcl-servers"
TOTAL_STEPS=5
CURRENT_STEP=0
OS="$(uname -s)"
ARCH="$(uname -m)"
SERVER_IP=$(hostname -I 2>/dev/null | awk '{print $1}' || echo "YOUR_IP")

trap cursor_show EXIT

# ═══════════════════════════════════════════
clear_screen
echo ""
echo -e "${MG}  ╔═══════════════════════════════════╗${RC}"
echo -e "${MG}  ║   JMCL Server Manager Installer   ║${RC}"
echo -e "${MG}  ║        ${CY}v1.0.0${MG}                     ║${RC}"
echo -e "${MG}  ╚═══════════════════════════════════╝${RC}"
echo ""

# Progress header
printf "  %-20s %s\n" "System:"   "${OS} / ${ARCH}"
printf "  %-20s %s\n" "Install:"  "${INSTALL_DIR}"
printf "  %-20s %s\n\n" "Data:"   "${DATA_DIR}"

draw_line 50 '─'

# ─── Step 1 ─────────────────────────────────
((CURRENT_STEP++))
echo ""
progress_bar "$CURRENT_STEP" "$TOTAL_STEPS" "Checking prerequisites..."
echo ""
echo ""

if [ "$OS" != "Linux" ]; then
    err "Only Linux is supported. For macOS, use: docker compose up -d"
fi

log "OS: $OS ($ARCH)"

DISK_FREE=$(df -BG /opt 2>/dev/null | tail -1 | awk '{print $4}' | tr -d 'G' || echo "0")
if [ "$DISK_FREE" -lt 5 ] 2>/dev/null; then
    warn "Low disk: ${DISK_FREE}GB free (need 5GB+)"
else
    log "Disk: ${DISK_FREE}GB free"
fi

MEM_TOTAL=$(free -m 2>/dev/null | awk '/Mem:/{print $2}' || echo "N/A")
log "Memory: ${MEM_TOTAL}MB total"

draw_line 50 '─'

# ─── Step 2 ─────────────────────────────────
((CURRENT_STEP++))
echo ""
progress_bar "$CURRENT_STEP" "$TOTAL_STEPS" "Installing Docker..."
echo ""
echo ""

if command -v docker &>/dev/null && docker --version &>/dev/null; then
    log "Docker: $(docker --version 2>&1 | head -1)"
else
    info "Installing Docker (this may take a minute)..."
    curl -fsSL https://get.docker.com -o /tmp/get-docker.sh 2>/dev/null &
    spinner $! "Downloading Docker installer..."
    sh /tmp/get-docker.sh > /tmp/docker-install.log 2>&1 &
    spinner $! "Running Docker installer..."
    systemctl start docker &>/dev/null
    systemctl enable docker &>/dev/null
    rm -f /tmp/get-docker.sh
    log "Docker installed"
fi

if ! docker compose version &>/dev/null; then
    info "Installing Docker Compose plugin..."
    DOCKER_CONFIG=${DOCKER_CONFIG:-/usr/local/lib/docker/cli-plugins}
    mkdir -p "$DOCKER_CONFIG"
    COMPOSE_URL="https://github.com/docker/compose/releases/latest/download/docker-compose-linux-${ARCH}"
    curl -SL "$COMPOSE_URL" -o "$DOCKER_CONFIG/docker-compose" 2>/tmp/compose-dl.log &
    spinner $! "Downloading Docker Compose..."
    chmod +x "$DOCKER_CONFIG/docker-compose"
    log "Docker Compose installed"
else
    log "Docker Compose: available"
fi

draw_line 50 '─'

# ─── Step 3 ─────────────────────────────────
((CURRENT_STEP++))
echo ""
progress_bar "$CURRENT_STEP" "$TOTAL_STEPS" "Setting up directories..."
echo ""
echo ""

mkdir -p "$INSTALL_DIR" "$DATA_DIR/instances" "$DATA_DIR/servers"
log "Created: $INSTALL_DIR"
log "Created: $DATA_DIR"

# Generate docker-compose.yml
if [ ! -f "$INSTALL_DIR/docker-compose.yml" ]; then
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
fi
log "docker-compose.yml ready"

draw_line 50 '─'

# ─── Step 4 ─────────────────────────────────
((CURRENT_STEP++))
echo ""
progress_bar "$CURRENT_STEP" "$TOTAL_STEPS" "Configuring auto-start..."
echo ""
echo ""

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
log "systemd service enabled (auto-start on boot)"

draw_line 50 '─'

# ─── Step 5 ─────────────────────────────────
((CURRENT_STEP++))
echo ""
progress_bar "$CURRENT_STEP" "$TOTAL_STEPS" "Pulling images & starting..."
echo ""
echo ""

cd "$INSTALL_DIR"

info "Pulling backend image..."
docker compose pull backend-core > /tmp/jmcl-pull.log 2>&1 &
spinner $! "Pulling backend-core..."
log "Backend image pulled"

info "Pulling frontend image..."
docker compose pull frontend > /tmp/jmcl-pull2.log 2>&1 &
spinner $! "Pulling frontend..."
log "Frontend image pulled"

info "Starting containers..."
docker compose up -d > /tmp/jmcl-up.log 2>&1 &
spinner $! "Launching JMCL services..."
sleep 3

# Verify
BACKEND_OK=false
FRONTEND_OK=false
docker ps 2>/dev/null | grep -q jmcl-core && BACKEND_OK=true
docker ps 2>/dev/null | grep -q jmcl-frontend && FRONTEND_OK=true

echo ""
echo -e "  ${MG}┌─────────────────────────────────────┐${RC}"
echo -e "  ${MG}│${RC}  Backend:  $($BACKEND_OK && echo "${GN}Running ✓${RC}" || echo "${RD}Failed ✗${RC}")  ${MG}                  │${RC}"
echo -e "  ${MG}│${RC}  Frontend: $($FRONTEND_OK && echo "${GN}Running ✓${RC}" || echo "${RD}Failed ✗${RC}")  ${MG}                  │${RC}"
echo -e "  ${MG}├─────────────────────────────────────┤${RC}"
echo -e "  ${MG}│${RC}  UI:   ${CY}http://${SERVER_IP}:25540${RC}     ${MG}│${RC}"
echo -e "  ${MG}│${RC}  API:  ${CY}http://${SERVER_IP}:25541${RC}     ${MG}│${RC}"
echo -e "  ${MG}└─────────────────────────────────────┘${RC}"

if $BACKEND_OK && $FRONTEND_OK; then
    echo ""
    echo -e "  ${GN}⭐ JMCL Server Manager ready!${RC}"
else
    echo ""
    warn "Some services may need attention. Check logs:"
    echo "    docker compose -f $INSTALL_DIR/docker-compose.yml logs"
fi

echo ""
echo -e "  ${BL}Commands:${RC}"
echo "    systemctl start/stop jmcl-server-manager"
echo "    docker compose -f $INSTALL_DIR/docker-compose.yml logs -f"
echo ""
draw_line 50 '─'
echo ""
