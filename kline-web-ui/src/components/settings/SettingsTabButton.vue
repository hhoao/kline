<template>
  <button
    type="button"
    :title="title"
    :data-testid="tabId ? `tab-${tabId}` : undefined"
    :data-value="tabId"
    :data-compact="compact"
    :class="[
      'whitespace-nowrap overflow-hidden min-w-0 h-12 px-4 py-3 box-border flex items-center border-l-2 border-transparent text-[var(--vscode-foreground)] bg-transparent cursor-pointer font-inherit text-left w-full',
      isActive
        ? 'opacity-100 border-l-2 border-l-[var(--vscode-focusBorder)] bg-[var(--vscode-list-activeSelectionBackground)]'
        : 'opacity-70 hover:bg-[var(--vscode-list-hoverBackground)]'
    ]"
    @click="emit('click')"
  >
    <slot />
  </button>
</template>

<script setup lang="ts">
defineProps<{
  isActive: boolean
  title?: string
  tabId?: string
  compact?: boolean
}>()
const emit = defineEmits<{ click: [] }>()
</script>
