<template>
  <div class="dropdown-container" :style="{ position: 'relative', zIndex: zIndex ?? 1000 }">
    <label :for="selectId">
      <span class="font-medium">{{ label }}</span>
    </label>
    <select
      :id="selectId"
      :value="selectedModelId ?? ''"
      class="mt-1 w-full px-2 py-1.5 rounded border border-[var(--vscode-input-border)] bg-[var(--vscode-input-background)] text-[var(--vscode-input-foreground)] text-[13px]"
      @change="onSelectChange"
    >
      <option value="">Select a model...</option>
      <option v-for="id in modelIds" :key="id" :value="id">{{ id }}</option>
    </select>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { ModelInfo } from '@/shared/api'

const props = withDefaults(
  defineProps<{
    models: Record<string, ModelInfo>
    selectedModelId?: string
    zIndex?: number
    label?: string
    inputId?: string
  }>(),
  { label: 'Model' }
)

const emit = defineEmits<{ change: [value: string] }>()

const fallbackId = ref(`model-selector-${Math.random().toString(36).slice(2, 9)}`)
const selectId = computed(() => props.inputId ?? fallbackId.value)
const modelIds = computed(() => Object.keys(props.models))

function onSelectChange(e: Event) {
  const value = (e.target as HTMLSelectElement).value
  emit('change', value)
}
</script>
