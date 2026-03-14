<template>
  <!-- Browser session group -->
  <BrowserSessionRow
    v-if="Array.isArray(messageOrGroup)"
    :expanded-rows="expandedRows"
    :is-last="index === groupedMessages.length - 1"
    :last-modified-message="lastModifiedMessage"
    :messages="messageOrGroup"
    @height-change="onHeightChange"
    @set-quote="onSetQuote"
    @toggle-expand="onToggleExpand"
  />

  <!-- Regular message -->
  <ChatRow
    v-else
    :input-value="inputValue"
    :is-expanded="expandedRows[messageOrGroup.ts] || false"
    :is-last="isLast"
    :last-modified-message="lastModifiedMessage"
    :message="messageOrGroup"
    :message-handlers="messageHandlers"
    :mode="mode"
    @cancel-command="handleCancelCommand"
    @height-change="onHeightChange"
    @set-quote="onSetQuote"
    @toggle-expand="onToggleExpand"
    @send-message="messageHandlers.handleSendMessage"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ClineMessage } from '@/shared/ExtensionMessage'
import BrowserSessionRow from '@/components/chat/BrowserSessionRow.vue'
import ChatRow from '@/components/chat/ChatRow.vue'
import type { MessageHandlers } from '../../types/chatTypes'

interface Props {
  index: number
  messageOrGroup: ClineMessage | ClineMessage[]
  groupedMessages: (ClineMessage | ClineMessage[])[]
  modifiedMessages: ClineMessage[]
  expandedRows: Record<number, boolean>
  inputValue: string
  messageHandlers: MessageHandlers
  mode: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  toggleExpand: [ts: number]
  heightChange: [isTaller: boolean]
  setQuote: [quote: string | null]
}>()

const onToggleExpand = (ts: number) => {
  emit('toggleExpand', ts)
}

const onHeightChange = (isTaller: boolean) => {
  emit('heightChange', isTaller)
}

const onSetQuote = (quote: string | null) => {
  emit('setQuote', quote)
}

const handleCancelCommand = () => {
  props.messageHandlers.executeButtonAction('cancel')
}

// Determine if this is the last message for status display purposes
const isLast = computed(() => {
  if (Array.isArray(props.messageOrGroup)) {
    return props.index === props.groupedMessages.length - 1
  }

  const nextMessage =
    props.index < props.groupedMessages.length - 1 && props.groupedMessages[props.index + 1]
  const isNextCheckpoint =
    !Array.isArray(nextMessage) && nextMessage && nextMessage?.say === 'checkpoint_created'
  const isLastMessageGroup = isNextCheckpoint && props.index === props.groupedMessages.length - 2

  return props.index === props.groupedMessages.length - 1 || isLastMessageGroup
})

const lastModifiedMessage = computed(() => {
  return props.modifiedMessages[props.modifiedMessages.length - 1]
})
</script>

