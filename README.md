# Kline

Kline 是一个参照 [Cline](https://github.com/cline/cline) 做的 Java 版本 AI Agent 自托管后端服务，支持多会话管理、上下文、Checkpoint、知识库、工具调用、MCP 等核心能力，配套 Vue3 前端 UI。

它基于 Cline 的架构做了一些修改，核心在于将对话流程从单链路任务循环，改为了基于事件状态机驱动的任务循环，架构更加清晰，同时不再需要等待用户响应，也能快速恢复状态。

目前还处于早期开发阶段，主要功能已经完成，正在完善细节和测试。

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

- 事件状态机驱动的任务循环
- 无停顿流式文本和工具输出
- 工具调用（读取、创建和编辑文件)
- 执行命令行
- 使用浏览器
- 使用 MCP 工具
- 使用知识库
- 模型管理
- 上下文管理
- 会话管理
- Checkpoint 管理

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
