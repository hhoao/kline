<template>
  <button
    :class="[
      'w-full px-4 py-2 rounded border cursor-pointer transition-colors',
      'bg-[var(--vscode-button-secondaryBackground)] border-[var(--vscode-button-secondaryBackground)] text-[var(--vscode-button-foreground)]',
      'hover:bg-[var(--vscode-button-secondaryHoverBackground)] hover:border-[var(--vscode-button-secondaryHoverBackground)]',
      'active:bg-[var(--vscode-button-secondaryBackground)] active:border-[var(--vscode-button-secondaryBackground)]',
      props.class
    ]"
    v-bind="$attrs"
    @click="$emit('click', $event)"
  >
    <slot />
  </button>
</template>

<script setup lang="ts">
interface Props {
  class?: string
}

const props = defineProps<Props>()

defineEmits<{
  click: [event: MouseEvent]
}>()
</script>

<style scoped>
button :deep(.codicon) {
  margin-right: 6px;
  flex-shrink: 0;
  font-size: 16px !important;
}
</style>

