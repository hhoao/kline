# kline-core Maven Central 发布设计

## 目标

将 `kline-core` 发布到 Maven Central，并保证外部项目可以直接通过 Maven/Gradle 正常解析依赖。

本次发布采用 `io.github.hhoao` 作为 namespace。

## 发布范围

首次发布仅包含以下 Maven 坐标：

- `io.github.hhoao:kline-parent`
- `io.github.hhoao:kline-common`
- `io.github.hhoao:kline-core`

不在本次发布范围内：

- `kline-plugins`
- `kline-web`
- 任何前端资源

## 设计选择

选择在父 POM 统一配置发布逻辑，而不是单独重构 `kline-core` 为完全独立模块。

原因：

- `kline-core` 当前继承父 POM。
- `kline-core` 当前依赖 `kline-common`。
- 统一在父 POM 配置更符合当前多模块结构，改动最小。

## 需要补齐的发布能力

父 POM 需要补齐以下 Maven Central 必需能力：

- 项目元数据：`name`、`description`、`url`
- 许可证信息：Apache-2.0
- 开发者信息
- SCM 信息，指向 GitHub 仓库
- `sources.jar`
- `javadoc.jar`
- GPG 签名
- Sonatype Central Portal 发布插件

## 模块调整

### kline-parent

- 将 `groupId` 切换为 `io.github.hhoao`
- 将版本从 `-SNAPSHOT` 调整为正式版本
- 提供统一的发布插件和 Central 所需元数据

### kline-common

- 继承新的父坐标
- 跟随父 POM 参与发布

### kline-core

- 继承新的父坐标
- 保留对 `kline-common` 的依赖
- 跟随父 POM 参与发布

## 发布命令

计划支持以下发布方式：

```bash
cd kline-server
mvn -pl kline-common,kline-core -am clean deploy
```

说明：

- `-am` 会一并构建所需的父 POM。
- 实际发布前需要在 `~/.m2/settings.xml` 中配置 `central` server 凭据。
- 实际发布前需要本机可用的 GPG 密钥。

## 风险与约束

- 如果本机没有 GPG 密钥，签名阶段会失败。
- 如果 Central Portal 尚未验证 `io.github.hhoao` namespace，上传会失败。
- 如果开发者信息或 SCM 信息缺失，Central 校验会失败。
- 如果只发布 `kline-core` 而不发布 `parent` 与 `kline-common`，消费者解析依赖会失败。

## 验证方式

在真正发布前，至少完成以下验证：

- `mvn -pl kline-common,kline-core -am clean verify`
- `mvn -pl kline-common,kline-core -am -DskipTests package`
- 检查生成的 `jar`、`-sources.jar`、`-javadoc.jar`、`.asc` 文件是否齐全

## 实施边界

本次实施只修改 Maven 发布相关配置，不重构 Java 代码，不调整运行时逻辑。
