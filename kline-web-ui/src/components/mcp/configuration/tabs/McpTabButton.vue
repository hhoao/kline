<template>
  <button
    type="button"
    :class="[
      'border-none bg-transparent cursor-pointer font-size-13px mb-[-1px] font-inherit',
      isActive ? 'text-[var(--vscode-foreground)] border-b-2 border-[var(--vscode-foreground)]' : 'text-[var(--vscode-descriptionForeground)] border-b-2 border-transparent',
      disabled && 'opacity-60 cursor-not-allowed pointer-events-none'
    ]"
    :disabled="disabled"
    @click="emit('click')"
  >
    <slot />
  </button>
</template>

<script setup lang="ts">
defineProps<{
  isActive: boolean
  disabled?: boolean
}>()
const emit = defineEmits<{ click: [] }>()
</script>

<style scoped>
.font-size-13px {
  font-size: 13px;
}
</style>
