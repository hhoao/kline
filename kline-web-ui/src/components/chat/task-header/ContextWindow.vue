<template>
  <div v-if="tokenData" class="flex flex-col my-1.5" @mouseleave="debounceCloseHover">
    <div class="flex flex-row gap-1 items-center text-sm">
      <div class="flex flex-1 gap-1.5 items-center text-xs whitespace-nowrap">
        <span class="cursor-pointer" title="Current tokens used in this request">
          {{ formatTokenNumber(tokenData.used) }}
        </span>
        <div
          class="flex relative flex-1 gap-1 items-center w-full h-full"
          @mouseenter="isOpened = true"
        >
          <div
            ref="progressBarRef"
            aria-label="Auto condense threshold"
            :aria-valuemax="100"
            :aria-valuemin="0"
            :aria-valuenow="Math.round(threshold * 100)"
            :aria-valuetext="`${Math.round(threshold * 100)}% threshold`"
            class="relative w-full text-[var(--vscode-foreground)] context-window-progress brightness-100"
            :tabindex="useAutoCondense ? 0 : -1"
            role="slider"
            @click="handleContextWindowBarClick"
            @focus="handleFocus"
            @keydown="handleKeyDown"
          >
            <div
              class="relative w-full h-3 rounded bg-[var(--vscode-foreground)]/10 cursor-pointer drop-shadow-md"
              @click.stop="handleContextWindowBarClick"
            >
              <div
                class="h-full bg-[var(--vscode-foreground)] rounded-r transition-all"
                :style="{ width: `${tokenData.percentage}%` }"
              />
            </div>
            <AutoCondenseMarker
              v-if="useAutoCondense"
              :is-context-window-hover-open="isOpened"
              :should-animate="shouldAnimateMarker"
              :threshold="threshold"
              :usage="tokenData.percentage"
            />
          </div>
          <Teleport to="body">
            <div
              v-if="isOpened"
              class="fixed z-[1000] pointer-events-none"
              :style="tooltipStyle"
            >
              <ContextWindowSummary
                :auto-compact-threshold="useAutoCondense ? threshold : undefined"
                :cache-reads="cacheReads"
                :cache-writes="cacheWrites"
                :context-window="tokenData.max"
                :percentage="tokenData.percentage"
                :tokens-in="tokensIn"
                :tokens-out="tokensOut"
                :token-used="tokenData.used"
              />
            </div>
          </Teleport>
        </div>
        <span class="cursor-pointer" title="Maximum context window size for this model">
          {{ formatTokenNumber(tokenData.max) }}
        </span>
      </div>
      <CompactTaskButton @click="handleCompactClick" />
    </div>
    <ConfirmationDialog
      v-if="confirmationNeeded"
      @cancel="handleCancel"
      @confirm="handleConfirm"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { formatLargeNumber as formatTokenNumber } from '@/utils/format'
import { stateService } from '@/api/state'
import AutoCondenseMarker from './AutoCondenseMarker.vue'
import CompactTaskButton from './buttons/CompactTaskButton.vue'
import ContextWindowSummary from './ContextWindowSummary.vue'
import ConfirmationDialog from './ConfirmationDialog.vue'
import debounce from 'debounce'

interface Props {
  contextWindow?: number
  lastApiReqTotalTokens?: number
  autoCondenseThreshold?: number
  onSendMessage?: (command: string, files: string[], images: string[]) => void
  useAutoCondense: boolean
  tokensIn?: number
  tokensOut?: number
  cacheWrites?: number
  cacheReads?: number
}

const props = withDefaults(defineProps<Props>(), {
  contextWindow: 0,
  lastApiReqTotalTokens: 0,
  autoCondenseThreshold: 0.75,
})

