<template>
  <input
    :id="id"
    :type="type"
    :value="localValue"
    :placeholder="placeholder"
    :disabled="disabled"
    class="w-full px-2 py-1 rounded border border-[var(--vscode-input-border)] bg-[var(--vscode-input-background)] text-[var(--vscode-input-foreground)] text-[13px]"
    :class="className"
    :style="style"
    @input="onInput"
  />
</template>

<script setup lang="ts">
import { watch } from 'vue'
import { useDebouncedInput } from '../utils/useDebouncedInput'

const props = withDefaults(
  defineProps<{
    initialValue: string
    onChange: (value: string) => void
    type?: 'text' | 'password'
    placeholder?: string
    id?: string
    disabled?: boolean
    className?: string
    style?: string | Record<string, string>
  }>(),
  { type: 'text', disabled: false }
)

const [localValueRef, setLocalValue] = useDebouncedInput(
  props.initialValue,
  props.onChange,
  100
)
const localValue = localValueRef

watch(
  () => props.initialValue,
  (v) => { setLocalValue(v) },
  { immediate: true }
)

function onInput(e: Event) {
  const value = (e.target as HTMLInputElement).value
  setLocalValue(value)
}
</script>
