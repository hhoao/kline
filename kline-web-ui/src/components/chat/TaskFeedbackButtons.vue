<template>
  <div
    v-if="!isFromHistory && shouldShow"
    class="flex justify-end items-center"
    :style="style"
  >
    <div class="flex gap-0 opacity-50 transition-opacity hover:opacity-100">
      <div class="scale-[0.85]">
        <button
          class="flex justify-center items-center p-0 bg-transparent border-0 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
          :aria-label="'This was helpful'"
          :disabled="feedback !== null"
          :title="'This was helpful'"
          @click="handleFeedback('thumbs_up')"
        >
          <span class="text-[var(--vscode-descriptionForeground)]">
            <span
              :class="feedback === 'thumbs_up' ? 'i-codicon:thumbsup-filled' : 'i-codicon:thumbsup'"
              class="codicon"
            />
          </span>
        </button>
      </div>
      <div class="scale-[0.85]">
        <button
          class="flex justify-center items-center p-0 bg-transparent border-0 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
          :aria-label="'This wasn\'t helpful'"
          :disabled="feedback !== null && feedback !== 'thumbs_down'"
          :title="'This wasn\'t helpful'"
          @click="handleFeedback('thumbs_down')"
        >
          <span class="text-[var(--vscode-descriptionForeground)]">
            <span
              :class="feedback === 'thumbs_down' ? 'i-codicon:thumbsdown-filled' : 'i-codicon:thumbsdown'"
              class="codicon"
            />
          </span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { TaskFeedbackType } from '@/shared/WebviewMessage'
import { taskService } from '@/api/task'

interface Props {
  messageTs: number
  isFromHistory?: boolean
  style?: Record<string, any>
}

const props = withDefaults(defineProps<Props>(), {
  isFromHistory: false,
})

const feedback = ref<TaskFeedbackType | null>(null)
const shouldShow = ref<boolean>(true)

// Check localStorage on mount to see if feedback was already given for this message
onMounted(() => {
  try {
    const feedbackHistory = localStorage.getItem('taskFeedbackHistory') || '{}'
    const history = JSON.parse(feedbackHistory)
    // Check if this specific message timestamp has received feedback
    if (history[props.messageTs]) {
      shouldShow.value = false
    }
  } catch (e) {
    console.error('Error checking feedback history:', e)
  }
})

const handleFeedback = async (type: TaskFeedbackType) => {
  if (feedback.value !== null) {
    return // Already provided feedback
  }

  feedback.value = type

  try {
    await taskService.taskFeedback(
      type
    )

    // Store in localStorage that feedback was provided for this message
    try {
      const feedbackHistory = localStorage.getItem('taskFeedbackHistory') || '{}'
      const history = JSON.parse(feedbackHistory)
      history[props.messageTs] = true
      localStorage.setItem('taskFeedbackHistory', JSON.stringify(history))
    } catch (e) {
      console.error('Error updating feedback history:', e)
    }
  } catch (error) {
    console.error('Error sending task feedback:', error)
  }
}
</script>

