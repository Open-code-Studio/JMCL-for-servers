# JMCL for Servers

Minecraft Java Edition 服务器启动管理器，使用 Docker 容器化部署，通过 Web UI 管理多个 MC 服务器。

## 架构

```
┌──────────────────────────────────────────────┐
│            GitHub Actions CI/CD              │
│      自动构建 → ghcr.io 镜像仓库              │
├──────────────────────────────────────────────┤
│              Docker Compose                   │
│  ┌──────────────────┐  ┌──────────────────┐  │
│  │  Frontend (TS)   │  │  Backend (Java)  │  │
│  │  React + MD3 UI  │  │  Spring Boot     │  │
│  │  Port: 25540     │◄─┤  Port: 25541     │  │
│  └──────────────────┘  └──────────────────┘  │
│             ▲                     ▲           │
│        ghcr.io 预编译镜像          │           │
│           │                      │           │
│           └──────────┬───────────┘           │
│                      ▼                       │
│              MC Server Instances              │
│        (Vanilla/Paper/Fabric/Forge...)       │
└──────────────────────────────────────────────┘
```

## 功能

- **多服务端类型支持**: Vanilla, Paper, Purpur, Folia, Fabric, Quilt, Forge, NeoForge, Velocity
- **服务器管理**: 启动/停止/重启 MC 服务器，发送控制台命令
- **参数配置**: 可视化修改 server.properties、内存分配、端口等
- **实时日志**: WebSocket 风格的实时控制台输出
- **Material Design 3**: 精美的现代化 UI，支持明暗主题
- **开机自启**: systemd (Linux) / launchd (macOS) 自动服务配置
- **Docker 部署**: 前后端独立容器，数据卷持久化

## 快速开始

### 远程服务器部署（推荐）

```bash
# 一键安装
git clone https://github.com/Open-code-Studio/JMCL-for-servers.git
cd JMCL-for-servers && sudo bash install.sh
```

安装脚本自动完成：检测系统 → 安装 Docker → 拉取镜像 → 配置 systemd 自启 → 启动服务。

### 本地开发

```bash
# 构建并启动
docker compose up -d --build

# 查看日志
docker compose logs -f

# 停止
docker compose down
```

### 访问

- **前端 UI**: http://localhost:25540
- **后端 API**: http://localhost:25541

## 项目结构

```
JMCL-for-servers/
├── install.sh                    # 一键远程安装脚本
├── build-and-push.sh             # 本地构建推送镜像
├── .github/workflows/            # GitHub Actions CI/CD
├── backend-core/                 # Java 后端 (Spring Boot)
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/jmcl/servers/core/
│       ├── controller/           # REST API 控制器
│       │   ├── ServerController.java
│       │   ├── DownloadController.java
│       │   └── ConfigController.java
│       ├── service/              # 业务逻辑
│       │   ├── McServerService.java
│       │   ├── ServerInstanceManager.java
│       │   ├── DownloadService.java
│       │   ├── ConfigService.java
│       │   └── ServerTypeRegistry.java
│       └── model/                # 数据模型
│           ├── ServerInstance.java
│           ├── ServerConfig.java
│           ├── ServerType.java
│           ├── DownloadRequest.java
│           └── LogEntry.java
├── frontend/                     # TypeScript 前端 (React + MD3)
│   ├── package.json
│   ├── Dockerfile
│   ├── nginx.conf
│   └── src/
│       ├── theme.ts              # MD3 主题系统
│       ├── App.tsx               # 路由和布局
│       ├── components/           # MD3 组件
│       │   ├── Button.tsx
│       │   ├── Card.tsx
│       │   ├── TextField.tsx
│       │   ├── Dialog.tsx
│       │   ├── Snackbar.tsx
│       │   ├── Spinner.tsx
│       │   ├── Sidebar.tsx
│       │   └── Terminal.tsx
│       ├── pages/                # 页面
│       │   ├── DashboardPage.tsx
│       │   ├── ServerListPage.tsx
│       │   ├── ServerConfigPage.tsx
│       │   ├── DownloadPage.tsx
│       │   └── LogsPage.tsx
│       └── stores/               # 状态管理
│           └── uiStore.ts
├── docker-compose.yml            # 容器编排
├── servers download.md           # 服务端下载源文档
└── README.md
```

## 技术栈

| 层级 | 技术 | 说明 |
|------|------|------|
| 安装 | Bash | 一键远程安装脚本 |
| 后端 | Java 21 + Spring Boot 3.2 | REST API 服务 |
| 前端 | TypeScript + React 18 + MD3 | Web 管理界面 |
| 编排 | Docker Compose | 容器化部署 |
| 服务管理 | systemd / launchd | 开机自启 |

## API 端点

### 服务器管理
- `GET    /api/servers`              - 列出所有服务器
- `GET    /api/servers/{id}`         - 获取服务器详情
- `DELETE /api/servers/{id}`         - 删除服务器
- `POST   /api/servers/{id}/start`   - 启动服务器
- `POST   /api/servers/{id}/stop`    - 停止服务器
- `POST   /api/servers/{id}/restart` - 重启服务器
- `POST   /api/servers/{id}/command` - 发送控制台命令
- `GET    /api/servers/{id}/logs`    - 获取服务器日志
- `GET    /api/servers/statuses`     - 获取所有状态

### 下载管理
- `GET    /api/download/types`       - 获取支持的服务端类型
- `POST   /api/download/start`       - 开始下载服务端

### 配置管理
- `GET    /api/config/{serverId}`    - 获取服务器配置
- `PUT    /api/config/{serverId}`    - 更新服务器配置

## License

MIT
