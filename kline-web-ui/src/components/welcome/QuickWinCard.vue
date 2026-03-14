<template>
  <div
    class="flex items-center mb-2 py-0 px-5 space-x-3 rounded-full cursor-pointer group transition-colors duration-150 ease-in-out bg-white/[0.02] border border-[var(--vscode-panel-border)] hover:bg-[var(--vscode-list-hoverBackground)]"
    @click="handleClick"
  >
    <div class="flex-shrink-0 flex items-center justify-center w-6 h-6 text-[var(--vscode-icon-foreground)]">
      <span :class="iconClass" class="!text-[28px] !leading-[1]"></span>
    </div>

    <div class="flex-grow min-w-0">
      <h3 class="text-sm font-medium truncate text-[var(--vscode-editor-foreground)] leading-tight mb-0 mt-0 pt-3">
        {{ task.title }}
      </h3>
      <p class="text-xs truncate text-[var(--vscode-descriptionForeground)] leading-tight mt-[1px]">
        {{ task.description }}
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { QuickWinTask } from './quickWinTasks'

interface Props {
  task: QuickWinTask
}

const props = defineProps<Props>()

const emit = defineEmits<{
  execute: []
}>()

const iconClass = computed(() => {
  if (!props.task.icon) {
    return 'i-codicon:rocket'
  }

  switch (props.task.icon) {
    case 'WebAppIcon':
      return 'i-codicon:dashboard'
    case 'TerminalIcon':
      return 'i-codicon:terminal'
    case 'GameIcon':
      return 'i-codicon:game'
    default:
      return 'i-codicon:rocket'
  }
})

const handleClick = () => {
  emit('execute')
}
</script>

