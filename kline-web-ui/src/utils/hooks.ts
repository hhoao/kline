import { onBeforeUnmount, onMounted, ref, watch, watchEffect } from "vue"
import { detectMetaKeyChar, detectOS, unknown } from "./platformUtils"

/**
 * Vue composable to detect meta key and OS based on platform
 * @param platform The platform string or a function that returns platform string
 * @returns A tuple containing [os, metaKeyChar]
 */
export const useMetaKeyDetection = (platform: string | (() => string)) => {
	const metaKeyChar = ref(unknown)
	const os = ref(unknown)

	watchEffect(() => {
		const currentPlatform = typeof platform === "function" ? platform() : platform
		const detectedMetaKeyChar = detectMetaKeyChar(currentPlatform)
		const detectedOs = detectOS(currentPlatform)
		metaKeyChar.value = detectedMetaKeyChar
		os.value = detectedOs
	})

	return [os, metaKeyChar] as const
}

interface UseShortcutOptions {
	disableTextInputs?: boolean
}

/**
 * Vue composable to handle keyboard shortcuts
 * @param shortcut The shortcut string (e.g., "Meta+P", "Control+C", or "g g" for sequence)
 * @param callback The callback function to execute when shortcut is triggered
 * @param options Configuration options
 */
export const useShortcut = (
	shortcut: string,
	callback: (...args: unknown[]) => void,
	options: UseShortcutOptions = { disableTextInputs: true }
) => {
	const callbackRef = ref(callback)
	const keyCombo = ref<string[]>([])

	// Update callback ref when callback changes
	watchEffect(() => {
		callbackRef.value = callback
	})

	const currentShortcut = ref(shortcut)
	const currentOptions = ref(options)

	const handleKeyDown = (event: KeyboardEvent) => {
		const isTextInput =
			event.target instanceof HTMLTextAreaElement ||
			(event.target instanceof HTMLInputElement && (!event.target.type || event.target.type === "text")) ||
			(event.target as HTMLElement).isContentEditable

		const modifierMap: { [key: string]: boolean } = {
			Control: event.ctrlKey,
			Alt: event.altKey,
			Meta: event.metaKey, // alias for Command
			Shift: event.shiftKey,
		}

		if (event.repeat) {
			return null
		}

		if (currentOptions.value.disableTextInputs && isTextInput) {
			return event.stopPropagation()
		}

		if (currentShortcut.value.includes("+")) {
			const keyArray = currentShortcut.value.split("+")

			if (Object.keys(modifierMap).includes(keyArray[0])) {
				const finalKey = keyArray.pop()
				if (!finalKey) {
					return
				}

				if (keyArray.every((k) => modifierMap[k]) && finalKey.toLowerCase() === event.key.toLowerCase()) {
					event.preventDefault()
					return callbackRef.value(event)
				}
			} else {
				if (keyArray[keyCombo.value.length] === event.key) {
					if (keyArray[keyArray.length - 1] === event.key && keyCombo.value.length === keyArray.length - 1) {
						callbackRef.value(event)
						keyCombo.value = []
						return
					}

					keyCombo.value = [...keyCombo.value, event.key]
					return
				}
				if (keyCombo.value.length > 0) {
					keyCombo.value = []
					return
				}
			}
		}

		if (currentShortcut.value === event.key) {
			return callbackRef.value(event)
		}
	}

	// Watch for changes in shortcut and options
	watch([() => shortcut, () => options], ([newShortcut, newOptions]) => {
		currentShortcut.value = newShortcut
		currentOptions.value = newOptions
		// Reset key combo when shortcut changes
		keyCombo.value = []
	}, { immediate: true })

	onMounted(() => {
		window.addEventListener("keydown", handleKeyDown)
	})

	onBeforeUnmount(() => {
		window.removeEventListener("keydown", handleKeyDown)
	})
}
