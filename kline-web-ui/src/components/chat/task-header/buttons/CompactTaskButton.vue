<template>
  <HeroTooltip
    :content="tooltipContent"
    :delay="0"
    :disable-animation="true"
    placement="bottom"
  >
    <button
      :class="[
        'text-[var(--vscode-foreground)] flex items-center text-sm font-bold hover:bg-transparent hover:opacity-80 bg-transparent border-0 cursor-pointer p-0',
        props.className
      ]"
      type="button"
      @click="handleClick"
    >
      <svg
        xmlns="http://www.w3.org/2000/svg"
        width="12"
        height="12"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <path d="M12 3v18" />
        <path d="M8 7l4-4 4 4" />
        <path d="M8 17l4 4 4-4" />
      </svg>
    </button>
  </HeroTooltip>
</template>

<script setup lang="ts">
import { h } from 'vue'
import HeroTooltip from '@/components/common/HeroTooltip.vue'

interface Props {
  className?: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  click: [event: MouseEvent]
}>()

const handleClick = (e: MouseEvent) => {
  emit('click', e)
}

const tooltipContent = h('div', {
  class: 'flex flex-col gap-1 bg-[var(--vscode-menu-background)] rounded shadow-sm border border-[var(--vscode-menu-border)] z-[100] max-w-xs py-1 px-2',
}, [
  h('div', { class: 'text-xs font-medium' }, 'Compact Task'),
  h('div', { class: 'text-xs text-[var(--vscode-descriptionForeground)]' }, 'Reduces the number of tokens used by summarizing the task.'),
])
</script>