const isOpened = ref(false)
const threshold = ref(props.useAutoCondense ? props.autoCondenseThreshold : 0)
const confirmationNeeded = ref(false)
const progressBarRef = ref<HTMLDivElement | null>(null)
const shouldAnimateMarker = ref(false)
const tooltipStyle = ref<{ top: string; left: string }>({ top: '0px', left: '0px' })

const tokenData = computed(() => {
  if (!props.contextWindow) {
    return null
  }
  return {
    percentage: (props.lastApiReqTotalTokens / props.contextWindow) * 100,
    max: props.contextWindow,
    used: props.lastApiReqTotalTokens,
  }
})

const updateTooltipPosition = () => {
  if (!progressBarRef.value || !isOpened.value) return
  
  const rect = progressBarRef.value.getBoundingClientRect()
  tooltipStyle.value = {
    top: `${rect.bottom + 8}px`,
    left: `${rect.left}px`,
  }
}

watch(isOpened, (newVal) => {
  if (newVal) {
    updateTooltipPosition()
  }
})

const updateSetting = async (key: string, value: any) => {
  try {
    await stateService.updateSettings({ [key]: value })
  } catch (error) {
    console.error('Error updating setting:', error)
  }
}

const handleContextWindowBarClick = (event: MouseEvent) => {
  const target = event.currentTarget as HTMLDivElement
  const rect = target.getBoundingClientRect()
  const clickX = event.clientX - rect.left
  const percentage = Math.max(0, Math.min(1, clickX / rect.width))
  const newThreshold = Math.round(percentage * 100) / 100
  confirmationNeeded.value = false
  threshold.value = newThreshold
  updateSetting('autoCondenseThreshold', newThreshold)
}

const handleCompactClick = (e: MouseEvent) => {
  e.preventDefault()
  e.stopPropagation()
  confirmationNeeded.value = !confirmationNeeded.value
}

const handleConfirm = (e: MouseEvent) => {
  e.preventDefault()
  e.stopPropagation()
  props.onSendMessage?.('/compact', [], [])
  confirmationNeeded.value = false
}

const handleCancel = (e: MouseEvent) => {
  e.preventDefault()
  e.stopPropagation()
  confirmationNeeded.value = false
}

const debounceCloseHover = debounce(() => {
  isOpened.value = false
}, 100)

const handleKeyDown = (event: KeyboardEvent) => {
  if (!props.useAutoCondense) {
    return
  }

  const step = event.shiftKey ? 0.1 : 0.05
  let newThreshold = threshold.value

  switch (event.key) {
    case 'ArrowLeft':
    case 'ArrowDown':
      event.preventDefault()
      event.stopPropagation()
      isOpened.value = true
      newThreshold = Math.max(0, threshold.value - step)
      break
    case 'ArrowRight':
    case 'ArrowUp':
      event.preventDefault()
      event.stopPropagation()
      isOpened.value = true
      newThreshold = Math.min(1, threshold.value + step)
      break
    default:
      return
  }

  if (newThreshold !== threshold.value) {
    threshold.value = newThreshold
    updateSetting('autoCondenseThreshold', newThreshold)
  }
}

const handleFocus = () => {
  isOpened.value = true
}

const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as Element
  const isInsideProgressBar = progressBarRef.value && progressBarRef.value.contains(target as Node)
  const isInsideTooltipContent = target.closest('.context-window-tooltip-content') !== null

  if (!isInsideProgressBar && !isInsideTooltipContent) {
    isOpened.value = false
  }
}

watch(isOpened, (newVal) => {
  if (newVal) {
    document.addEventListener('mousedown', handleClickOutside)
    updateTooltipPosition()
  } else {
    document.removeEventListener('mousedown', handleClickOutside)
  }
})

onMounted(() => {
  if (props.useAutoCondense && threshold.value > 0) {
    shouldAnimateMarker.value = true
    const timer = setTimeout(() => {
      shouldAnimateMarker.value = false
    }, 1400)
    return () => clearTimeout(timer)
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('mousedown', handleClickOutside)
})
</script>

