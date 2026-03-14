<template>
  <ChatLayout :is-hidden="isHidden">
    <!-- min-h-0 + flex-1 确保消息区域能在内部滚动，而不是把整个布局撑高 -->
    <div class="flex overflow-hidden flex-col flex-1 min-h-0">
      <Navbar v-if="showNavbar" />
      <TaskSection
        v-if="task"
        :api-metrics="apiMetrics"
        :last-api-req-total-tokens="lastApiReqTotalTokens"
        :last-progress-message-text="lastProgressMessageText"
        :message-handlers="messageHandlers"
        :scroll-behavior="scrollBehavior"
        :selected-model-info="{
          supportsPromptCache: selectedModelInfo.supportsPromptCache,
          supportsImages: selectedModelInfo.supportsImages || false,
        }"
        :task="task"
      />
      <WelcomeSection
        v-else
        :hide-announcement="() => false"
        :should-show-quick-wins="shouldShowQuickWins || true"
        :show-announcement="showAnnouncement || true"
        :show-history-view="showHistoryView || (() => true)"
        :task-history="taskHistory || []"
        :telemetry-setting="telemetrySetting || 'enabled'"
        :version="version || '1.0.0'"
      />
      <MessagesArea
        v-if="task"
        :grouped-messages="groupedMessages"
        :message-handlers="messageHandlers"
        :mode="mode"
        :modified-messages="modifiedMessages"
        :scroll-behavior="scrollBehavior"
        :task="task"
        :text-area-ref="textAreaRef as any"
      />
    </div>
    <footer class="bg-[var(--vscode-sidebar-background)]" style="grid-row: 2">
      <AutoApproveBar />
      <ActionButtons
        :message-handlers="messageHandlers"
        :messages="messages"
        :mode="mode"
        :scroll-behavior="{
          scrollToBottomSmooth: scrollBehavior.scrollToBottomSmooth,
          disableAutoScrollRef: scrollBehavior.disableAutoScrollRef as any,
          showScrollToBottom: typeof scrollBehavior.showScrollToBottom === 'object' && 'value' in scrollBehavior.showScrollToBottom ? scrollBehavior.showScrollToBottom.value : scrollBehavior.showScrollToBottom,
          virtuosoRef: scrollBehavior.virtuosoRef as any,
        }"
        :task="task"
      />
      <InputSection
        :message-handlers="messageHandlers"
        :placeholder-text="placeholderText"
        :scroll-behavior="scrollBehavior"
        :select-files-and-images="selectFilesAndImages"
        :should-disable-files-and-images="shouldDisableFilesAndImages"
      />
    </footer>
  </ChatLayout>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch, watchEffect } from 'vue'
import { findLast } from '@/shared/array'
import { combineApiRequests } from '@/shared/combineApiRequests'
import { combineCommandSequences } from '@/shared/combineCommandSequences'
import type { ClineApiReqInfo, ClineMessage } from '@/shared/ExtensionMessage'
import { getApiMetrics } from '@/shared/getApiMetrics'
import { fileService } from '@/api/file'

const normalizeApiConfiguration = (_apiConfiguration: any, _mode: string) => {
  return {
    supportsPromptCache: false,
    supportsImages: false,
  }
}
import { useExtensionStateStore } from '@/stores/extensionState'
import {
  ActionButtons,
  CHAT_CONSTANTS,
  ChatLayout,
  convertHtmlToMarkdown,
  filterVisibleMessages,
  groupMessages,
  InputSection,
  MessagesArea,
  TaskSection,
  WelcomeSection,
} from './chat-view'
import Navbar from '../menu/Navbar.vue'
import AutoApproveBar from './auto-approve-menu/AutoApproveBar.vue'
import { useScrollBehavior, useMessageHandlers } from './chat-view'
import { useChatStateStore } from '@/stores/chatState'

interface Props {
  isHidden?: boolean
  showAnnouncement?: boolean
  hideAnnouncement?: () => void
  showHistoryView?: () => void
}

const props = defineProps<Props>()

const MAX_IMAGES_AND_FILES_PER_MESSAGE = CHAT_CONSTANTS.MAX_IMAGES_AND_FILES_PER_MESSAGE
const QUICK_WINS_HISTORY_THRESHOLD = 3

const extensionStateStore = useExtensionStateStore()
const extensionState = computed(() => extensionStateStore.extensionState)

