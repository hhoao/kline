<template>
  <div :class="['px-5 py-2.5 border-b border-[var(--vscode-panel-border)]', props.class]">
    <slot />
  </div>
</template>

<script setup lang="ts">
interface Props {
  class?: string
}

const props = defineProps<Props>()
</script>

