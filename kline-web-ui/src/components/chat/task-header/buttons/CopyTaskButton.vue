<template>
  <HeroTooltip content="Copy Text" placement="right">
    <button
      aria-label="Copy"
      :class="[
        'bg-transparent hover:opacity-100 border-0 cursor-pointer p-0 flex items-center justify-center rounded-sm',
        props.className
      ]"
      @click="handleCopy"
    >
      <svg
        v-if="copied"
        xmlns="http://www.w3.org/2000/svg"
        width="13"
        height="13"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <polyline points="20 6 9 17 4 12" />
      </svg>
      <svg
        v-else
        xmlns="http://www.w3.org/2000/svg"
        width="13"
        height="13"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <rect width="14" height="14" x="8" y="8" rx="2" ry="2" />
        <path d="M4 16c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2h8c1.1 0 2 .9 2 2" />
      </svg>
    </button>
  </HeroTooltip>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import HeroTooltip from '@/components/common/HeroTooltip.vue'

interface Props {
  taskText?: string
  className?: string
}

const props = defineProps<Props>()

const copied = ref(false)

const handleCopy = () => {
  if (!props.taskText) {
    return
  }

  navigator.clipboard.writeText(props.taskText).then(() => {
    copied.value = true
    setTimeout(() => {
      copied.value = false
    }, 1500)
  })
}
</script>

