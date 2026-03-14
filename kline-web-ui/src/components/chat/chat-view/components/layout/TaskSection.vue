<template>
  <TaskHeader
    :task="task"
    :tokens-in="apiMetrics.totalTokensIn"
    :tokens-out="apiMetrics.totalTokensOut"
    :cache-writes="apiMetrics.totalCacheWrites"
    :cache-reads="apiMetrics.totalCacheReads"
    :total-cost="apiMetrics.totalCost"
    :does-model-support-prompt-cache="selectedModelInfo.supportsPromptCache"
    :last-api-req-total-tokens="lastApiReqTotalTokens"
    :last-progress-message-text="lastProgressMessageText"
    @close="messageHandlers.handleTaskCloseButtonClick"
    @scroll-to-message="scrollBehavior.scrollToMessage"
    @send-message="messageHandlers.handleSendMessage"
  />
</template>

<script setup lang="ts">
import type { ClineMessage } from '@/shared/ExtensionMessage'
import TaskHeader from '@/components/chat/task-header/TaskHeader.vue'
import type { MessageHandlers, ScrollBehavior } from '../../types/chatTypes'

interface Props {
  task: ClineMessage
  apiMetrics: {
    totalTokensIn: number
    totalTokensOut: number
    totalCacheWrites?: number
    totalCacheReads?: number
    totalCost: number
  }
  lastApiReqTotalTokens?: number
  selectedModelInfo: {
    supportsPromptCache: boolean
    supportsImages: boolean
  }
  messageHandlers: MessageHandlers
  scrollBehavior: ScrollBehavior
  lastProgressMessageText?: string
}

defineProps<Props>()
</script>