// Extension state values
const version = computed(() => extensionState.value?.version || '')
const messages = computed(() => extensionState.value?.clineMessages || [])
const taskHistory = computed(() => extensionState.value?.taskHistory || [])
const apiConfiguration = computed(() => extensionState.value?.apiConfiguration)
const telemetrySetting = computed(() => extensionState.value?.telemetrySetting || 'enabled')
const mode = computed(() => extensionState.value?.mode || 'plan')
const userInfo = computed(() => extensionState.value?.userInfo)
const currentFocusChainChecklist = computed(() => extensionState.value?.currentFocusChainChecklist)

// Show navbar - based on platform detection
// In VSCode extension, show navbar; in web, hide it
const showNavbar = computed(() => {
  const platform = extensionState.value?.platform
  // If platform is an object (VSCode extension), show navbar
  // If platform is a string like 'web', hide navbar
  if (typeof platform === 'object' && platform !== null) {
    return true // VSCode extension
  }
  return true // Web or other platforms
})

const isProdHostedApp = computed(() => userInfo.value?.apiBaseUrl === 'https://app.cline.bot')
const shouldShowQuickWins = computed(
  () => isProdHostedApp.value && (!taskHistory.value || taskHistory.value.length < QUICK_WINS_HISTORY_THRESHOLD)
)

// Computed values
const task = computed(() => messages.value.length > 0 ? messages.value[0] : undefined)
const modifiedMessages = computed(() => combineApiRequests(combineCommandSequences(messages.value.slice(1))))
const apiMetrics = computed(() => getApiMetrics(modifiedMessages.value))

const lastApiReqTotalTokens = computed(() => {
  const getTotalTokensFromApiReqMessage = (msg: ClineMessage): number => {
    if (!msg.text) {
      return 0
    }
    const { tokensIn, tokensOut, cacheWrites, cacheReads }: ClineApiReqInfo = JSON.parse(msg.text)
    return (tokensIn || 0) + (tokensOut || 0) + (cacheWrites || 0) + (cacheReads || 0)
  }
  const lastApiReqMessage = findLast(modifiedMessages.value, (msg) => {
    if (msg.say !== 'api_req_started') {
      return false
    }
    return getTotalTokensFromApiReqMessage(msg) > 0
  })
  if (!lastApiReqMessage) {
    return undefined
  }
  return getTotalTokensFromApiReqMessage(lastApiReqMessage)
})

// Use Pinia store directly for chat state
const chatStateStore = useChatStateStore()

// Create textAreaRef for component-level DOM reference
const textAreaRef = ref<HTMLTextAreaElement | null>(null)

const visibleMessages = computed(() => filterVisibleMessages(modifiedMessages.value))
const groupedMessages = computed(() => groupMessages(visibleMessages.value))

// Use the converted useScrollBehavior composable
const scrollBehavior = useScrollBehavior(
  messages,
  visibleMessages,
  groupedMessages,
  computed(() => chatStateStore.expandedRows),
  (value: Record<number, boolean>) => {
    chatStateStore.setExpandedRows(value)
  }
)

// Use the converted useMessageHandlers composable
const messageHandlers = useMessageHandlers(messages, scrollBehavior)

const selectedModelInfo = computed(() => {
  if (!apiConfiguration.value || !mode.value) {
    return { supportsPromptCache: false, supportsImages: false }
  }
  return normalizeApiConfiguration(apiConfiguration.value, mode.value)
})

const selectFilesAndImages = async () => {
  try {
    const response = await fileService.selectFiles(selectedModelInfo.value.supportsImages)
    
    // Note: fileService.selectFiles returns string[], but TSX version expects { values1, values2 }
    // For now, treat all as images. If API returns separate images/files, adjust accordingly
    if (response && response.length > 0) {
      const currentTotal = chatStateStore.selectedImages.length + chatStateStore.selectedFiles.length
      const availableSlots = MAX_IMAGES_AND_FILES_PER_MESSAGE - currentTotal

      if (availableSlots > 0) {
        // Prioritize images first (if API returns separate arrays, use response[0] for images)
        const imagesToAdd = Math.min(response.length, availableSlots)
        if (imagesToAdd > 0) {
          chatStateStore.setSelectedImages([...chatStateStore.selectedImages, ...response.slice(0, imagesToAdd)])
        }

        // Use remaining slots for files (if API returns separate arrays, use response[1] for files)
        // const remainingSlots = availableSlots - imagesToAdd
        // if (remainingSlots > 0 && response.values2 && response.values2.length > 0) {
        //   chatStateStore.setSelectedFiles((prevFiles) => [...prevFiles, ...response.values2.slice(0, remainingSlots)])
        // }
      }
    }
  } catch (error) {
    console.error('Error selecting images & files:', error)
  }
}

