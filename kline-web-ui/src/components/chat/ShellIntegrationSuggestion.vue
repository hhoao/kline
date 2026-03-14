<template>
  <div
    class="p-2 bg-[rgba(0,122,204,0.1)] rounded-[3px] border border-[rgba(0,122,204,0.3)]"
  >
    <div class="flex items-center mb-1">
      <i
        class="i-codicon:lightbulb mr-1.5 text-sm text-[var(--vscode-textLink-foreground)]"
      ></i>
      <span class="font-medium text-[var(--vscode-foreground)]">
        Shell integration issues
      </span>
    </div>
    <div class="text-[var(--vscode-foreground)] opacity-90 mb-2">
      Since you're experiencing repeated shell integration issues, we recommend switching to
      Background Terminal mode for better reliability.
    </div>
    <button
      :disabled="isBackgroundModeEnabled"
      class="bg-[var(--vscode-button-background)] text-[var(--vscode-button-foreground)] border-0 rounded-[2px] px-3 py-1.5 text-xs cursor-pointer font-inherit flex items-center gap-1.5"
      :class="{
        'bg-[var(--vscode-charts-green)] opacity-80 cursor-default': isBackgroundModeEnabled,
        'hover:bg-[var(--vscode-button-hoverBackground)]': !isBackgroundModeEnabled,
      }"
      @click="handleEnableBackgroundTerminal"
    >
      <i class="i-codicon:settings-gear"></i>
      {{
        isBackgroundModeEnabled
          ? 'Background Terminal Enabled'
          : 'Enable Background Terminal (Recommended)'
      }}
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useExtensionStateStore } from "@/stores/extensionState"
import { uiService } from '@/api/ui'

const extensionState = computed(() => useExtensionStateStore().extensionState)
const vscodeTerminalExecutionMode = computed(
  () => extensionState.value?.vscodeTerminalExecutionMode || ''
)

const isBackgroundModeEnabled = computed(() => {
  return vscodeTerminalExecutionMode.value === 'backgroundExec'
})

const handleEnableBackgroundTerminal = async () => {
  try {
    await uiService.setTerminalExecutionMode(true)
  } catch (error) {
    console.error('Failed to enable background terminal:', error)
  }
}
</script>


