<template>
  <div v-if="!task" />
  <div v-else class="flex px-[15px]" :style="{ opacity: opacity }">
    <!-- Scroll buttons -->
    <button v-if="showScrollToBottom || !hasButtons"
      :aria-label="showScrollToBottom ? 'Scroll to bottom' : 'Scroll to top'"
      class="text-lg text-[var(--vscode-primaryButton-foreground)] bg-[color-mix(in_srgb,var(--vscode-toolbar-hoverBackground)_55%,transparent)] rounded-[3px] overflow-hidden cursor-pointer flex justify-center items-center flex-1 h-[25px] hover:bg-[color-mix(in_srgb,var(--vscode-toolbar-hoverBackground)_90%,transparent)] active:bg-[color-mix(in_srgb,var(--vscode-toolbar-hoverBackground)_70%,transparent)] border-0"
      @click="showScrollToBottom ? handleScrollToBottom : handleScrollToTop" @keydown="handleScrollKeyDown">
      <span :class="showScrollToBottom ? 'i-codicon:chevron-down' : 'i-codicon:chevron-up'" />
    </button>
  </div>
</template>

<script setup lang="ts">
import type { ClineMessage } from '@/shared/ExtensionMessage'
import type { Mode } from '@/shared/storage/types'
import { useChatStateStore } from "@/stores/chatState"
import { computed, ref, watch } from 'vue'
import { getButtonConfig } from '../../shared/buttonConfig'
import type { MessageHandlers } from '../../types/chatTypes'

interface Props {
  task?: ClineMessage
  messages: ClineMessage[]
  messageHandlers: MessageHandlers
  mode: Mode
  scrollBehavior: {
    scrollToBottomSmooth: () => void
    disableAutoScrollRef: { value: boolean }
    showScrollToBottom: boolean
    virtuosoRef: { value: any }
  }
}

const props = defineProps<Props>()

// Use store directly
const chatStateStore = useChatStateStore()
const { setSendingDisabled } = chatStateStore
const isProcessing = ref(false)

const msg = computed(() => {
  const len = props.messages.length
  return len > 0 ? [props.messages[len - 1], props.messages[len - 2]] : [undefined, undefined]
})

const buttonConfig = computed(() => {
  return msg.value[0] ? getButtonConfig(msg.value[0], props.mode) : { sendingDisabled: false, enableButtons: false }
})

watch(buttonConfig, (config) => {
  setSendingDisabled(config.sendingDisabled)
  isProcessing.value = false
})

// Clear input when transitioning from command_output to api_req
watch(
  [() => msg.value[0]?.type, () => msg.value[0]?.say, () => msg.value[1]?.ask],
  ([lastType, lastSay, secondLastAsk]) => {
    if (lastType === 'say' && lastSay === 'api_req_started' && secondLastAsk === 'command_output') {
      chatStateStore.setInputValue('')
      chatStateStore.setSelectedImages([])
      chatStateStore.setSelectedFiles([])
    }
  }
)

const { showScrollToBottom, scrollToBottomSmooth, disableAutoScrollRef, virtuosoRef } =
  props.scrollBehavior

const primaryText = computed(() => buttonConfig.value.primaryText)
const secondaryText = computed(() => buttonConfig.value.secondaryText)
const primaryAction = computed(() => buttonConfig.value.primaryAction)
const secondaryAction = computed(() => buttonConfig.value.secondaryAction)
const enableButtons = computed(() => buttonConfig.value.enableButtons)
const hasButtons = computed(() => !!(primaryText.value && primaryAction.value) || !!(secondaryText.value && secondaryAction.value))
const isStreaming = computed(() => props.task?.partial === true)
const canInteract = computed(() => enableButtons.value && !isProcessing.value)

const opacity = computed(() => {
  return canInteract.value || isStreaming.value ? 1 : 0.5
})

const handleScrollToBottom = () => {
  scrollToBottomSmooth()
  disableAutoScrollRef.value = false
}

const handleScrollToTop = () => {
  if (virtuosoRef.value?.scrollTo) {
    virtuosoRef.value.scrollTo({
      top: 0,
      behavior: 'smooth',
    })
  }
  disableAutoScrollRef.value = true
}

const handleScrollKeyDown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' || e.key === ' ') {
    e.preventDefault()
    if (showScrollToBottom) {
      handleScrollToBottom()
    } else {
      handleScrollToTop()
    }
  }
}
</script>
