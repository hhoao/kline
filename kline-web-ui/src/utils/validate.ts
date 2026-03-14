import { ApiConfiguration, getApiKey, ModelInfo, openRouterDefaultModelId } from "@/shared/api"
import { Mode } from "@/shared/storage/types"

export function validateApiConfiguration(currentMode: Mode, apiConfiguration?: ApiConfiguration): string | undefined {
	if (apiConfiguration) {
		const apiProvider = currentMode === 'plan' ? apiConfiguration.planModeApiProvider : apiConfiguration.actModeApiProvider
		const openAiModelId = currentMode === 'plan' ? apiConfiguration.planModeApiModelId : apiConfiguration.actModeApiModelId
		const requestyModelId = currentMode === 'plan' ? apiConfiguration.planModeApiModelId : apiConfiguration.actModeApiModelId
		const togetherModelId = currentMode === 'plan' ? apiConfiguration.planModeApiModelId : apiConfiguration.actModeApiModelId
		const ollamaModelId = currentMode === 'plan' ? apiConfiguration.planModeApiModelId : apiConfiguration.actModeApiModelId
		const lmStudioModelId = currentMode === 'plan' ? apiConfiguration.planModeLmStudioModelId : apiConfiguration.actModeLmStudioModelId
		const vsCodeLmModelSelector = currentMode === 'plan' ? apiConfiguration.planModeVsCodeLmModelSelector : apiConfiguration.actModeVsCodeLmModelSelector

		switch (apiProvider) {
			case "anthropic":
				if (!getApiKey(apiConfiguration, 'anthropic')) {
					return "You must provide a valid API key or choose a different provider."
				}
				break
			case "bedrock":
				if (!apiConfiguration.awsRegion) {
					return "You must choose a region to use with AWS Bedrock."
				}
				break
			case "openrouter":
				if (!getApiKey(apiConfiguration, 'openrouter')) {
					return "You must provide a valid API key or choose a different provider."
				}
				break
			case "vertex":
				if (!apiConfiguration.vertexProjectId || !apiConfiguration.vertexRegion) {
					return "You must provide a valid Google Cloud Project ID and Region."
				}
				break
			case "gemini":
				if (!getApiKey(apiConfiguration, 'gemini')) {
					return "You must provide a valid API key or choose a different provider."
				}
				break
			case "openai-native":
				if (!getApiKey(apiConfiguration, 'openai-native')) {
					return "You must provide a valid API key or choose a different provider."
				}
				break
			case "deepseek":
				if (!getApiKey(apiConfiguration, 'deepseek')) {
					return "You must provide a valid API key or choose a different provider."
				}
				break
			case "xai":
				if (!getApiKey(apiConfiguration, 'xai')) {
					return "You must provide a valid API key or choose a different provider."
				}
				break
			case "qwen":
				if (!getApiKey(apiConfiguration, 'qwen')) {
					return "You must provide a valid API key or choose a different provider."
				}
				break
			case "doubao":
				if (!getApiKey(apiConfiguration, 'doubao')) {
					return "You must provide a valid API key or choose a different provider."
				}
				break
			case "mistral":
				if (!getApiKey(apiConfiguration, 'mistral')) {
					return "You must provide a valid API key or choose a different provider."
				}
				break
			case "cline":
				break
			case "openai":
				if (!apiConfiguration.openAiBaseUrl || !getApiKey(apiConfiguration, 'openai') || !openAiModelId) {
					return "You must provide a valid base URL, API key, and model ID."
				}
				break
			case "requesty":
				if (!getApiKey(apiConfiguration, 'requesty')) {
					return "You must provide a valid API key or choose a different provider."
				}
				break
			case "fireworks":
				if (!getApiKey(apiConfiguration, 'fireworks')) {
					return "You must provide a valid API key or choose a different provider."
				}
				break
			case "together":
				if (!getApiKey(apiConfiguration, 'together') || !togetherModelId) {
					return "You must provide a valid API key or choose a different provider."
				}
				break
			case "ollama":
				if (!ollamaModelId) {
					return "You must provide a valid model ID."
				}
				break
			case "lmstudio":
				if (!lmStudioModelId) {
					return "You must provide a valid model ID."
				}
				break
			case "vscode-lm":
				if (!vsCodeLmModelSelector) {
					return "You must provide a valid model selector."
				}
				break
			case "moonshot":
				if (!getApiKey(apiConfiguration, 'moonshot')) {
					return "You must provide a valid API key or choose a different provider."
				}
				break
			case "nebius":
				if (!getApiKey(apiConfiguration, 'nebius')) {
					return "You must provide a valid API key or choose a different provider."
				}
				break
			case "asksage":
				if (!getApiKey(apiConfiguration, 'asksage')) {
					return "You must provide a valid API key or choose a different provider."
				}
				break
			case "sambanova":
				if (!getApiKey(apiConfiguration, 'sambanova')) {
					return "You must provide a valid API key or choose a different provider."
				}
				break
			case "sapaicore":
				if (!apiConfiguration.sapAiCoreBaseUrl) {
					return "You must provide a valid Base URL key or choose a different provider."
				}
				if (!apiConfiguration.sapAiCoreClientId) {
					return "You must provide a valid Client Id or choose a different provider."
				}
				if (!apiConfiguration.sapAiCoreClientSecret) {
					return "You must provide a valid Client Secret or choose a different provider."
				}
				if (!apiConfiguration.sapAiCoreTokenUrl) {
					return "You must provide a valid Auth URL or choose a different provider."
				}
				break
			case "zai":
				if (!getApiKey(apiConfiguration, 'zai')) {
					return "You must provide a valid API key or choose a different provider."
				}
				break
			case "dify":
				if (!apiConfiguration.difyBaseUrl) {
					return "You must provide a valid Base URL or choose a different provider."
				}
				if (!getApiKey(apiConfiguration, 'dify')) {
					return "You must provide a valid API key or choose a different provider."
				}
				break
			case "minimax":
				if (!getApiKey(apiConfiguration, 'minimax')) {
					return "You must provide a valid API key or choose a different provider."
				}
				break
		}
	}
	return undefined
}

export function validateModelId(
	currentMode: Mode,
	apiConfiguration?: ApiConfiguration,
	openRouterModels?: Record<string, ModelInfo>,
): string | undefined {
	if (apiConfiguration) {
		const apiProvider = currentMode === 'plan' ? apiConfiguration.planModeApiProvider : apiConfiguration.actModeApiProvider
		const openRouterModelId = currentMode === 'plan' ? apiConfiguration.planModeOpenRouterModelId : apiConfiguration.actModeOpenRouterModelId
		switch (apiProvider) {
			case "openrouter":
			case "cline":
				const modelId = openRouterModelId || openRouterDefaultModelId
				if (!modelId) {
					return "You must provide a model ID."
				}
				if (modelId.startsWith("@preset/")) {
					break
				}
				if (openRouterModels && !Object.keys(openRouterModels).includes(modelId)) {
					return "The model ID you provided is not available. Please choose a different model."
				}
				break
		}
	}
	return undefined
}
