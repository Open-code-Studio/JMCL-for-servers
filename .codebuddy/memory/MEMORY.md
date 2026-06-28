# JMCL-for-servers 项目

## 项目概述
JMCL-for-servers 是 Minecraft Java Edition 服务器启动管理器，使用 Docker 容器化部署，通过 Web UI 管理多个 MC 服务器。

## 技术栈
- **安装器**: Java 21 (跨平台一键安装)
- **后端**: Java 21 + Spring Boot 3.2 (REST API)
- **前端**: TypeScript + React 18 + Material Design 3 (Web UI)
- **部署**: Docker Compose (两个容器: 后端核心 + 前端)

## 项目结构
- `installer/` - Java 安装器 (OsDetector/DockerInstaller/ServiceConfigurator)，支持 Linux systemd 和 macOS launchd 自启配置
- `backend-core/` - Spring Boot 后端，管理 MC 服务器进程生命周期，提供 REST API，支持 9 种服务端类型下载
- `frontend/` - React + TS 前端，MD3 主题(明暗)/Sidebar/组件库/5 个页面(Dashboard/ServerList/ServerConfig/Download/Logs)
- `docker-compose.yml` - 容器编排，前端 27781 端口，后端 27780 端口

## 支持的服务端类型
VANILLA, PAPER, PURPUR, FOLIA, FABRIC, QUILT, FORGE, NEOFORGE, VELOCITY

## 端口
- Frontend: 27781
- Backend API: 27780
- MC Servers: 由用户配置

## 数据持久化
- Docker volume: jmcl_data
- 宿主机: /data (容器内), /opt/jmcl-servers/data 或 /Library/JMCL-Servers/data
