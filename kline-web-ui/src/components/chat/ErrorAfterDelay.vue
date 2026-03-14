<template>
  <div
    class="absolute top-0 right-0 bg-[rgba(255,0,0,0.5)] text-[var(--vscode-errorForeground)] px-1.5 py-0.5 text-xs rounded-bl z-[100]"
  >
    Error in {{ tickCount }}/{{ numSecondsToWait }} seconds
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

interface Props {
  numSecondsToWait?: number
}

const props = withDefaults(defineProps<Props>(), {
  numSecondsToWait: 5,
})

const tickCount = ref(0)
let intervalID: NodeJS.Timeout | null = null

onMounted(() => {
  intervalID = setInterval(() => {
    if (tickCount.value >= props.numSecondsToWait) {
      if (intervalID) {
        clearInterval(intervalID)
      }
      // Error boundaries don't catch async code, so we throw synchronously
      throw new Error('This is an error for testing the error boundary')
    } else {
      tickCount.value++
    }
  }, 1000)
})

onBeforeUnmount(() => {
  if (intervalID) {
    clearInterval(intervalID)
  }
})
</script>


