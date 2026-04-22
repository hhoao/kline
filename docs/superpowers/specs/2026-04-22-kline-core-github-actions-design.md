# kline-core GitHub Actions 设计

## 目标

为 `kline-common` 与 `kline-core` 增加 GitHub Actions 自动化流程：

- 日常 `CI`：在 `push` 与 `pull_request` 时自动验证构建
- 手动 `CD`：在 GitHub Actions 页面手动触发发布到 Maven Central

## 设计选择

本次采用两个 workflow 分离职责：

- `ci.yml`：负责构建验证
- `release.yml`：负责手动发布

这样可以保持校验流程简单稳定，同时避免每次提交都触发发布相关逻辑。

## CI 范围

- 工作目录：`kline-server`
- 仅构建与验证 `kline-common`、`kline-core`
- 使用 Java 21
- 使用 Maven 依赖缓存
- 运行命令：

```bash
mvn -pl kline-common,kline-core -am -DskipTests verify
```

## 发布范围

- 触发方式：`workflow_dispatch`
- 发布命令：

```bash
mvn -pl kline-common,kline-core -am -Prelease-to-central deploy
```

- 使用 GitHub Secrets 注入：
  - Central Portal token username
  - Central Portal token password
  - GPG private key
  - GPG passphrase

## 安全约束

- 凭据只来自 GitHub Secrets，不写入仓库
- workflow 内动态生成 `~/.m2/settings.xml`
- workflow 内动态导入 GPG 私钥

## 验证标准

- `ci.yml` 可以在无 secrets 的情况下正常运行
- `release.yml` 语法正确，并具备完整发布前置步骤
- 仓库内保留清晰的 secrets 命名约定，便于后续配置
