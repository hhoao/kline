<template>
  <div>
    <label class="flex items-center gap-2 cursor-pointer">
      <input
        :checked="isCompactPromptEnabled"
        type="checkbox"
        class="rounded border-[var(--vscode-checkbox-border)]"
        @change="onToggle"
      />
      <span>Use compact prompt</span>
    </label>
    <p class="text-xs mt-1 text-[var(--vscode-descriptionForeground)]">
      A system prompt optimized for smaller context window (e.g. 8k or less).
    </p>
    <p class="text-xs mt-0.5 flex items-center gap-1 text-[var(--vscode-errorForeground)]">
      <i class="codicon codicon-close block" />
      Does not support Mcp and Focus Chain
    </p>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useExtensionStateStore } from '@/stores/extensionState'
import { updateSetting } from './utils/settingsHandlers'

const store = useExtensionStateStore()
const isCompactPromptEnabled = ref(store.extensionState?.customPrompt === 'compact')

watch(
  () => store.extensionState?.customPrompt,
  (v) => { isCompactPromptEnabled.value = v === 'compact' },
  { immediate: true }
)

function onToggle(e: Event) {
  const checked = (e.target as HTMLInputElement).checked
  isCompactPromptEnabled.value = checked
  updateSetting('customPrompt', checked ? 'compact' : '')
}
</script>