const shouldDisableFilesAndImages = computed(
  () => chatStateStore.selectedImages.length + chatStateStore.selectedFiles.length >= MAX_IMAGES_AND_FILES_PER_MESSAGE
)

const lastProgressMessageText = computed(() => {
  // First check if we have a current focus chain list from the extension state
  if (currentFocusChainChecklist.value) {
    return currentFocusChainChecklist.value
  }

  // Fall back to the last task_progress message if no state focus chain list
  const lastProgressMessage = [...modifiedMessages.value].reverse().find((message) => message.say === 'task_progress')
  return lastProgressMessage?.text
})

const placeholderText = computed(() => {
  return task.value ? 'Type a message...' : 'Type your task here...'
})

// Copy handler
const handleCopy = async (e: ClipboardEvent) => {
    const targetElement = e.target as HTMLElement | null
    // If the copy event originated from an input or textarea,
    // let the default browser behavior handle it.
    if (
      targetElement &&
      (targetElement.tagName === 'INPUT' || targetElement.tagName === 'TEXTAREA' || targetElement.isContentEditable)
    ) {
      return
    }

    if (window.getSelection) {
      const selection = window.getSelection()
      if (selection && selection.rangeCount > 0) {
        const range = selection.getRangeAt(0)
        const commonAncestor = range.commonAncestorContainer
        let textToCopy: string | null = null

        // Check if the selection is inside an element where plain text copy is preferred
        let currentElement =
          commonAncestor.nodeType === Node.ELEMENT_NODE
            ? (commonAncestor as HTMLElement)
            : commonAncestor.parentElement
        let preferPlainTextCopy = false
        while (currentElement) {
          if (currentElement.tagName === 'PRE' && currentElement.querySelector('code')) {
            preferPlainTextCopy = true
            break
          }
          // Check computed white-space style
          const computedStyle = window.getComputedStyle(currentElement)
          if (
            computedStyle.whiteSpace === 'pre' ||
            computedStyle.whiteSpace === 'pre-wrap' ||
            computedStyle.whiteSpace === 'pre-line'
          ) {
            preferPlainTextCopy = true
            break
          }

          // Stop searching if we reach a known chat message boundary or body
          if (
            currentElement.classList.contains('chat-row-assistant-message-container') ||
            currentElement.classList.contains('chat-row-user-message-container') ||
            currentElement.tagName === 'BODY'
          ) {
            break
          }
          currentElement = currentElement.parentElement
        }

        if (preferPlainTextCopy) {
          // For code blocks or elements with pre-formatted white-space, get plain text.
          textToCopy = selection.toString()
        } else {
          // For other content, use the existing HTML-to-Markdown conversion
          const clonedSelection = range.cloneContents()
          const div = document.createElement('div')
          div.appendChild(clonedSelection)
          const selectedHtml = div.innerHTML
          textToCopy = await convertHtmlToMarkdown(selectedHtml)
        }

        if (textToCopy !== null) {
          try {
            await fileService.copyToClipboard(textToCopy)
            e.preventDefault()
          } catch (error) {
            console.error('Error copying to clipboard:', error)
          }
        }
      }
    }
}

// Focus chat input handler
const handleFocusChatInput = () => {
  // Only focus chat input box if user is currently viewing the chat (not hidden).
  if (!props.isHidden) {
    textAreaRef.value?.focus()
  }
}

onMounted(() => {
  document.addEventListener('copy', handleCopy)

  // Focus chat input on mount
  nextTick(() => {
    textAreaRef.value?.focus()
  })

  // Listen for local focusChatInput event
  window.addEventListener('focusChatInput', handleFocusChatInput as EventListener)

  // Set up addToInput subscription
  // TODO: Implement subscribeToAddToInput using SSE or appropriate streaming mechanism
  // Original TSX version used UiServiceClient.subscribeToAddToInput with gRPC streaming
  // Current implementation may need to use SSE or event-based system
  // For now, this is handled through the SSE subscription system in extensionStateStore
})

onBeforeUnmount(() => {
  document.removeEventListener('copy', handleCopy)
  window.removeEventListener('focusChatInput', handleFocusChatInput as EventListener)
})

// Watch for task changes to clear expanded rows
watch(
  () => task.value?.ts,
  () => {
    chatStateStore.clearExpandedRows()
  }
)

// Auto focus logic
watchEffect(() => {
  if (!props.isHidden && !chatStateStore.sendingDisabled && !chatStateStore.enableButtons) {
    const timer = setTimeout(() => {
      textAreaRef.value?.focus()
    }, 50)
    return () => clearTimeout(timer)
  }
})
</script>

