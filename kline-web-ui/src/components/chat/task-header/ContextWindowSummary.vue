<template>
  <div class="flex flex-col gap-2 p-4 w-60 rounded border shadow-sm context-window-tooltip-content bg-[var(--vscode-menu-background)] border-[var(--vscode-menu-border)] z-[100]">
    <AccordionItem
      v-if="autoCompactThreshold > 0"
      :is-expanded="expandedSections.has('threshold')"
      title="Auto Condense Threshold"
      @toggle="(event?: MouseEvent) => toggleSection('threshold', event)"
    >
      <template #value>
        <span class="text-[var(--vscode-descriptionForeground)]">{{ `${(autoCompactThreshold * 100).toFixed(0)}%` }}</span>
      </template>
      <div class="space-y-1">
        <p class="text-xs leading-relaxed text-white">
          Click on the context window bar to set a new threshold.
        </p>
        <p class="mt-0 mb-0 text-xs leading-relaxed">
          When the context window usage exceeds this threshold, the task will be automatically condensed.
        </p>
      </div>
    </AccordionItem>

    <AccordionItem
      :is-expanded="expandedSections.has('context')"
      title="Context Window"
      @toggle="(event?: MouseEvent) => toggleSection('context', event)"
    >
      <template #value>
        {{ percentage ? `${percentage.toFixed(1)}% used` : formatTokenNumber(contextWindow) }}
      </template>
      <div class="space-y-1">
        <div class="flex justify-between">
          <span>Used:</span>
          <span class="font-mono">{{ formatTokenNumber(tokenUsed) }}</span>
        </div>
        <div class="flex justify-between">
          <span>Total:</span>
          <span class="font-mono">{{ formatTokenNumber(contextWindow) }}</span>
        </div>
        <div class="flex justify-between">
          <span>Remaining:</span>
          <span class="font-mono">{{ formatTokenNumber(contextWindow - tokenUsed) }}</span>
        </div>
      </div>
    </AccordionItem>

    <AccordionItem
      v-if="totalTokens > 0"
      :is-expanded="expandedSections.has('tokens')"
      title="Token Usage"
      @toggle="(event?: MouseEvent) => toggleSection('tokens', event)"
    >
      <template #value>
        {{ `${formatTokenNumber(totalTokens)} total` }}
      </template>
      <TokenUsageDetails
        :cache-reads="cacheReads"
        :cache-writes="cacheWrites"
        :tokens-in="tokensIn"
        :tokens-out="tokensOut"
      />
    </AccordionItem>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { formatLargeNumber as formatTokenNumber } from '@/utils/format'
import AccordionItem from './AccordionItem.vue'
import TokenUsageDetails from './TokenUsageDetails.vue'

interface Props {
  contextWindow: number
  tokenUsed: number
  tokensIn?: number
  tokensOut?: number
  cacheWrites?: number
  cacheReads?: number
  percentage: number
  autoCompactThreshold?: number
}

const props = withDefaults(defineProps<Props>(), {
  autoCompactThreshold: 0,
})

const expandedSections = ref<Set<string>>(new Set())

const toggleSection = (section: string, event?: MouseEvent) => {
  if (event) {
    event.preventDefault()
    event.stopPropagation()
  }
  const newSet = new Set(expandedSections.value)
  if (newSet.has(section)) {
    newSet.delete(section)
  } else {
    newSet.add(section)
  }
  expandedSections.value = newSet
}

const totalTokens = computed(() => {
  return (props.tokensIn || 0) + (props.tokensOut || 0) + (props.cacheWrites || 0) + (props.cacheReads || 0)
})
</script>

