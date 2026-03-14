<template>
  <div>
    <label class="flex items-center gap-2 cursor-pointer">
      <input
        v-model="isEnabled"
        type="checkbox"
        class="rounded border-[var(--vscode-checkbox-border)]"
        @change="onToggle"
      />
      <span>Enable thinking{{ localValue > 0 ? ` (${localValue.toLocaleString()} tokens)` : '' }}</span>
    </label>
    <div v-if="isEnabled" class="mt-1 mb-2 flex flex-col">
      <input
        :id="sliderId"
        v-model.number="localValue"
        type="range"
        :min="minBudget"
        :max="maxBudget"
        step="1"
        class="thinking-range mt-1"
        :style="rangeStyle"
        aria-label="Thinking budget (tokens)"
        @change="onSliderCommit"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useExtensionStateStore } from '@/stores/extensionState'
import { updateSetting } from './utils/settingsHandlers'
import {
  ANTHROPIC_MIN_THINKING_BUDGET,
  ANTHROPIC_MAX_THINKING_BUDGET,
} from '@/shared/api'

const props = defineProps<{
  currentMode: 'plan' | 'act'
}>()

const store = useExtensionStateStore()
const minBudget = ANTHROPIC_MIN_THINKING_BUDGET
const maxBudget = ANTHROPIC_MAX_THINKING_BUDGET

const settingKey = computed(() =>
  props.currentMode === 'plan' ? 'planModeThinkingBudgetTokens' : 'actModeThinkingBudgetTokens'
)

const storedValue = computed(() => {
  const cfg = store.extensionState?.apiConfiguration
  if (!cfg) return 0
  const v = (cfg as Record<string, number | undefined>)[settingKey.value]
  return v ?? 0
})

const localValue = ref(Math.max(minBudget, Math.min(maxBudget, storedValue.value)))
const isEnabled = ref(storedValue.value > 0)

watch(
  storedValue,
  (v) => {
    const n = v ?? 0
    localValue.value = Math.max(minBudget, Math.min(maxBudget, n))
    isEnabled.value = n > 0
  },
  { immediate: true }
)

const sliderId = ref(`thinking-budget-slider-${Math.random().toString(36).slice(2, 9)}`)

const rangeStyle = computed(() => {
  const pct = ((localValue.value - minBudget) / (maxBudget - minBudget)) * 100
  return {
    '--pct': `${pct}%`,
  } as Record<string, string>
})

function commit(value: number) {
  updateSetting(settingKey.value as 'planModeThinkingBudgetTokens' | 'actModeThinkingBudgetTokens', value)
}

function onToggle() {
  const value = isEnabled.value ? minBudget : 0
  localValue.value = value
  commit(value)
}

function onSliderCommit() {
  const clamped = Math.max(minBudget, Math.min(maxBudget, localValue.value))
  localValue.value = clamped
  commit(clamped)
}
</script>

<style scoped>
.thinking-range {
  width: 100%;
  height: 8px;
  appearance: none;
  border-radius: 4px;
  outline: none;
  cursor: pointer;
  padding: 0;
  background: linear-gradient(
    to right,
    var(--vscode-progressBar-background) 0%,
    var(--vscode-progressBar-background) var(--pct, 0%),
    var(--vscode-scrollbarSlider-background) var(--pct, 0%),
    var(--vscode-scrollbarSlider-background) 100%
  ) !important;
}
.thinking-range::-webkit-slider-thumb {
  appearance: none;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--vscode-foreground);
  cursor: pointer;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}
.thinking-range::-moz-range-thumb {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--vscode-foreground);
  cursor: pointer;
  border: none;
}
</style>
