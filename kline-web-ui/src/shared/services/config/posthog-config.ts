export interface PostHogClientConfig {
	/**
	 * The main API key for PostHog telemetry service.
	 */
	apiKey?: string | undefined
	/**
	 * The API key for PostHog used only for error tracking service.
	 */
	errorTrackingApiKey?: string | undefined
	host: string
	uiHost: string
}

/**
 * Helper type for a valid PostHog client configuration.
 * Must contains api keys for both telemetry and error tracking.
 */
export interface PostHogClientValidConfig extends PostHogClientConfig {
	apiKey: string
	errorTrackingApiKey: string
}

/**
 * PostHog configuration for Production Environment.
 * NOTE: The production environment variables will be injected at build time in CI/CD pipeline.
 * IMPORTANT: The secrets must be added to the GitHub Secrets and matched with the environment variables names
 * defined in the .github/workflows/publish.yml workflow.
 * NOTE: The development environment variables should be retrieved from 1password shared vault.
 */
export const posthogConfig: PostHogClientConfig = {
	apiKey: "",
	errorTrackingApiKey: "",
	host: "https://data.cline.bot",
	uiHost: "https://us.i.posthog.com",
}


export function isPostHogConfigValid(config: PostHogClientConfig): config is PostHogClientValidConfig {
	// Allow invalid config in test environment to enable mocking and stubbing
	return (
		typeof config.apiKey === "string" &&
		typeof config.errorTrackingApiKey === "string" &&
		typeof config.host === "string" &&
		typeof config.uiHost === "string"
	)
}
