<template>
  <select
    :id="id"
    :value="modelValue"
    :class="className"
    class="px-2 py-1 rounded border border-[var(--vscode-select-border)] bg-[var(--vscode-select-background)] text-[var(--vscode-select-foreground)] text-[13px]"
    :style="style"
    @change="handleChange"
    @click.stop
  >
    <option value="plain">Plain Text</option>
    <option value="rich">Rich Display</option>
    <option value="markdown">Markdown</option>
  </select>
</template>

<script setup lang="ts">
import type { McpDisplayMode } from '@/shared/McpDisplayMode'

defineProps<{
  modelValue: McpDisplayMode
  id?: string
  className?: string
  style?: string | Record<string, string>
}>()

const emit = defineEmits<{ 'update:modelValue': [value: McpDisplayMode] }>()

function handleChange(ev: Event) {
  const value = (ev.target as HTMLSelectElement).value as McpDisplayMode
  emit('update:modelValue', value)
}
</script>
