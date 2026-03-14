<template>
  <div class="pt-2.5 px-0">
    <div class="rounded-[3px] bg-[var(--vscode-editor-background)] overflow-hidden border border-[var(--vscode-editorGroup-border)]">
      <div class="flex items-center px-2.5 py-2.25">
        <span class="whitespace-normal break-words">
          <span class="font-medium">Browse Action: </span>
          {{ actionText }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { BrowserAction } from '@/shared/ExtensionMessage'

interface Props {
  action: BrowserAction
  coordinate?: string
  text?: string
}

const props = defineProps<Props>()

const actionText = computed(() => {
  switch (props.action) {
    case 'launch':
      return `Launch browser at ${props.text}`
    case 'click':
      return `Click (${props.coordinate?.replace(',', ', ')})`
    case 'type':
      return `Type "${props.text}"`
    case 'scroll_down':
      return 'Scroll down'
    case 'scroll_up':
      return 'Scroll up'
    case 'close':
      return 'Close browser'
    default:
      return props.action
  }
})
</script>


