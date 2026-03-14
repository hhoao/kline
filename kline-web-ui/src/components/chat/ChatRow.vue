<template>
  <div
    ref="containerRef"
    class="pt-[10px] pr-[6px] pb-[10px] pl-[15px] relative hover:[&_.checkpoint-controls]:opacity-100"
  >
    <CheckpointControls
      :message-ts="message.ts"
    />
      <ChatRowContent
        :input-value="inputValue"
        :is-expanded="isExpanded"
        :is-last="isLast"
        :last-modified-message="lastModifiedMessage"
        :message="message"
        :on-cancel-command="onCancelCommand"
        :on-set-quote="onSetQuote"
        :on-toggle-expand="onToggleExpand"
        :send-message-from-chat-row="sendMessageFromChatRow"
      />
      <div
        class="flex right-0 bottom-0 gap-2 mt-2"
        :style="{ opacity: askButtonsOpacity }"
        v-if="showAskActionButtons"
      >
        <button
          v-if="askButtonConfig.primaryText && askButtonConfig.primaryAction"
          type="button"
          class="px-2.5 py-1.5 rounded-[2px] text-[13px] font-inherit cursor-pointer border-0 bg-[var(--vscode-button-background)] text-[var(--vscode-button-foreground)] hover:bg-[var(--vscode-button-hoverBackground)] disabled:opacity-50 disabled:cursor-not-allowed"
          :class="askButtonConfig.secondaryText ? 'flex-1' : 'flex-[2]'"
          :disabled="!askCanInteract"
          @click="handleAskActionClick(askButtonConfig.primaryAction!)"
        >
          {{ askButtonConfig.primaryText }}
        </button>
        <button
          v-if="askButtonConfig.secondaryText && askButtonConfig.secondaryAction"
          type="button"
          class="px-2.5 py-1.5 rounded-[2px] text-[13px] font-inherit cursor-pointer border-0 bg-[var(--vscode-button-secondaryBackground)] text-[var(--vscode-button-secondaryForeground)] hover:bg-[var(--vscode-button-secondaryHoverBackground)] disabled:opacity-50 disabled:cursor-not-allowed flex-1"
          :disabled="!askCanInteract"
          @click="handleAskActionClick(askButtonConfig.secondaryAction!)"
        >
          {{ askButtonConfig.secondaryText }}
        </button>
      </div>
    </div>
</template>

<script setup lang="ts">
import type { ButtonActionType } from '@/components/chat/chat-view/shared/buttonConfig'
import { getButtonConfig } from '@/components/chat/chat-view/shared/buttonConfig'
import type { MessageHandlers } from '@/components/chat/chat-view/types/chatTypes'
import CheckpointControls from '@/components/common/CheckpointControls.vue'
import ChatRowContent from './ChatRowContent.vue'
import { useChatStateStore } from '@/stores/chatState'
import { onBeforeUnmount, onMounted, computed, ref, watch } from 'vue'
import type { ClineMessage } from '@/shared/ExtensionMessage'

interface Props {
  message: ClineMessage
  isExpanded: boolean
  onToggleExpand: (ts: number) => void
  lastModifiedMessage?: ClineMessage
  isLast: boolean
  onHeightChange: (isTaller: boolean) => void
  inputValue?: string
  messageHandlers: MessageHandlers
  mode: string
  sendMessageFromChatRow?: (text: string, images: string[], files: string[]) => void
  onSetQuote: (text: string) => void
  onCancelCommand?: () => void
}

const props = defineProps<Props>()
const chatStateStore = useChatStateStore()

const containerRef = ref<HTMLDivElement | null>(null)
const prevHeightRef = ref(0)
const isAskProcessing = ref(false)

const askButtonConfig = computed(() => {
  if (props.message.type !== 'ask' || !props.isLast || !props.messageHandlers || props.mode == null) {
    return { sendingDisabled: false, enableButtons: false, primaryText: undefined as string | undefined, secondaryText: undefined as string | undefined, primaryAction: undefined as ButtonActionType | undefined, secondaryAction: undefined as ButtonActionType | undefined }
  }
  return getButtonConfig(props.message, props.mode as 'act' | 'plan')
})

const showAskActionButtons = computed(() => {
  const c = askButtonConfig.value
  return !!(c.primaryText && c.primaryAction) || !!(c.secondaryText && c.secondaryAction)
})

const askCanInteract = computed(() => askButtonConfig.value.enableButtons && !isAskProcessing.value)
const askButtonsOpacity = computed(() => (askCanInteract.value || props.message.partial === true) ? 1 : 0.5)

watch(askButtonConfig, () => {
  isAskProcessing.value = false
})

function handleAskActionClick(action: ButtonActionType) {
  if (!askCanInteract.value || !props.messageHandlers) return
  isAskProcessing.value = true
  props.messageHandlers.executeButtonAction(
    action,
    props.inputValue,
    chatStateStore.selectedImages,
    chatStateStore.selectedFiles
  )
}

let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  if (containerRef.value) {
    resizeObserver = new ResizeObserver(() => {
      // Only track height changes for the last message (similar to React version)
      // This is used for partials command output etc.
      // NOTE: it's important we don't distinguish between partial or complete here
      // since our scroll effects in chatview need to handle height change during partial -> complete
      if (containerRef.value && props.isLast) {
        const height = containerRef.value.offsetHeight
        const isInitialRender = prevHeightRef.value === 0 // prevents scrolling when new element is added since we already scroll for that
        // height starts off at Infinity in React's useSize, but offsetHeight should never be Infinity
        if (height !== 0 && height !== Infinity && height !== prevHeightRef.value) {
          if (!isInitialRender) {
            props.onHeightChange(height > prevHeightRef.value)
          }
          prevHeightRef.value = height
        }
      }
    })
    resizeObserver.observe(containerRef.value)
  }
})

onBeforeUnmount(() => {
  if (resizeObserver) {
    resizeObserver.disconnect()
  }
})

// Watch for message changes to reset height tracking if needed
// This is similar to React's deepEqual comparison in memo
watch(
  () => props.message,
  () => {
    // Reset height tracking when message changes significantly
    // This prevents incorrect height comparisons when message content updates
    if (containerRef.value) {
      prevHeightRef.value = 0
    }
  },
  { deep: true }
)
</script>

