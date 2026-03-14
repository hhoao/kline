<template>
  <div v-if="threshold" class="flex-1" id="auto-condense-threshold-marker">
    <div
      :class="[
        'absolute top-0 bottom-0 h-full cursor-pointer pointer-events-none z-10 bg-[var(--vscode-button-background)] shadow-lg w-1',
        {
          'transition-all duration-75': !isAnimating,
        },
      ]"
      :style="{
        left: marker.start,
        transform: isAnimating ? `translateX(${animatedPosition - threshold * 100}%)` : 'translateX(0)',
      }"
    >
      <div
        v-if="isContextWindowHoverOpen || isAnimating || showPercentageAfterAnimation"
        :class="[
          'absolute -top-4 -left-1 text-[var(--vscode-button-background)] font-mono text-xs',
          {
            'opacity-0': isFadingOut,
          },
        ]"
      >
        {{ marker.label }}%
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

interface Props {
  threshold: number
  usage: number
  isContextWindowHoverOpen?: boolean
  shouldAnimate?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  shouldAnimate: false,
})

const isAnimating = ref(false)
const animatedPosition = ref(0)
const showPercentageAfterAnimation = ref(false)
const isFadingOut = ref(false)

let animationFrameId: number | null = null
let fadeOutTimeoutId: NodeJS.Timeout | null = null
let hideTimeoutId: NodeJS.Timeout | null = null

const cleanup = () => {
  if (animationFrameId !== null) {
    cancelAnimationFrame(animationFrameId)
    animationFrameId = null
  }
  if (fadeOutTimeoutId !== null) {
    clearTimeout(fadeOutTimeoutId)
    fadeOutTimeoutId = null
  }
  if (hideTimeoutId !== null) {
    clearTimeout(hideTimeoutId)
    hideTimeoutId = null
  }
}

const startAnimation = () => {
  if (!props.shouldAnimate || props.threshold <= 0) {
    return
  }

  cleanup()

  isAnimating.value = true
  const targetPosition = props.threshold * 100
  const duration = 1200 // ms
  const startTime = Date.now()

  const animate = () => {
    const elapsed = Date.now() - startTime
    const progress = Math.min(elapsed / duration, 1)
    // Ease-out animation curve
    const easeOut = 1 - (1 - progress) ** 3
    const currentPosition = easeOut * targetPosition
    animatedPosition.value = currentPosition

    if (progress < 1) {
      animationFrameId = requestAnimationFrame(animate)
    } else {
      animationFrameId = null
      isAnimating.value = false
      showPercentageAfterAnimation.value = true
      // Start fade out after 1 second
      fadeOutTimeoutId = setTimeout(() => {
        isFadingOut.value = true
        // Completely hide after fade transition
        hideTimeoutId = setTimeout(() => {
          showPercentageAfterAnimation.value = false
          isFadingOut.value = false
          hideTimeoutId = null
          animatedPosition.value = props.threshold * 100 // Ensure it ends exactly at threshold
        }, 300) // 300ms fade duration
        fadeOutTimeoutId = null
      }, 1000)
    }
  }

  animationFrameId = requestAnimationFrame(animate)
}

watch(
  () => [props.shouldAnimate, props.threshold],
  () => {
    startAnimation()
  },
  { immediate: true }
)

onMounted(() => {
  startAnimation()
})

onBeforeUnmount(() => {
  cleanup()
})

const marker = computed(() => {
  const _threshold = props.threshold * 100
  // Always use the current threshold for position and label - animation only affects visual movement
  const position = _threshold
  const startingPosition = isAnimating.value ? animatedPosition.value : position

  return {
    start: startingPosition + '%',
    label: startingPosition.toFixed(0),
    end: props.usage > startingPosition ? props.usage - startingPosition + '%' : 0,
  }
})
</script>

