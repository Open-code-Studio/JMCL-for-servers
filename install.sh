#!/bin/bash
set -e

echo "========================================"
echo "  JMCL Server Manager Installer"
echo "========================================"
echo ""

# Mirror mode
MIRROR=false
[[ "${1:-}" == "--mirror" ]] && MIRROR=true
$MIRROR && echo "[Mirror Mode]"

# ─── Docker ───────────────────────────
echo ">>> Step 1/3: Docker"
if ! command -v docker &>/dev/null; then
    echo "Installing Docker (apt)..."
    apt-get update -qq 2>/dev/null || true
    apt-get install -y -qq docker.io 2>/dev/null && echo "  Docker installed via apt" || {
        echo "  Apt failed, trying get.docker.com..."
        curl -fsSL --connect-timeout 30 --retry 2 https://get.docker.com | sh 2>/dev/null || {
            echo "ERROR: Cannot install Docker. Run: apt install docker.io"
            exit 1
        }
    }
    systemctl start docker 2>/dev/null || true
    systemctl enable docker 2>/dev/null || true
else
    echo "  Docker: $(docker --version 2>&1)"
fi

# Compose
if ! docker compose version &>/dev/null; then
    echo "Installing docker-compose-plugin..."
    apt-get install -y -qq docker-compose-plugin 2>/dev/null && \
        echo "  Compose plugin installed" || {
        echo "  WARN: compose plugin install failed, continuing..."
    }
fi

# Registry mirror (China)
if $MIRROR; then
    mkdir -p /etc/docker
    if [ ! -f /etc/docker/daemon.json ] || ! grep -q mirror /etc/docker/daemon.json 2>/dev/null; then
        echo '{ "registry-mirrors": ["https://docker.1ms.run","https://docker.xuanyuan.me"], "log-driver": "json-file", "log-opts": {"max-size":"10m","max-file":"3"} }' > /etc/docker/daemon.json
        systemctl restart docker 2>/dev/null || true
        echo "  Registry mirror configured"
    fi
fi

# ─── Setup ────────────────────────────
echo ""
echo ">>> Step 2/3: Setup"
mkdir -p /opt/jmcl-servers /var/lib/jmcl-servers/instances /var/lib/jmcl-servers/servers
echo "  Directories created"

# Write docker-compose.yml
cat > /opt/jmcl-servers/docker-compose.yml << 'YML'
version: '3.8'
services:
  backend-core:
    image: ghcr.io/open-code-studio/jmcl-backend:latest
    container_name: jmcl-core
    ports: ["25541:25541"]
    volumes: ["jmcl_data:/data","/var/run/docker.sock:/var/run/docker.sock"]
    environment: ["SERVER_PORT=25541","JMCL_DATA_DIR=/data"]
    restart: unless-stopped
  frontend:
    image: ghcr.io/open-code-studio/jmcl-frontend:latest
    container_name: jmcl-frontend
    ports: ["25540:25540"]
    environment: ["API_BASE_URL=http://backend-core:25541"]
    depends_on: ["backend-core"]
    restart: unless-stopped
volumes:
  jmcl_data: {}
YML
echo "  docker-compose.yml written"

# ─── Pull & Start ──────────────────────
echo ""
echo ">>> Step 3/3: Pull & Start"
cd /opt/jmcl-servers

echo "  Pulling backend-core..."
docker compose pull backend-core 2>&1 | head -5 || echo "  WARN: pull may have issues"

echo "  Pulling frontend..."
docker compose pull frontend 2>&1 | head -5 || echo "  WARN: pull may have issues"

echo "  Starting containers..."
docker compose up -d 2>&1 || echo "  WARN: start may have issues"

sleep 3

# Systemd auto-start
cat > /etc/systemd/system/jmcl-server-manager.service << SVC
[Unit]
Description=JMCL Server Manager
After=docker.service network-online.target
Requires=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/opt/jmcl-servers
ExecStart=/usr/bin/docker compose -f /opt/jmcl-servers/docker-compose.yml up -d
ExecStop=/usr/bin/docker compose -f /opt/jmcl-servers/docker-compose.yml down
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
SVC
systemctl daemon-reload 2>/dev/null || true
systemctl enable jmcl-server-manager 2>/dev/null || true
echo "  Auto-start enabled"

# ─── Result ────────────────────────────
echo ""
echo "========================================"
if docker ps 2>/dev/null | grep -q jmcl-core; then
    echo "  Backend:  RUNNING"
else
    echo "  Backend:  NOT RUNNING"
fi
if docker ps 2>/dev/null | grep -q jmcl-frontend; then
    echo "  Frontend: RUNNING"
else
    echo "  Frontend: NOT RUNNING"
fi
echo "----------------------------------------"
IP=$(hostname -I 2>/dev/null | awk '{print $1}' || echo "YOUR_IP")
echo "  UI:  http://${IP}:25540"
echo "  API: http://${IP}:25541"
echo "========================================"
echo "  Logs: docker compose -f /opt/jmcl-servers/docker-compose.yml logs -f"
echo ""
