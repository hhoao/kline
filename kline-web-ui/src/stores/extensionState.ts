import { modelsService } from '@/api/models'
import { stateService } from '@/api/state'
import { taskService } from '@/api/task'
import { uiService } from '@/api/ui'
import type { ModelInfo } from '@/shared/api'
import {
	basetenDefaultModelId,
	basetenModels,
	groqDefaultModelId,
	groqModels,
	openRouterDefaultModelId,
	openRouterDefaultModelInfo,
	requestyDefaultModelId,
	requestyDefaultModelInfo,
	vercelAiGatewayDefaultModelId,
	vercelAiGatewayDefaultModelInfo,
} from '@/shared/api'
import { findLastIndex } from '@/shared/array'
import type { DictationSettings } from '@/shared/DictationSettings'
import type { ClineMessage, ExtensionState } from '@/shared/ExtensionMessage'
import { HistoryItem } from '@/shared/HistoryItem'
import type { McpMarketplaceCatalog, McpResource, McpServer, McpTool, McpViewTab } from '@/shared/mcp'
import type { UserInfo } from '@/shared/proto/cline/account'
import type { McpResource as ProtoMcpResource, McpServer as ProtoMcpServer, McpTool as ProtoMcpTool } from '@/shared/proto/cline/mcp'
import { McpServerStatus } from '@/shared/proto/cline/mcp'
import type { OpenRouterModelInfo } from '@/shared/proto/cline/models'
import type { TerminalProfile } from '@/shared/proto/cline/state'
import { SubscriptionMessage } from '@/types/subscription'
import { getAppEnvConfig } from '@/utils/env'
import { defineStore } from 'pinia'
import { subscriptionHandlers } from '../handlers/subscription'
import { SSEClient } from '../utils/sse'
import { getTokenFromStorage, saveTokenToStorage } from '../utils/token'

// 默认 satoken
const DEFAULT_SATOKEN = 'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJsb2dpblR5cGUiOiJsb2dpbiIsImxvZ2luSWQiOjEsImRldmljZVR5cGUiOiJERUYiLCJlZmYiOjE3NjI4Mjg5MDUwMTMsInJuU3RyIjoiSGpOaVFPOTU5REVaQkJGTDFTMU55b1lQMXF6eEZoRzAiLCJ1c2VybmFtZSI6InN5c2FkbWluIn0.U0V0kpvdJOSyXrbrH9tWs9ASFpmGhhNXWnweAL-Wzno'

// Conversion functions - temporary implementations
// TODO: Create proper conversion functions in proto-conversions directory

/**
 * Convert proto MCP tool to application MCP tool
 */
function convertProtoMcpTool(protoTool: ProtoMcpTool): McpTool {
	return {
		name: protoTool.name,
		description: protoTool.description,
		inputSchema: protoTool.inputSchema ? (typeof protoTool.inputSchema === 'string' ? JSON.parse(protoTool.inputSchema) : protoTool.inputSchema) : undefined,
	}
}

/**
 * Convert proto MCP resource to application MCP resource
 */
function convertProtoMcpResource(protoResource: ProtoMcpResource): McpResource {
	return {
		uri: protoResource.uri,
		name: protoResource.name,
		description: protoResource.description,
		mimeType: protoResource.mimeType,
	}
}

/**
 * Convert proto MCP servers to application MCP servers
 */
function convertProtoMcpServersToMcpServers(protoServers: ProtoMcpServer[]): McpServer[] {
	return protoServers.map((server) => ({
		name: server.name,
		config: server.config,
		status:
			server.status === McpServerStatus.MCP_SERVER_STATUS_CONNECTED
				? 'connected'
				: server.status === McpServerStatus.MCP_SERVER_STATUS_CONNECTING
					? 'connecting'
					: 'disconnected',
		error: server.error,
		tools: server.tools?.map(convertProtoMcpTool) || [],
		resources: server.resources?.map(convertProtoMcpResource) || [],
		resourceTemplates: [],
		disabled: server.disabled,
		timeout: server.timeout,
	}))
}

/**
 * Convert protobuf models to ModelInfo records
 */
