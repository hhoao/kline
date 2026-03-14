import { ref, watchEffect } from "vue"

/**
 * Vue composable to check feature flag status in the webview
 * Feature flags work independently of telemetry settings to ensure
 * proper extension functionality regardless of user privacy preferences
 * @param flagName The name of the feature flag to check
 * @returns A ref containing the feature flag status (boolean)
 */
export const useHasFeatureFlag = (flagName: string) => {
	const flagEnabled = ref<boolean>(false)

	watchEffect(() => {
		// Try to access posthog from window object or global scope
		// This handles cases where posthog is initialized globally
		const posthogInstance = (window as any).posthog || (globalThis as any).posthog

		if (posthogInstance?.isFeatureEnabled) {
			try {
				const enabled = posthogInstance.isFeatureEnabled(flagName)
				if (typeof enabled === "boolean") {
					flagEnabled.value = enabled
				} else {
					flagEnabled.value = false
				}
			} catch (error) {
				console.warn(`[useHasFeatureFlag] Error checking feature flag "${flagName}":`, error)
				flagEnabled.value = false
			}
		} else {
			// Fallback to false if posthog is not initialized
			flagEnabled.value = false
		}
	})

	return flagEnabled
}
