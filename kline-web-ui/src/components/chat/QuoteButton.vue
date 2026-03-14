<template>
  <div
    class="quote-button-class absolute z-10 bg-[var(--vscode-button-background)] border border-[var(--vscode-button-border)] rounded transition-transform duration-100 hover:scale-105 hover:bg-[var(--vscode-button-hoverBackground)]"
    :style="{
      top: `${top}px`,
      left: `${left}px`,
      boxShadow: '0 2px 8px rgba(0, 0, 0, 0.2)',
    }"
  >
    <button
      class="bg-transparent border-0 cursor-pointer p-[2px_4px] h-auto min-w-auto flex items-center justify-center"
      aria-label="Quote selection"
      @click.stop="handleClick"
      title="Quote selection in reply"
    >
      <span
        class="i-codicon:quote text-xs text-[var(--vscode-button-foreground)]"
      ></span>
    </button>
  </div>
</template>

<script setup lang="ts">
interface Props {
  top: number
  left: number
}

const props = defineProps<Props>()

const emit = defineEmits<{
  click: []
}>()

const handleClick = () => {
  emit('click')
}
</script>
