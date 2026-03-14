<template>
  <button
    :class="[
      'px-4 py-2 rounded border cursor-pointer transition-colors',
      'bg-[var(--vscode-button-secondaryBackground)] text-[var(--vscode-button-foreground)] border-[var(--vscode-button-secondaryBackground)]',
      'hover:bg-[var(--vscode-button-secondaryHoverBackground)]',
      props.class
    ]"
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

