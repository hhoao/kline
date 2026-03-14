<template>
  <div
    class="flex border rounded border-[var(--vscode-editorWidget-border)] overflow-hidden cursor-pointer min-h-[80px] max-w-[512px] p-3"
    @click="openInBrowser"
  >
    <div class="flex-1 flex flex-col overflow-hidden min-w-0">
      <div class="font-bold mb-1 truncate">{{ hostname }}</div>
      <div class="text-[12px] text-[var(--vscode-textLink-foreground)] truncate mb-1 break-all">
        {{ url }}
      </div>
      <div class="text-[11px] text-[var(--vscode-descriptionForeground)] mt-1">
        Click to open in browser
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { getSafeHostname } from './utils/mcpRichUtil'

const props = defineProps<{ url: string }>()

const hostname = computed(() => getSafeHostname(props.url))

function openInBrowser() {
  try {
    window.open(props.url, '_blank', 'noopener,noreferrer')
  } catch (e) {
    console.error('Error opening URL:', e)
  }
}
</script>
