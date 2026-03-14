<template>
  <p :class="['text-[var(--vscode-descriptionForeground)] text-sm text-left', props.class]">
    <slot />
  </p>
</template>

<script setup lang="ts">
interface Props {
  class?: string
}

const props = defineProps<Props>()
</script>

