<template>
  <HeroTooltip content="Close current task to start new one" :delay="1000" placement="bottom">
    <button
      aria-label="Start a New Task"
      :class="[
        'flex items-center border-0 text-sm font-bold bg-transparent opacity-70 hover:opacity-100 hover:bg-[var(--vscode-toolbar-hoverBackground)]/10 px-0 cursor-pointer',
        props.className
      ]"
      type="button"
      @click="handleClick"
    >
      <svg
        xmlns="http://www.w3.org/2000/svg"
        width="14"
        height="14"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <path d="M18 6L6 18" />
        <path d="M6 6l12 12" />
      </svg>
    </button>
  </HeroTooltip>
</template>

<script setup lang="ts">
import HeroTooltip from '@/components/common/HeroTooltip.vue'

interface Props {
  className?: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  click: []
}>()

const handleClick = (e: MouseEvent) => {
  e.preventDefault()
  e.stopPropagation()
  emit('click')
}
</script>

