export interface NavigatorUAData {
	platform: string
	brands: { brand: string; version: string }[]
}

export const unknown = "Unknown"

const platforms = {
	windows: /win32/,
	mac: /darwin/,
	linux: /linux/,
}

export const detectOS = (platform: string) => {
	let detectedOs = unknown
	if (platform.match(platforms.windows)) {
		detectedOs = "windows"
	} else if (platform.match(platforms.mac)) {
		detectedOs = "mac"
	} else if (platform.match(platforms.linux)) {
		detectedOs = "linux"
	}
	return detectedOs
}

export const detectMetaKeyChar = (platform: string) => {
	if (platform.match(platforms.mac)) {
		return "CMD"
	} else if (platform.match(platforms.windows)) {
		return "Win"
	} else if (platform.match(platforms.linux)) {
		return "Alt"
	} else {
		return "CMD"
	}
}

const userAgent = navigator?.userAgent || ""

export const isChrome = userAgent.indexOf("Chrome") >= 0

export const isSafari = !isChrome && userAgent.indexOf("Safari") >= 0

/**
 * Checks if the platform is macOS or Linux
 * @param platform - Optional platform string (e.g., "darwin", "linux", "win32")
 *                    If not provided, will try to detect from navigator or extensionState
 * @returns true if platform is darwin (macOS) or linux
 */
export const isMacOSOrLinux = (platform?: string): boolean => {
	// If platform is provided, use it directly
	if (platform) {
		return platform === "darwin" || platform === "linux"
	}

	// Try to get platform from navigator (browser environment)
	if (typeof navigator !== "undefined" && navigator.platform) {
		const navPlatform = navigator.platform.toLowerCase()
		return navPlatform.includes("mac") || navPlatform.includes("linux") || navPlatform.includes("darwin")
	}

	// Fallback: assume non-Windows (safer default for web)
	// In VSCode extension context, this should be called with platform parameter
	return true
}
