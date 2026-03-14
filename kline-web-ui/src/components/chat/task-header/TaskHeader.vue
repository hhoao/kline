<template>
  <div class="flex flex-col gap-1.5 p-2">
    <!-- Display Checkpoint Error -->
    <CheckpointError
      :checkpoint-manager-error-message="checkpointManagerErrorMessage"
      @checkpoint-settings-click="handleCheckpointSettingsClick"
    />
    <!-- Task Header -->
    <div
      :class="[
        'relative overflow-hidden cursor-pointer rounded-sm flex flex-col gap-1.5 z-10 pt-2 pb-2 px-2 hover:opacity-100 bg-[var(--vscode-toolbar-hoverBackground)]/65 border',
        {
          'opacity-100': isTaskExpanded,
          'hover:bg-[var(--vscode-toolbar-hoverBackground)]': !isTaskExpanded,
        },
      ]"
      :style="{ borderColor: environmentBorderColor }"
    >
      <!-- Task Title -->
      <div class="flex justify-between items-center cursor-pointer" @click="toggleTaskExpanded">
        <div class="flex justify-between items-center">
          <svg
            v-if="isTaskExpanded"
            xmlns="http://www.w3.org/2000/svg"
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <polyline points="6 9 12 15 18 9" />
          </svg>
          <svg
            v-else
            xmlns="http://www.w3.org/2000/svg"
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <polyline points="9 18 15 12 9 6" />
          </svg>
          <div v-if="isTaskExpanded" class="flex flex-wrap justify-end mt-1 max-h-3 opacity-80 cursor-pointer">
            <CopyTaskButton :class="BUTTON_CLASS" :task-text="task.text" />
            <DeleteTaskButton
              :class="BUTTON_CLASS"
              :task-id="currentTaskItem?.id"
              :task-size="currentTaskItem?.size"
            />
            <!-- Only visible in development mode -->
            <OpenDiskConversationHistoryButton
              :class="BUTTON_CLASS"
              :task-id="currentTaskItem?.id"
            />
          </div>
        </div>
        <div class="flex flex-grow gap-1 justify-between items-center min-w-0 select-none">
          <div v-if="!isTaskExpanded" class="overflow-hidden flex-grow min-w-0 text-sm whitespace-nowrap text-ellipsis">
            <span class="ph-no-capture">
              <component :is="highlightedTextCollapsed" />
            </span>
          </div>
        </div>
        <div class="inline-flex flex-shrink-0 justify-end items-center select-none">
          <div
            v-if="isCostAvailable"
            class="mr-1 px-1 py-0.25 rounded-full inline-flex shrink-0 text-[var(--vscode-badge-foreground)] bg-[var(--vscode-badge-background)]/80 items-center"
            id="price-tag"
          >
            <span class="text-xs">${{ totalCost?.toFixed(4) }}</span>
          </div>
          <NewTaskButton :class="BUTTON_CLASS" @click="onClose" />
        </div>
      </div>

      <!-- Expand/Collapse Task Details -->
      <div v-if="isTaskExpanded" class="flex flex-col break-words" :key="`task-details-${currentTaskItem?.id}`">
        <div
          ref="highlightedTextRef"
          :class="[
            'ph-no-capture whitespace-pre-wrap break-words px-0.5 text-sm cursor-pointer mt-1 relative',
            {
              'max-h-[25vh] overflow-y-auto scroll-smooth': isHighlightedTextExpanded,
              'max-h-[4.5rem] overflow-hidden': !isHighlightedTextExpanded && highlight.displayTextExpandable,
            },
          ]"
          :style="
            !isHighlightedTextExpanded && highlight.displayTextExpandable
              ? {
                  WebkitMaskImage: 'linear-gradient(to bottom, black 60%, transparent 100%)',
                  maskImage: 'linear-gradient(to bottom, black 60%, transparent 100%)',
                }
              : undefined
          "
          @click="highlight.displayTextExpandable && (isHighlightedTextExpanded = true)"
        >
          <component :is="highlightedTextExpanded" />
        </div>

        <Thumbnails
          v-if="(task.images && task.images.length > 0) || (task.files && task.files.length > 0)"
          :files="task.files ?? []"
          :images="task.images ?? []"
        />

        <ContextWindow
          :cache-reads="cacheReads"
          :cache-writes="cacheWrites"
          :context-window="100000"
          :last-api-req-total-tokens="lastApiReqTotalTokens"
          :tokens-in="tokensIn"
          :tokens-out="tokensOut"
          :use-auto-condense="false"
          @send-message="onSendMessage"
        />

        <TaskTimeline :messages="clineMessages" @block-click="onScrollToMessage" />
      </div>
    </div>

    <!-- Display Focus Chain To-Do List -->
    <FocusChain
      :current-task-item-id="currentTaskItem?.id"
      :last-progress-message-text="lastProgressMessageText"
    />
  </div>
</template>