function fromProtobufModels(models: { [key: string]: OpenRouterModelInfo }): Record<string, ModelInfo> {
	const result: Record<string, ModelInfo> = {}
	for (const [key, value] of Object.entries(models)) {
		result[key] = {
			maxTokens: value.maxTokens,
			contextWindow: value.contextWindow,
			supportsImages: value.supportsImages,
			supportsPromptCache: value.supportsPromptCache,
			inputPrice: value.inputPrice,
			outputPrice: value.outputPrice,
			cacheWritesPrice: value.cacheWritesPrice,
			cacheReadsPrice: value.cacheReadsPrice,
			description: value.description,
			thinkingConfig: value.thinkingConfig,
			supportsGlobalEndpoint: value.supportsGlobalEndpoint,
			tiers: value.tiers || [],
		}
	}
	return result
}

/**
 * Extension State Store
 * Manages extension state, models, MCP servers, UI navigation, chat, and app state
 * Replaces React ExtensionStateContext, chat store, and app store
 */
interface ExtensionStateStoreState {
	// Derived state
	didHydrateState: boolean
	showWelcome: boolean

	// App state (from app.ts)
	apiBaseUrl: string
	conversationId: string | null
	token: string

	// Chat state (from chat.ts)
	sendingChat: boolean
	sseClient: SSEClient | null
	extensionState: ExtensionState | null

	// Model lists
	openRouterModels: Record<string, ModelInfo>
	openAiModels: string[]
	requestyModels: Record<string, ModelInfo>
	groqModels: Record<string, ModelInfo>
	basetenModels: Record<string, ModelInfo>
	huggingFaceModels: Record<string, ModelInfo>
	vercelAiGatewayModels: Record<string, ModelInfo>

	// MCP state
	mcpServers: McpServer[]
	mcpMarketplaceCatalog: McpMarketplaceCatalog

	// Other state
	totalTasksSize: number | null
	availableTerminalProfiles: TerminalProfile[]
	expandTaskHeader: boolean

	// UI navigation state
	// Note: Navigation is handled via Vue Router in components
	// These flags are kept for compatibility but should be managed via router
	showSettings: boolean
	showHistory: boolean
	showAccount: boolean
	showMcp: boolean
	mcpTab: McpViewTab | undefined
	showAnnouncement: boolean
	showChatModelSelector: boolean

	// Relinquish control callbacks
	relinquishControlCallbacks: Set<() => void>
}

