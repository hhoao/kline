<template>
  <div
    :class="[
      'fixed top-[50%] left-[50%] grid w-full max-w-[calc(100%-2rem)] -translate-x-1/2 -translate-y-1/2',
      props.class
    ]"
    @click.stop
  >
    <div class="bg-[var(--vscode-editor-background)] rounded-sm gap-3 border border-[var(--vscode-panel-border)] p-6 shadow-lg sm:max-w-lg">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  class?: string
}

const props = defineProps<Props>()
</script>

