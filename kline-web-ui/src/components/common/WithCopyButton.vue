<template>
  <div
    ref="containerRef"
    :class="['relative group', className]"
    :style="style"
    @mouseup="onMouseUp"
  >
    <slot />
    <div
      v-if="textToCopy || onCopy"
      :class="[
        'absolute z-10 opacity-0 transition-opacity pointer-events-auto',
        position === 'bottom-right' ? 'bottom-0.5 right-0.5' : 'top-1.25 right-1.25',
        'group-hover:opacity-100'
      ]"
    >
      <CopyButton
        :text-to-copy="textToCopy"
        :on-copy="onCopy"
        :aria-label="ariaLabel"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import CopyButton from './CopyButton.vue'

interface Props {
  textToCopy?: string
  onCopy?: () => string | undefined | null
  position?: 'top-right' | 'bottom-right'
  style?: Record<string, any>
  className?: string
  onMouseUp?: (event: MouseEvent) => void
  ariaLabel?: string
}

withDefaults(defineProps<Props>(), {
  position: 'top-right',
})

const containerRef = ref<HTMLDivElement | null>(null)

defineExpose({
  containerRef,
})
</script>

