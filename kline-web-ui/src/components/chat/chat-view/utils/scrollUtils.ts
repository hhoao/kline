/**
 * Utility functions for scroll behavior and management
 */

import debounce from "debounce"
import type { Ref } from "vue"

/**
 * VirtuosoHandle type - using any for Vue compatibility since react-virtuoso is React-specific
 */
type VirtuosoHandle = any

/**
 * Create a debounced smooth scroll function
 */
export function createSmoothScrollToBottom(virtuosoRef: Ref<VirtuosoHandle | null> | { current: VirtuosoHandle | null }) {
	return debounce(
		() => {
			const ref = 'value' in virtuosoRef ? virtuosoRef.value : virtuosoRef.current
			ref?.scrollTo({
				top: Number.MAX_SAFE_INTEGER,
				behavior: "smooth",
			})
		},
		10,
		{ immediate: true },
	)
}

/**
 * Scroll to bottom with auto behavior
 */
export function scrollToBottomAuto(virtuosoRef: Ref<VirtuosoHandle | null> | { current: VirtuosoHandle | null }) {
	const ref = 'value' in virtuosoRef ? virtuosoRef.value : virtuosoRef.current
	ref?.scrollTo({
		top: Number.MAX_SAFE_INTEGER,
		behavior: "auto", // instant causes crash
	})
}

/**
 * Handle wheel events to detect user scroll
 */
export function createWheelHandler(
	scrollContainerRef: Ref<HTMLDivElement | null> | { current: HTMLDivElement | null },
	disableAutoScrollRef: Ref<boolean> | { current: boolean },
) {
	return (event: Event) => {
		const wheelEvent = event as WheelEvent
		if (wheelEvent.deltaY && wheelEvent.deltaY < 0) {
			const containerRef = 'value' in scrollContainerRef ? scrollContainerRef.value : scrollContainerRef.current
			if (containerRef?.contains(wheelEvent.target as Node)) {
				// user scrolled up
				if ('value' in disableAutoScrollRef) {
					disableAutoScrollRef.value = true
				} else {
					disableAutoScrollRef.current = true
				}
			}
		}
	}
}

/**
 * Constants for scroll behavior
 */
export const SCROLL_CONSTANTS = {
	AT_BOTTOM_THRESHOLD: 10,
	VIEWPORT_INCREASE_TOP: 3_000,
	VIEWPORT_INCREASE_BOTTOM: Number.MAX_SAFE_INTEGER,
	FOOTER_HEIGHT: 5,
} as const
