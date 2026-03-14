<template>
  <button
    :class="[
      'z-[1] scale-90 px-2 py-1 rounded border cursor-pointer transition-all',
      'bg-[var(--vscode-button-background)] text-[var(--vscode-button-foreground)] border-[var(--vscode-button-border)]',
      'hover:bg-[var(--vscode-button-hoverBackground)]',
      className,
      copied ? 'i-codicon:check' : 'i-codicon:copy'
    ]"
    :aria-label="copied ? 'Copied' : (ariaLabel || 'Copy')"
    @click="handleCopy"
  >
  </button>
</template>

<script setup lang="ts">
import { ref } from 'vue'

interface Props {
  textToCopy?: string
  onCopy?: () => string | undefined | null
  className?: string
  ariaLabel?: string
}

const props = defineProps<Props>()

const copied = ref(false)

const handleCopy = async () => {
  if (!props.textToCopy && !props.onCopy) {
    return
  }

  let textToCopyFinal = props.textToCopy

  if (props.onCopy) {
    const result = props.onCopy()
    if (typeof result === 'string') {
      textToCopyFinal = result
    }
  }

  if (textToCopyFinal) {
    try {
      await navigator.clipboard.writeText(textToCopyFinal)
      copied.value = true
      setTimeout(() => {
        copied.value = false
      }, 1500)
    } catch (err) {
      console.error('Copy failed', err)
    }
  }
}
</script>
