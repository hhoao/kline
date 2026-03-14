<template>
  <div>
    <label for="subagent-output-limit" class="font-semibold text-xs block mb-1">
      Subagent output limit
    </label>
    <div class="flex items-center gap-4">
      <input
        id="subagent-output-limit"
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
    <p class="text-[11px] text-[var(--vscode-descriptionForeground)] mt-1">
      Maximum number of lines to include in output from CLI subagents. Truncates middle to save tokens.
    </p>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useExtensionStateStore } from '@/stores/extensionState'
import { updateSetting } from './utils/settingsHandlers'

const store = useExtensionStateStore()
const localValue = ref(store.extensionState?.subagentTerminalOutputLineLimit ?? 2000)

watch(
  () => store.extensionState?.subagentTerminalOutputLineLimit,
  (v) => { if (v !== undefined) localValue.value = v },
  { immediate: true }
)

function onChange() {
  updateSetting('subagentTerminalOutputLineLimit', localValue.value)
}
</script>
