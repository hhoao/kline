<template>
  <div
    class="relative inline-block group"
    @mouseenter="isHovered = true"
    @mouseleave="isHovered = false"
  >
    <slot />
    <div
      v-if="!disabled && isHovered"
      :class="[
        'absolute z-50 pointer-events-none transition-opacity',
        placement === 'top' ? 'bottom-full mb-2' : '',
        placement === 'bottom' ? 'top-full mt-2' : '',
        placement === 'left' ? 'right-full mr-2' : '',
        placement === 'right' ? 'left-full ml-2' : '',
        'bg-code-background text-code-foreground border border-code-foreground/20 rounded shadow-md max-w-[250px] text-sm p-2',
        className
      ]"
      :style="{ transitionDelay: `${delay}ms` }"
    >
      <span class="whitespace-pre-wrap break-words overflow-y-auto">
        <slot name="content">
          {{ typeof content === 'string' ? content : '' }}
        </slot>
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

interface Props {
  content?: string | any
  className?: string
  delay?: number
  closeDelay?: number
  placement?: 'top' | 'bottom' | 'left' | 'right'
  showArrow?: boolean
  disabled?: boolean
}

withDefaults(defineProps<Props>(), {
  showArrow: false,
  delay: 0,
  closeDelay: 500,
  placement: 'top',
  disabled: false,
})

const isHovered = ref(false)
</script>

