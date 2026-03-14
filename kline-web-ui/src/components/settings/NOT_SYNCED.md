# Cline Settings 与 Vue 同步说明

与 Cline React `webview-ui/src/components/settings` 对比后的同步状态。

## 已同步

- **SettingsView**：Tab 布局、tab 列表（API Configuration, Features, Browser, Terminal, General, About, Debug(dev)）、Done 按钮；Tab 悬停 title（tooltipText：About Cline、Debug Tools 等）、Tab 按钮 data-testid/data-value/data-compact 与 React 一致。
- **Section / SectionHeader**：结构一致。
- **sections/**：ApiConfigurationSection（Plan/Act + GenericProvider）、GeneralSettingsSection、FeatureSettingsSection、**BrowserSettingsSection**（文案与占位符已与 React 对齐；连接状态/Relaunch/检测路径依赖后端未实现）、TerminalSettingsSection、AboutSection、DebugSection。
- **common/**：DebouncedTextField、CollapsibleContent、**ErrorMessage**、**ModelInfoView**（简化版）、**BaseUrlField**、**ApiKeyField**、**ModelSelector**、**ContextWindowSwitcher**。
- **providers/**：GenericProvider.vue（已使用 ModelSelector、ApiKeyField）+ providerConfig.ts（mistral、deepseek、nebius、moonshot、minimax 等通用 provider）。
- **utils/**：settingsHandlers、useDebouncedInput、**pricingUtils**（formatPrice、formatTokenPrice、hasThinkingBudget、supportsImages、supportsPromptCache、formatTokenLimit 等）。
- **PreferredLanguageSetting**、**TerminalOutputLineLimitSlider**、**SubagentOutputLineLimitSlider**、**ThinkingBudgetSlider**（已接入 API Configuration，按 Plan/Act 显示）、**UseCustomPromptCheckbox**（API Configuration 内）。

## 未同步 / 简化

- **ApiOptions.tsx 全量**：React 有完整 provider 下拉 + 各独立 Provider 组件（Anthropic、OpenRouter、OpenAI Compatible 等 30+）。Vue 使用 GenericProvider + provider 下拉，仅覆盖通用 provider；其余 provider 在聊天区配置。
- **utils/useApiConfigurationHandlers.ts、providerUtils.ts**：未同步（为 React ApiOptions/Provider 服务）。Vue 使用 stateService.updateSettings/updateSecrets 与 providerConfig。
- **各独立 Provider 组件**（如 OllamaProvider、OpenAICompatible.tsx 等）：未迁移；Vue 通过 GenericProvider 覆盖部分，其余依赖后端/聊天头配置。
- **OllamaModelPicker、OpenRouterModelPicker、ModelDescriptionMarkdown、FeaturedModelCard、ClineAccountInfoCard** 等：未迁移；需要时可在 Vue 中按需实现。
- **Subagents**：React 在 macOS/Linux + VSCODE 下展示 CLI 安装检测与「Install Now」；Vue 仅根据 `subagentsEnabled` 展示开关与 SubagentOutputLineLimitSlider，无平台/CLI 逻辑。
- **BrowserSettingsSection**：文案与占位符已与 React 对齐（远程浏览器说明、Chrome 路径/Custom args 占位符及说明）。连接状态检测、Relaunch、Debug 模式、检测到的 Chrome 路径等依赖后端 gRPC，Vue 未实现。
- **__tests__/**、**README.md**：未迁移。