export const useExtensionStateStore = defineStore('extensionState', {
	state: (): ExtensionStateStoreState => ({
		// Derived state
		didHydrateState: false,
		showWelcome: false,

		// App state
		apiBaseUrl: getAppEnvConfig().VITE_GLOB_API_URL,
		conversationId: null,
		token: getTokenFromStorage('satoken') || DEFAULT_SATOKEN,

		// Chat state
		sendingChat: false,
		sseClient: null,
		extensionState: null,

		// Model lists
		openRouterModels: {
			[openRouterDefaultModelId]: openRouterDefaultModelInfo,
		},
		openAiModels: [],
		requestyModels: {
			[requestyDefaultModelId]: requestyDefaultModelInfo,
		},
		groqModels: {
			[groqDefaultModelId]: groqModels[groqDefaultModelId],
		},
		basetenModels: {
			[basetenDefaultModelId]: basetenModels[basetenDefaultModelId],
		},
		huggingFaceModels: {},
		vercelAiGatewayModels: {
			[vercelAiGatewayDefaultModelId]: vercelAiGatewayDefaultModelInfo,
		},

		// MCP state
		mcpServers: [],
		mcpMarketplaceCatalog: { items: [] },

		// Other state
		totalTasksSize: null,
		availableTerminalProfiles: [],
		expandTaskHeader: true,

		// UI navigation state
		showSettings: false,
		showHistory: false,
		showAccount: false,
		showMcp: false,
		mcpTab: undefined,
		showAnnouncement: false,
		showChatModelSelector: false,

		// Relinquish control callbacks
		relinquishControlCallbacks: new Set(),
	}),

	getters: {
		/**
		 * 聊天消息列表
		 */
		chatMessages(): ClineMessage[] {
			return this.extensionState?.clineMessages || []
		},

		/**
		 * 最近的对话历史
		 */
		recentConversations(): HistoryItem[] {
			return this.extensionState?.taskHistory || []
		},

		/**
		 * 当前对话 ID
		 */
		currentConversationId(): string | null {
			return this.conversationId
		},

		/**
		 * 计算 showWelcome，基于 extensionState.welcomeViewCompleted
		 * 如果 extensionState 不存在，返回 false
		 */
		computedShowWelcome(): boolean {
			return this.extensionState ? !this.extensionState.welcomeViewCompleted : false
		},
	},

	actions: {
		// Actions - App state setters
		setApiBaseUrl(url: string): void {
			this.apiBaseUrl = url
		},

		setConversationId(id: string | null): void {
			this.conversationId = id
		},

		setToken(newToken: string | null | undefined): void {
			// 如果传入空值，使用默认 token
			this.token = newToken || DEFAULT_SATOKEN
			// 同时保存到 localStorage 和 cookie
			if (newToken) {
				saveTokenToStorage(newToken, 'satoken')
			} else {
				// 如果 token 为空，保存默认 token
				saveTokenToStorage(DEFAULT_SATOKEN, 'satoken')
			}
		},

		// Actions - Chat actions
		/**
		 * 发送消息
		 */
		async sendChatMessage(content: string): Promise<any> {
			if (!content.trim() || this.sendingChat) {
				return null
			}

			this.sendingChat = true

			try {
				// 如果没有任务ID，先创建任务
				let targetTaskId: string | null = this.conversationId

				// 如果有任务ID，先添加用户消息
				if (this.extensionState) {
					const newMessages = [...(this.extensionState.clineMessages || [])]
					newMessages.push({
						ts: Date.now(),
						type: 'ask',
						text: content,
					})
					this.extensionState = {
						...this.extensionState,
						clineMessages: newMessages,
					}
				}

				if (!targetTaskId) {
					// 创建新任务，传入消息内容
					const taskId = await taskService.newTask({
						text: content,
					})
					this.setConversationId(taskId)
				} else {
					// 发送消息到任务
					const msgs = this.extensionState?.clineMessages || []
					const lastAskMsg = [...msgs].reverse().find(m => m.type === 'ask')
					await taskService.askResponse({
						responseType: 'messageResponse',
						text: content,
						taskId: targetTaskId,
						pendingId: lastAskMsg?.pendingId,
					})
				}
				return true
			} catch (error) {
				console.error('发送消息失败:', error)
				const errorMessage =
					error instanceof Error ? error.message : '发送消息失败，请稍后重试'
				if (this.extensionState) {
					const newMessages = [...(this.extensionState.clineMessages || [])]
					newMessages.push({
						ts: Date.now(),
						type: 'say',
						text: `错误: ${errorMessage}`,
					})
					this.extensionState = {
						...this.extensionState,
						clineMessages: newMessages,
					}
				}
				throw error
			} finally {
				this.sendingChat = false
			}
		},

		/**
		 * 选择任务（对应选择对话）
		 */
		async selectConversation(taskId: string): Promise<void> {
			await taskService.showTaskWithId(taskId)
			this.setConversationId(taskId)
		},

		/**
		 * 启动消息订阅（通过 SSE）
		 */
		async startSubscription(): Promise<void> {
			// 如果已有连接且正在连接或已连接，直接返回
			if (this.sseClient) {
				console.log('[SSE] 部分消息订阅已存在，跳过重复创建')
				return
			}

			try {
				// 构建完整的URL
				const baseURL = this.apiBaseUrl || ''
				const endpoint = '/api/cline/ui/subscribe'
				const url = `${baseURL}${endpoint}`

				const client = new SSEClient<SubscriptionMessage>({
					url: url,
					autoReconnect: true,
					reconnectDelay: 3000,
					maxReconnectAttempts: Infinity,
					heartbeatInterval: 30000,
					heartbeatTimeout: 5000,
					debug: true,
				})

				// 使用循环注册所有消息类型处理器
				for (const handler of subscriptionHandlers) {
					const messageType = handler.getMessageType()
					client.onMessageType(messageType, (message: SubscriptionMessage) => {
						handler.handle(message)
					})
				}

				// 注册错误监听器
				client.on('error', (error: Event) => {
					console.error('[SSE] 部分消息订阅错误:', error)
				})

				client.on('open', () => {
					console.log('[SSE] 部分消息订阅已连接')
				})

				// 注册连接关闭监听器
				client.on('close', () => {
					console.log('[SSE] 部分消息订阅已关闭')
					// 连接关闭时，清理客户端引用
					if (this.sseClient === client) {
						this.sseClient = null
					}
				})

				this.sseClient = client
				await client.connect()

				try {
					const stateJson = await stateService.getLatestState()
					if (stateJson) {
						const state = JSON.parse(stateJson) as ExtensionState
						this.updateExtensionState(state)
					}
				} catch (error) {
					console.error('[SSE] 获取最新状态失败:', error)
				}
			} catch (error) {
				console.error('[SSE] 启动部分消息订阅失败:', error)
				this.sseClient = null
			}
		},

		/**
		 * 停止订阅
		 */
		stopSubscription(): void {
			if (this.sseClient) {
				this.sseClient.disconnect()
				this.sseClient = null
				console.log('[SSE] 状态订阅已停止')
			}
		},

		// Actions - Setters
		setDidHydrateState(value: boolean) {
			this.didHydrateState = value
		},

		setOpenRouterModels(models: Record<string, ModelInfo>) {
			this.openRouterModels = models
		},

		setRequestyModels(models: Record<string, ModelInfo>) {
			this.requestyModels = models
		},

		setGroqModels(models: Record<string, ModelInfo>) {
			this.groqModels = models
		},

		setBasetenModels(models: Record<string, ModelInfo>) {
			this.basetenModels = models
		},

		setHuggingFaceModels(models: Record<string, ModelInfo>) {
			this.huggingFaceModels = models
		},

		setVercelAiGatewayModels(models: Record<string, ModelInfo>) {
			this.vercelAiGatewayModels = models
		},

		setMcpServers(servers: McpServer[]) {
			this.mcpServers = servers
		},

		setMcpMarketplaceCatalog(catalog: McpMarketplaceCatalog) {
			this.mcpMarketplaceCatalog = catalog
		},

		setTotalTasksSize(size: number | null) {
			this.totalTasksSize = size
		},

		setAvailableTerminalProfiles(profiles: TerminalProfile[]) {
			this.availableTerminalProfiles = profiles
		},

		setExpandTaskHeader(value: boolean) {
			this.expandTaskHeader = value
		},

		setShowAnnouncement(value: boolean) {
			this.showAnnouncement = value
		},

		setShowChatModelSelector(value: boolean) {
			this.showChatModelSelector = value
		},

		setMcpTab(tab?: McpViewTab) {
			this.mcpTab = tab
		},

		// Actions - Extension state setters
		setShouldShowAnnouncement(value: boolean) {
			if (this.extensionState) {
				this.extensionState = {
					...this.extensionState,
					shouldShowAnnouncement: value,
				}
			}
		},

		setUserInfo(userInfo?: UserInfo) {
			if (this.extensionState) {
				this.extensionState = {
					...this.extensionState,
					userInfo,
				}
			}
		},

		setDictationSettings(value: DictationSettings) {
			if (this.extensionState) {
				this.extensionState = {
					...this.extensionState,
					dictationSettings: value,
				}
			}
		},

		setGlobalClineRulesToggles(toggles: Record<string, boolean>) {
			if (this.extensionState) {
				this.extensionState = {
					...this.extensionState,
					globalClineRulesToggles: toggles,
				}
			}
		},

		setLocalClineRulesToggles(toggles: Record<string, boolean>) {
			if (this.extensionState) {
				this.extensionState = {
					...this.extensionState,
					localClineRulesToggles: toggles,
				}
			}
		},

		setLocalCursorRulesToggles(toggles: Record<string, boolean>) {
			if (this.extensionState) {
				this.extensionState = {
					...this.extensionState,
					localCursorRulesToggles: toggles,
				}
			}
		},

		setLocalWindsurfRulesToggles(toggles: Record<string, boolean>) {
			if (this.extensionState) {
				this.extensionState = {
					...this.extensionState,
					localWindsurfRulesToggles: toggles,
				}
			}
		},

		setLocalWorkflowToggles(toggles: Record<string, boolean>) {
			if (this.extensionState) {
				this.extensionState = {
					...this.extensionState,
					localWorkflowToggles: toggles,
				}
			}
		},

		setGlobalWorkflowToggles(toggles: Record<string, boolean>) {
			if (this.extensionState) {
				this.extensionState = {
					...this.extensionState,
					globalWorkflowToggles: toggles,
				}
			}
		},

		// Actions - Hide functions
		hideAnnouncement() {
			this.setShowAnnouncement(false)
		},

		hideChatModelSelector() {
			this.setShowChatModelSelector(false)
		},

		// Actions - Refresh functions
		async refreshOpenRouterModels() {
			try {
				const response = await modelsService.refreshOpenRouterModelsRpc()
				// Note: Response format may need adjustment based on actual API
				if (response && response.models) {
					const models = fromProtobufModels(response.models)
					this.setOpenRouterModels({
						[openRouterDefaultModelId]: openRouterDefaultModelInfo,
						...models,
					})
				}
			} catch (error) {
				console.error('Failed to refresh OpenRouter models:', error)
			}
		},

		// Actions - Event callbacks
		onRelinquishControl(callback: () => void) {
			this.relinquishControlCallbacks.add(callback)
			return () => {
				this.relinquishControlCallbacks.delete(callback)
			}
		},

		// Initialize - fetch available terminal profiles
		async initializeTerminalProfiles() {
			try {
				const response = await stateService.getAvailableTerminalProfiles()
				// Note: API returns string, may need to parse JSON
				// For now, handle as string and parse if needed
				if (response && typeof response === 'string') {
					try {
						const parsed = JSON.parse(response)
						if (parsed && parsed.profiles) {
							this.setAvailableTerminalProfiles(parsed.profiles)
						}
					} catch {
						// If parsing fails, response might already be an object
						console.warn('Failed to parse terminal profiles response')
					}
				} else if (response && typeof response === 'object' && 'profiles' in response) {
					this.setAvailableTerminalProfiles((response as any).profiles)
				}
			} catch (error) {
				console.error('Failed to fetch available terminal profiles:', error)
			}
		},

		// Initialize webview
		async initializeWebview() {
			try {
				await uiService.initializeWebview()
				console.log('[DEBUG] Webview initialization completed')
			} catch (error) {
				console.error('Failed to initialize webview:', error)
			}
		},

		// Handle state updates from SSE (called by StateMessageHandler)
		updateExtensionState(state: ExtensionState) {
			if (!state) return

			// Versioning logic for autoApprovalSettings
			const incomingVersion = state.autoApprovalSettings?.version ?? 1
			const currentVersion = this.extensionState?.autoApprovalSettings?.version ?? 1
			const shouldUpdateAutoApproval = incomingVersion > currentVersion

			const newState: ExtensionState = {
				...state,
				autoApprovalSettings: shouldUpdateAutoApproval
					? state.autoApprovalSettings
					: (this.extensionState?.autoApprovalSettings || state.autoApprovalSettings),
			}

			// Update welcome screen state
			this.showWelcome = !newState.welcomeViewCompleted
			this.setDidHydrateState(true)

			// Only auto-switch conversationId when no task is currently selected,
			// to avoid interrupting the user when they're viewing another task.
			if (!this.conversationId) {
				this.setConversationId(newState.currentTaskItem?.id || null)
			}
			this.extensionState = newState
		},

		// Handle partial message updates
		updatePartialMessage(partialMessage: ClineMessage) {
			try {
				// Validate critical fields
				if (!partialMessage.ts || partialMessage.ts <= 0) {
					console.error('Invalid timestamp in partial message:', partialMessage)
					return
				}

				if (!this.extensionState) return

				const messages = this.extensionState.clineMessages || []
				const lastIndex = findLastIndex(messages, (msg) => msg.ts === partialMessage.ts)

				if (lastIndex !== -1) {
					const newClineMessages = [...messages]
					newClineMessages[lastIndex] = partialMessage
					this.extensionState = {
						...this.extensionState,
						clineMessages: newClineMessages,
					}
				}
			} catch (error) {
				console.error('Failed to process partial message:', error, partialMessage)
			}
		},

		// Handle MCP servers update
		updateMcpServers(response: any) {
			if (response && response.mcpServers) {
				this.setMcpServers(convertProtoMcpServersToMcpServers(response.mcpServers))
			}
		},

		// Handle OpenRouter models update
		updateOpenRouterModels(response: any) {
			if (response && response.models) {
				const models = fromProtobufModels(response.models)
				this.setOpenRouterModels({
					[openRouterDefaultModelId]: openRouterDefaultModelInfo,
					...models,
				})
			}
		},

		// Handle relinquish control event
		handleRelinquishControl() {
			this.relinquishControlCallbacks.forEach((callback) => {
				callback()
			})
		},

		/**
		 * 初始化 store
		 * 应该在应用启动时调用一次
		 */
		async init() {
			await this.startSubscription()
			// await this.initializeTerminalProfiles()
			// await this.initializeWebview()
		},
	},
})
