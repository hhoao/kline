# Kline

Kline 是一个 AI 编程助手的自托管后端服务，提供对话、知识库、工具调用（MCP）等核心能力，配套 Vue3 前端 UI。

## 项目结构

```text
kline/
├── kline-server/          # 后端（Spring Boot）
│   ├── kline-common/      # 公共工具、异常、枚举
│   ├── kline-core/        # 核心逻辑
│   ├── kline-plugins/     # 插件（JDBC 等）
│   └── kline-web/         # Web 层（Controller、API）
└── kline-web-ui/          # 前端（Vue3 + TypeScript + Vite）
```

## 主要功能

- **AI 对话**：多会话管理，支持流式消息
- **知识库**：文档上传、分段管理、向量检索
- **模型管理**：支持配置多种 AI 模型与 API Key
- **工具调用**：内置工具 + MCP（Model Context Protocol）支持
- **文件管理**：上传与引用文件作为上下文
- **多数据库**：MySQL、Oracle、SQL Server、达梦、金仓、openGauss 等


## 快速开始

### 后端

```bash
cd kline-server
# 修改 kline-web/kline-web-server/src/main/resources/application-dev.yml 中的数据库配置
mvn spring-boot:run
# 服务默认启动在 http://localhost:9527
```

### 前端

```bash
cd kline-web-ui
pnpm install
pnpm dev
```

## 许可证

本项目基于 [LICENSE](LICENSE) 授权开源。
