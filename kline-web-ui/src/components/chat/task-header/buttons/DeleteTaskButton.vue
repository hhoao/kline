<template>
  <HeroTooltip :content="tooltipContent" placement="right">
    <button
      aria-label="Delete Task"
      :class="[
        'flex items-center border-0 text-sm font-bold bg-transparent hover:opacity-100 p-0 cursor-pointer rounded-sm',
        props.className
      ]"
      @click="handleDelete"
    >
      <svg
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
        <path d="M3 6h18" />
        <path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6" />
        <path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2" />
      </svg>
    </button>
  </HeroTooltip>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import HeroTooltip from '@/components/common/HeroTooltip.vue'
import { formatSize } from '@/utils/format.ts'
import { taskService } from '@/api/task'

interface Props {
  taskId?: string
  taskSize?: number
  className?: string
}

const props = defineProps<Props>()

const tooltipContent = computed(() => {
  return `Delete Task (size: ${props.taskSize ? formatSize(props.taskSize) : '--'})`
})

const handleDelete = () => {
  if (props.taskId) {
    taskService.deleteTasksWithIds([props.taskId]).catch(
      (err: any) => console.error('Error deleting task:', err)
    )
  }
}
</script>

