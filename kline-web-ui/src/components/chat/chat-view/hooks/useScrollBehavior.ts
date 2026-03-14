import { ClineMessage } from "@/shared/ExtensionMessage"
import debounce from "debounce"
import { onBeforeUnmount, onMounted, ref, watch, type Ref } from "vue"
import { ScrollBehavior } from "../types/chatTypes"

/**
 * Vue composable for managing scroll behavior
 * Handles auto-scrolling, manual scrolling, and scroll-to-message functionality
 */
export function useScrollBehavior(
	messages: Ref<ClineMessage[]> | ClineMessage[],
	visibleMessages: Ref<ClineMessage[]> | ClineMessage[],
	groupedMessages: Ref<(ClineMessage | ClineMessage[])[]> | (ClineMessage | ClineMessage[])[],
	expandedRows: Ref<Record<number, boolean>> | Record<number, boolean>,
	setExpandedRows: (value: Record<number, boolean>) => void,
): ScrollBehavior & {
	showScrollToBottom: Ref<boolean>
	setShowScrollToBottom: (value: boolean | ((prev: boolean) => boolean)) => void
	isAtBottom: Ref<boolean>
	setIsAtBottom: (value: boolean | ((prev: boolean) => boolean)) => void
	pendingScrollToMessage: Ref<number | null>
	setPendingScrollToMessage: (value: number | null | ((prev: number | null) => number | null)) => void
} {
	// Normalize inputs to refs for consistent access
	const messagesRef = Array.isArray(messages) ? ref(messages) : messages
	const visibleMessagesRef = Array.isArray(visibleMessages) ? ref(visibleMessages) : visibleMessages
	const groupedMessagesRef = Array.isArray(groupedMessages) ? ref(groupedMessages) : groupedMessages
	const expandedRowsRef = typeof expandedRows === 'object' && 'value' in expandedRows ? expandedRows : ref(expandedRows)

	// Refs
	const virtuosoRef = ref<any>(null) // VirtuosoHandle from react-virtuoso, using any for Vue compatibility
	const scrollContainerRef = ref<HTMLDivElement | null>(null)
	const disableAutoScrollRef = ref(false)

	// State
	const showScrollToBottom = ref(false)
	const isAtBottom = ref(false)
	const pendingScrollToMessage = ref<number | null>(null)

	// Debounced smooth scroll to bottom
	const scrollToBottomSmooth = debounce(
		() => {
			virtuosoRef.value?.scrollTo({
				top: Number.MAX_SAFE_INTEGER,
				behavior: "smooth",
			})
		},
		10,
		{ immediate: true },
	)

	// Smooth scroll to bottom with debounce
	const scrollToBottomAuto = () => {
		virtuosoRef.value?.scrollTo({
			top: Number.MAX_SAFE_INTEGER,
			behavior: "auto", // instant causes crash
		})
	}

	const scrollToMessage = (messageIndex: number) => {
		pendingScrollToMessage.value = messageIndex

		const targetMessage = messagesRef.value[messageIndex]
		if (!targetMessage) {
			pendingScrollToMessage.value = null
			return
		}

		const visibleIndex = visibleMessagesRef.value.findIndex((msg) => msg.ts === targetMessage.ts)
		if (visibleIndex === -1) {
			pendingScrollToMessage.value = null
			return
		}

		let groupIndex = -1

		for (let i = 0; i < groupedMessagesRef.value.length; i++) {
			const group = groupedMessagesRef.value[i]
			if (Array.isArray(group)) {
				const messageInGroup = group.some((msg) => msg.ts === targetMessage.ts)
				if (messageInGroup) {
					groupIndex = i
					break
				}
			} else {
				if (group.ts === targetMessage.ts) {
					groupIndex = i
					break
				}
			}
		}

		if (groupIndex !== -1) {
			pendingScrollToMessage.value = null
			disableAutoScrollRef.value = true
			requestAnimationFrame(() => {
				requestAnimationFrame(() => {
					virtuosoRef.value?.scrollToIndex({
						index: groupIndex,
						align: "start",
						behavior: "smooth",
					})
				})
			})
		}
	}

	// scroll when user toggles certain rows
	const toggleRowExpansion = (ts: number) => {
		const isCollapsing = expandedRowsRef.value[ts] ?? false
		const lastGroup = groupedMessagesRef.value.length > 0 
			? groupedMessagesRef.value[groupedMessagesRef.value.length - 1] 
			: undefined
		const isLast = Array.isArray(lastGroup) ? lastGroup[0]?.ts === ts : lastGroup?.ts === ts
		const secondToLastGroup = groupedMessagesRef.value.length > 1
			? groupedMessagesRef.value[groupedMessagesRef.value.length - 2]
			: undefined
		const isSecondToLast = Array.isArray(secondToLastGroup)
			? secondToLastGroup[0]?.ts === ts
			: secondToLastGroup?.ts === ts

		const isLastCollapsedApiReq =
			isLast &&
			!Array.isArray(lastGroup) && // Make sure it's not a browser session group
			lastGroup?.say === "api_req_started" &&
			!expandedRowsRef.value[lastGroup.ts]

		setExpandedRows({
			...expandedRowsRef.value,
			[ts]: !expandedRowsRef.value[ts],
		})

		// disable auto scroll when user expands row
		if (!isCollapsing) {
			disableAutoScrollRef.value = true
		}

		if (isCollapsing && isAtBottom.value) {
			setTimeout(() => {
				scrollToBottomAuto()
			}, 0)
		} else if (isLast || isSecondToLast) {
			if (isCollapsing) {
				if (isSecondToLast && !isLastCollapsedApiReq) {
					return
				}
				setTimeout(() => {
					scrollToBottomAuto()
				}, 0)
			} else {
				setTimeout(() => {
					virtuosoRef.value?.scrollToIndex({
						index: groupedMessagesRef.value.length - (isLast ? 1 : 2),
						align: "start",
					})
				}, 0)
			}
		}
	}

	const handleRowHeightChange = (isTaller: boolean) => {
		if (!disableAutoScrollRef.value) {
			if (isTaller) {
				scrollToBottomSmooth()
			} else {
				setTimeout(() => {
					scrollToBottomAuto()
				}, 0)
			}
		}
	}

	// Watch for grouped messages length changes to auto-scroll
	watch(
		() => groupedMessagesRef.value.length,
		() => {
			if (!disableAutoScrollRef.value) {
				setTimeout(() => {
					scrollToBottomSmooth()
				}, 50)
			}
		}
	)

	// Watch for pending scroll to message
	watch(
		() => pendingScrollToMessage.value,
		(value) => {
			if (value !== null) {
				scrollToMessage(value)
			}
		}
	)

	// Watch for messages length to hide scroll to bottom button
	watch(
		() => messagesRef.value.length,
		() => {
			if (!messagesRef.value?.length) {
				showScrollToBottom.value = false
			}
		}
	)

	// Handle wheel event for detecting user scroll
	const handleWheel = (event: Event) => {
		const wheelEvent = event as WheelEvent
		if (wheelEvent.deltaY && wheelEvent.deltaY < 0) {
			if (scrollContainerRef.value?.contains(wheelEvent.target as Node)) {
				// user scrolled up
				disableAutoScrollRef.value = true
			}
		}
	}

	onMounted(() => {
		window.addEventListener("wheel", handleWheel, { passive: true })
	})

	onBeforeUnmount(() => {
		window.removeEventListener("wheel", handleWheel)
		scrollToBottomSmooth.clear?.() // Clean up debounce
	})

	return {
		virtuosoRef,
		scrollContainerRef,
		disableAutoScrollRef,
		scrollToBottomSmooth,
		scrollToBottomAuto,
		scrollToMessage,
		toggleRowExpansion,
		handleRowHeightChange,
		showScrollToBottom: showScrollToBottom as any, // Vue ref, compatible with boolean usage
		setShowScrollToBottom: (value: boolean | ((prev: boolean) => boolean)) => {
			showScrollToBottom.value = typeof value === 'function' ? value(showScrollToBottom.value) : value
		},
		isAtBottom: isAtBottom as any, // Vue ref, compatible with boolean usage
		setIsAtBottom: (value: boolean | ((prev: boolean) => boolean)) => {
			isAtBottom.value = typeof value === 'function' ? value(isAtBottom.value) : value
		},
		pendingScrollToMessage: pendingScrollToMessage as any, // Vue ref, compatible with number | null usage
		setPendingScrollToMessage: (value: number | null | ((prev: number | null) => number | null)) => {
			pendingScrollToMessage.value = typeof value === 'function' ? value(pendingScrollToMessage.value) : value
		},
	} as ScrollBehavior & {
		showScrollToBottom: Ref<boolean>
		setShowScrollToBottom: (value: boolean | ((prev: boolean) => boolean)) => void
		isAtBottom: Ref<boolean>
		setIsAtBottom: (value: boolean | ((prev: boolean) => boolean)) => void
		pendingScrollToMessage: Ref<number | null>
		setPendingScrollToMessage: (value: number | null | ((prev: number | null) => number | null)) => void
	}
}
