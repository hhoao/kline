<template>
  <div v-if="hasError" class="p-2.5 text-[var(--vscode-errorForeground)] overflow-auto max-w-[512px] border border-[var(--vscode-editorError-foreground)] rounded bg-[var(--vscode-inputValidation-errorBackground,rgba(255,0,0,0.1))]" :style="{ height: height || 'auto' }">
    <h3 class="m-0 mb-2">{{ errorTitle || 'Something went wrong displaying this content' }}</h3>
    <p class="m-0">{{ errorBody || `Error: ${error?.message || 'Unknown error'}` }}</p>
  </div>
  <slot v-else />
</template>

<script setup lang="ts">
import { onErrorCaptured, ref } from 'vue'

interface Props {
  errorTitle?: string
  errorBody?: string
  height?: string
}

const props = defineProps<Props>()

const hasError = ref(false)
const error = ref<Error | null>(null)

onErrorCaptured((err: Error) => {
  hasError.value = true
  error.value = err
  console.error('Error in ChatErrorBoundary:', err.message)
  return false // Prevent the error from propagating further
})
</script>


