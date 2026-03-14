<template>
  <HeroTooltip content="Open Conversation History File" placement="right">
    <button
      aria-label="Open Disk Conversation History"
      :class="[
        'flex items-center border-0 text-sm font-bold bg-transparent hover:opacity-100 cursor-pointer rounded-sm p-0',
        props.className
      ]"
      @click="handleOpenDiskConversationHistory"
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
        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
        <polyline points="7 10 12 15 17 10" />
        <line x1="12" y1="15" x2="12" y2="3" />
      </svg>
    </button>
  </HeroTooltip>
</template>

<script setup lang="ts">
import HeroTooltip from '@/components/common/HeroTooltip.vue'
import { fileService } from '@/api/file'

interface Props {
  taskId?: string
  className?: string
}

const props = defineProps<Props>()

const handleOpenDiskConversationHistory = () => {
  if (!props.taskId) {
    return
  }

  fileService.openDiskConversationHistory(props.taskId).catch(
    (err: any) => console.error('Error opening disk conversation history:', err)
  )
}
</script>

