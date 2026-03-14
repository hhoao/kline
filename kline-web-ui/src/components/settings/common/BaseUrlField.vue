<template>
  <div>
    <div class="flex items-center gap-2">
      <label class="flex items-center gap-2 cursor-pointer">
        <input
          :checked="isEnabled"
          type="checkbox"
          :disabled="disabled"
          class="rounded border-[var(--vscode-checkbox-border)]"
          @change="onToggle"
        />
        <span>{{ label }}</span>
      </label>
      <i v-if="showLockIcon" class="codicon codicon-lock text-[var(--vscode-descriptionForeground)] text-sm" />
    </div>
    <div v-if="isEnabled" class="mt-1">
      <DebouncedTextField
        :initial-value="localValueRef.value"
        :disabled="disabled"
        :placeholder="placeholder"
        :on-change="(v: string) => emit('change', v)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useDebouncedInput } from '../utils/useDebouncedInput'
import DebouncedTextField from './DebouncedTextField.vue'

const props = withDefaults(
  defineProps<{
    initialValue?: string
    defaultValue?: string
    label?: string
    placeholder?: string
    disabled?: boolean
    showLockIcon?: boolean
  }>(),
  {
    label: 'Use custom base URL',
    placeholder: 'Default: https://api.example.com',
    disabled: false,
    showLockIcon: false,
  }
)

const emit = defineEmits<{ change: [value: string] }>()

const isEnabled = ref(!!props.initialValue)
const [localValueRef, setLocalValue] = useDebouncedInput(
  props.initialValue || '',
  (v: string) => emit('change', v),
  300
)

watch(
  () => props.initialValue,
  (v) => {
    isEnabled.value = !!v
    setLocalValue(v || '')
  },
  { immediate: true }
)

function onToggle(e: Event) {
  const checked = (e.target as HTMLInputElement).checked
  isEnabled.value = checked
  if (!checked) {
    setLocalValue('')
  }
}
</script>
