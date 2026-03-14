<template>
  <div v-if="!tokensIn" class="text-sm">No token usage data available</div>
  <div v-else class="space-y-2">
    <div
      v-for="item in contextTokenDetails"
      :key="item.icon"
      class="flex justify-between items-center"
    >
      <div class="flex gap-1 items-center">
        <i :class="`${item.icon} text-xs`" />
        <span>{{ item.title }}</span>
      </div>
      <span class="font-mono">{{ formatTokenNumber(item.value || 0) }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { formatLargeNumber as formatTokenNumber } from '@/utils/format'

interface Props {
  tokensIn?: number
  tokensOut?: number
  cacheWrites?: number
  cacheReads?: number
}

const props = defineProps<Props>()

const TOKEN_DETAILS_CONFIG = [
  { title: 'Prompt Tokens', icon: 'i-codicon:arrow-up' },
  { title: 'Completion Tokens', icon: 'i-codicon:arrow-down' },
  { title: 'Cache Writes', icon: 'i-codicon:arrow-left' },
  { title: 'Cache Reads', icon: 'i-codicon:arrow-right' },
]

const contextTokenDetails = computed(() => {
  const values = [props.tokensIn, props.tokensOut, props.cacheWrites || 0, props.cacheReads || 0]
  return TOKEN_DETAILS_CONFIG.map((config, index) => ({ ...config, value: values[index] })).filter(
    (item) => item.value
  )
})
</script>

