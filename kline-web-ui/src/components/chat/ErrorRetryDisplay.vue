<template>
  <div
    class="flex flex-col bg-[var(--vscode-textBlockQuote-background)] p-2 rounded-[3px] text-xs"
  >
    <div class="flex items-center mb-1">
      <i
        :class="isFailed ? 'i-codicon:warning' : 'i-codicon:sync'"
        class="mr-2 text-sm text-[var(--vscode-descriptionForeground)]"
      ></i>
      <span class="font-medium text-[var(--vscode-foreground)]">
        {{ isFailed ? 'Auto-Retry Failed' : 'Auto-Retry in Progress' }}
      </span>
    </div>
    <div class="text-[var(--vscode-foreground)] opacity-80">
      <template v-if="isFailed">
        Auto-retry failed after <strong>{{ maxAttempts }}</strong> attempts. Manual intervention
        required.
      </template>
      <template v-else>
        Attempt <strong>{{ attempt }}</strong> of <strong>{{ maxAttempts }}</strong> - Retrying in
        {{ delaySeconds }} seconds...
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ClineMessage } from '@/shared/ExtensionMessage'

interface Props {
  message: ClineMessage
}

const props = defineProps<Props>()

const retryInfo = computed(() => {
  try {
    return JSON.parse(props.message.text || '{}') as {
      attempt: number
      maxAttempts: number
      delaySeconds: number
      failed: boolean
    }
  } catch {
    return null
  }
})

const attempt = computed(() => retryInfo.value?.attempt ?? 0)
const maxAttempts = computed(() => retryInfo.value?.maxAttempts ?? 0)
const delaySeconds = computed(() => retryInfo.value?.delaySeconds ?? 0)
const isFailed = computed(() => retryInfo.value?.failed === true)
</script>


