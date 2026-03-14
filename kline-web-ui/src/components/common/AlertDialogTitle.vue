<template>
  <h2 :class="['text-base font-medium text-[var(--vscode-editor-foreground)] flex items-center gap-2 text-left', props.class]">
    <slot />
  </h2>
</template>

<script setup lang="ts">
interface Props {
  class?: string
}

const props = defineProps<Props>()
</script>