<script setup lang="ts">
import { uiService } from '@/api/ui'
import Thumbnails from '@/components/common/Thumbnails.vue'
import type { ClineMessage } from '@/shared/ExtensionMessage'
import { getEnvironmentColor } from '@/utils/environmentColors'
import { useExtensionStateStore } from "@/stores/extensionState"
import { computed, h, onBeforeUnmount, ref, watch } from 'vue'
import CopyTaskButton from './buttons/CopyTaskButton.vue'
import DeleteTaskButton from './buttons/DeleteTaskButton.vue'
import NewTaskButton from './buttons/NewTaskButton.vue'
import OpenDiskConversationHistoryButton from './buttons/OpenDiskConversationHistoryButton.vue'
import CheckpointError from './CheckpointError.vue'
import ContextWindow from './ContextWindow.vue'
import FocusChain from './FocusChain.vue'
import { highlightText } from './Highlights'
import TaskTimeline from './TaskTimeline.vue'


interface Props {
  task: ClineMessage
  tokensIn: number
  tokensOut: number
  doesModelSupportPromptCache: boolean
  cacheWrites?: number
  cacheReads?: number
  totalCost: number
  lastApiReqTotalTokens?: number
  lastProgressMessageText?: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  close: []
  scrollToMessage: [messageIndex: number]
  sendMessage: [command: string, files: string[], images: string[]]
}>()

const BUTTON_CLASS = 'max-h-3 border-0 font-bold bg-transparent hover:opacity-100 text-[var(--vscode-foreground)]'

const extensionState = computed(() => useExtensionStateStore().extensionState)
const apiConfiguration = computed(() => extensionState.value?.apiConfiguration)
const currentTaskItem = computed(() => extensionState.value?.currentTaskItem)
const checkpointManagerErrorMessage = computed(() => extensionState.value?.checkpointManagerErrorMessage)
const clineMessages = computed(() => extensionState.value?.clineMessages || [])
const mode = computed(() => extensionState.value?.mode || 'plan-act')

const isTaskExpanded = ref(false)
const isHighlightedTextExpanded = ref(false)
const highlightedTextRef = ref<HTMLDivElement | null>(null)

watch(
  () => false,
  (newVal) => {
    if (newVal !== undefined) {
      isTaskExpanded.value = newVal
    }
  }
)

const highlight = computed(() => {
  const taskTextLines = props.task.text?.split('\n') || []
  const highlighted = highlightText(props.task.text, false)

  return { highlightedText: highlighted, displayTextExpandable: taskTextLines.length > 3 }
})

const highlightedTextCollapsed = computed(() => {
  const result = highlightText(props.task.text, false)
  if (typeof result === 'string') {
    return () => h('span', result)
  }
  return () => result
})

const highlightedTextExpanded = computed(() => {
  const result = highlight.value.highlightedText
  if (typeof result === 'string') {
    return () => h('span', result)
  }
  return () => result
})

const modeFields = computed(() => {
  return {
    apiProvider: 'openai',
    openAiModelInfo: {
      inputPrice: 0.000001,
      outputPrice: 0.000002,
    },
  }
})

const isCostAvailable = computed(() => {
  return (
    (props.totalCost &&
      modeFields.value.apiProvider === 'openai' &&
      modeFields.value.openAiModelInfo?.inputPrice &&
      modeFields.value.openAiModelInfo?.outputPrice) ||
    (modeFields.value.apiProvider !== 'vscode-lm' &&
      modeFields.value.apiProvider !== 'ollama' &&
      modeFields.value.apiProvider !== 'lmstudio')
  )
})

const toggleTaskExpanded = () => {
  isTaskExpanded.value = !isTaskExpanded.value
  // Update store if needed
  if (extensionState.value) {
    // Note: This might need to be updated based on your store implementation
    // extensionState.value.expandTaskHeader = isTaskExpanded.value
  }
}

const handleCheckpointSettingsClick = () => {
  // navigateToSettings() - This might need to be implemented based on your router setup
  setTimeout(async () => {
    try {
      await uiService.scrollToSettings('features')
    } catch (error) {
      console.error('Error scrolling to checkpoint settings:', error)
    }
  }, 300)
}

const environmentBorderColor = computed(() => {
  return getEnvironmentColor(import.meta.env.MODE, 'border')
})

const handleClickOutside = (event: MouseEvent) => {
  if (highlightedTextRef.value && !highlightedTextRef.value.contains(event.target as Node)) {
    isHighlightedTextExpanded.value = false
  }
}

watch(isHighlightedTextExpanded, (newVal) => {
  if (newVal) {
    document.addEventListener('mousedown', handleClickOutside)
  } else {
    document.removeEventListener('mousedown', handleClickOutside)
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('mousedown', handleClickOutside)
})

const onClose = () => {
  emit('close')
}

const onScrollToMessage = (messageIndex: number) => {
  emit('scrollToMessage', messageIndex)
}

const onSendMessage = (command: string, files: string[], images: string[]) => {
  emit('sendMessage', command, files, images)
}
</script>

