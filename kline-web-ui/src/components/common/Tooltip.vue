<template>
  <div
    class="relative inline-block"
    @mouseenter="isHovered = true"
    @mouseleave="isHovered = false"
  >
    <slot />
    <div
      v-if="shouldShow"
      :class="[
        'absolute bottom-full mb-2 px-1.5 py-1 rounded text-xs whitespace-normal max-w-[200px] pointer-events-none z-[1000]',
        'bg-[var(--vscode-sideBar-background)] text-[var(--vscode-descriptionForeground)]',
        'border border-[var(--vscode-input-border)]'
      ]"
      :style="tooltipStyle"
    >
      {{ tipText }}
      <div
        v-if="hintText"
        class="text-[0.8em] text-[var(--vscode-input-placeholderForeground)] opacity-80 mt-0.5"
      >
        {{ hintText }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

interface Props {
  visible?: boolean
  hintText?: string
  tipText: string
  style?: {
    left?: string | number
    zIndex?: number
  }
}

const props = defineProps<Props>()

const isHovered = ref(false)

const shouldShow = computed(() => {
  return props.visible !== undefined ? props.visible : isHovered.value
})

const tooltipStyle = computed(() => {
  return {
    left: props.style?.left ?? '-180%',
    zIndex: props.style?.zIndex ?? 1000,
  }
})
</script>

