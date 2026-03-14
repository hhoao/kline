<template>
  <div v-if="shouldShowQuickWins" class="px-4 pt-1 pb-3 select-none">
    <h2 class="mb-2.5 text-sm font-medium text-center text-gray">
      Quick <span class="text-white">[Wins]</span> with Cline
    </h2>
    <div class="flex flex-col space-y-1">
      <QuickWinCard
        v-for="task in quickWinTasks"
        :key="task.id"
        :task="task"
        @execute="() => handleExecuteQuickWin(task.prompt)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { taskService } from '@/api/task';
import QuickWinCard from './QuickWinCard.vue';
import { quickWinTasks } from './quickWinTasks';

interface Props {
  shouldShowQuickWins: boolean
}

defineProps<Props>()

const handleExecuteQuickWin = async (prompt: string) => {
  try {
    await taskService.newTask({ text: prompt, images: [], files: [] })
  } catch (error) {
    console.error('Error executing quick win:', error)
  }
}
</script>

