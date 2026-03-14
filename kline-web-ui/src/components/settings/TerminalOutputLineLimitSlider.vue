<template>
  <div class="mb-4">
    <label for="terminal-output-limit" class="font-medium block mb-1">
      Terminal output limit
    </label>
    <div class="flex items-center gap-4">
      <input
        id="terminal-output-limit"
        v-model.number="localValue"
        type="range"
        min="100"
        max="5000"
        step="100"
        class="flex-1"
        @change="onChange"
      />
      <span>{{ localValue }}</span>
    </div>
    <p class="text-xs text-[var(--vscode-descriptionForeground)] mt-1">
      Maximum number of lines to include in terminal output when executing commands. When exceeded, lines will be
      removed from the middle, saving tokens.
    </p>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useExtensionStateStore } from '@/stores/extensionState'
import { updateSetting } from './utils/settingsHandlers'

const store = useExtensionStateStore()
const localValue = ref(store.extensionState?.terminalOutputLineLimit ?? 500)

watch(
  () => store.extensionState?.terminalOutputLineLimit,
  (v) => { if (v !== undefined) localValue.value = v },
  { immediate: true }
)

function onChange() {
  updateSetting('terminalOutputLineLimit', localValue.value)
}
</script>
