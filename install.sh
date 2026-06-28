#!/bin/bash
set -e

echo "JMCL Server Manager Installer"

# Parse flags
MODE="pull"  # pull or build
[[ "${1:-}" == "--build" ]] && MODE="build"
[[ "${1:-}" == "--mirror" ]] && MODE="mirror"
echo "Mode: $MODE"

# Docker
echo ">>> Docker"
if ! command -v docker &>/dev/null; then
    apt-get update -qq 2>/dev/null || true
    apt-get install -y -qq docker.io docker-compose-plugin 2>/dev/null || true
    systemctl start docker 2>/dev/null || true
    systemctl enable docker 2>/dev/null || true
fi
echo "  $(docker --version 2>&1)"

# Registry mirror (for base images)
if [ "$MODE" = "mirror" ]; then
    mkdir -p /etc/docker
    echo '{"registry-mirrors":["https://docker.1ms.run","https://docker.xuanyuan.me"]}' > /etc/docker/daemon.json
    systemctl restart docker 2>/dev/null || true
    echo "  Mirror configured"
fi

# Setup
echo ">>> Setup"
mkdir -p /opt/jmcl-servers /var/lib/jmcl-servers

if [ "$MODE" = "build" ]; then
    # Build locally from source in current directory
    if [ ! -f docker-compose.yml ]; then
        echo "ERROR: Run from project root (where docker-compose.yml is)"
        exit 1
    fi
    echo "  Building images locally..."
    docker compose build 2>&1
    cp docker-compose.yml /opt/jmcl-servers/
    cp -r backend-core frontend /opt/jmcl-servers/ 2>/dev/null || true
    echo "  Starting..."
    docker compose up -d 2>&1
else
    # Pull from GHCR
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
    restart: unless-stopped
volumes: {jmcl_data: {}}
YML
    cd /opt/jmcl-servers
    docker compose pull 2>&1 | tail -5 || echo "  Pull issues, continuing..."
    docker compose up -d 2>&1
fi

# Systemd
cat > /etc/systemd/system/jmcl-server-manager.service << SVC
[Unit]
Description=JMCL Server Manager
After=docker.service
[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/opt/jmcl-servers
ExecStart=/usr/bin/docker compose -f /opt/jmcl-servers/docker-compose.yml up -d
ExecStop=/usr/bin/docker compose -f /opt/jmcl-servers/docker-compose.yml down
Restart=on-failure
[Install]
WantedBy=multi-user.target
SVC
systemctl daemon-reload 2>/dev/null || true
systemctl enable jmcl-server-manager 2>/dev/null || true

sleep 3
echo ""
IP=$(hostname -I | awk '{print $1}')
echo "Backend:  $(docker ps|grep jmcl-core >/dev/null 2>&1 && echo 'RUNNING' || echo 'NOT RUNNING')"
echo "Frontend: $(docker ps|grep jmcl-frontend >/dev/null 2>&1 && echo 'RUNNING' || echo 'NOT RUNNING')"
echo "UI:  http://${IP}:25540"
echo "API: http://${IP}:25541"
