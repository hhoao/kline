<template>
  <div class="shrink-0">
    <div
      class="cursor-pointer select-none flex items-center mx-5 my-2.5 text-[var(--vscode-descriptionForeground)] hover:opacity-80"
      @click="toggleExpanded"
    >
      <span
        :class="['codicon', isExpanded ? 'i-codicon:chevron-down' : 'i-codicon:chevron-right']"
        class="mr-1 scale-90"
      />
      <span class="mr-1 scale-90 i-codicon:comment-discussion" />
      <span class="font-medium text-[0.85em] uppercase">Recent Tasks</span>
    </div>

    <div v-if="isExpanded" class="px-5">
      <template v-if="filteredTaskHistory.length > 0">
        <div
          v-for="item in filteredTaskHistory.slice(0, 3)"
          :key="item.id"
          class="mb-3 cursor-pointer history-preview-item"
          @click="handleHistorySelect(item.id)"
        >
          <div class="p-3">
            <div class="mb-2">
              <span class="text-[var(--vscode-descriptionForeground)] font-medium text-[0.85em] uppercase">
                {{ formatDate(item.ts) }}
              </span>
            </div>
            <div
              v-if="item.isFavorited"
              class="absolute top-3 right-3 text-[var(--vscode-button-background)]"
            >
              <span aria-label="Favorited" class="i-codicon:star-full" />
            </div>

            <div
              :id="`history-preview-task-${item.id}`"
              class="history-preview-task text-[var(--vscode-font-size)] text-[var(--vscode-descriptionForeground)] mb-2 line-clamp-3 whitespace-pre-wrap break-words overflow-wrap-anywhere"
            >
              <span class="ph-no-capture">{{ item.task }}</span>
            </div>
            <div class="text-[0.85em] text-[var(--vscode-descriptionForeground)]">
              <span>
                Tokens: ↑{{ formatLargeNumber(item.tokensIn || 0) }} ↓{{ formatLargeNumber(item.tokensOut || 0) }}
              </span>
              <template v-if="item.cacheWrites">
                <span> • </span>
                <span>
                  Cache: +{{ formatLargeNumber(item.cacheWrites || 0) }} → {{ formatLargeNumber(item.cacheReads || 0) }}
                </span>
              </template>
              <template v-if="item.totalCost">
                <span> • </span>
                <span>API Cost: ${{ item.totalCost?.toFixed(4) }}</span>
              </template>
            </div>
          </div>
        </div>
        <div class="flex justify-center items-center">
          <button
            class="opacity-90 bg-[var(--vscode-button-background)] text-[var(--vscode-button-foreground)] border border-[var(--vscode-button-border)] px-4 py-2 rounded cursor-pointer transition-colors hover:bg-[var(--vscode-button-hoverBackground)]"
            aria-label="View all history"
            @click="showHistoryView"
          >
            <div class="text-[var(--vscode-font-size)] text-[var(--vscode-descriptionForeground)]">
              View all history
            </div>
          </button>
        </div>
      </template>
      <div
        v-else
        class="text-center text-[var(--vscode-descriptionForeground)] text-[var(--vscode-font-size)] py-2.5"
      >
        No recent tasks
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { taskService } from '@/api/task';
import { useExtensionStateStore } from "@/stores/extensionState";
import { formatLargeNumber } from '@/utils/format';
import { computed, ref } from 'vue';

interface Props {
  showHistoryView: () => void
}

const props = defineProps<Props>()

const extensionState = computed(() => useExtensionStateStore().extensionState)
const isExpanded = ref(true)

const taskHistory = computed(() => extensionState.value?.taskHistory || [])

const filteredTaskHistory = computed(() => {
  return taskHistory.value.filter((item) => item.ts && item.task)
})

const handleHistorySelect = (id: string) => {
  taskService.showTaskWithId(id).catch((error: any) =>
    console.error('Error showing task:', error)
  )
}

const toggleExpanded = () => {
  isExpanded.value = !isExpanded.value
}

const formatDate = (timestamp: number) => {
  const date = new Date(timestamp)
  return date
    ?.toLocaleString('en-US', {
      month: 'long',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
      hour12: true,
    })
    .replace(', ', ' ')
    .replace(' at', ',')
    .toUpperCase()
}
</script>

<style scoped>
.history-preview-item {
  background-color: color-mix(in srgb, var(--vscode-toolbar-hoverBackground) 65%, transparent);
  border-radius: 4px;
  position: relative;
  overflow: hidden;
  opacity: 0.8;
  cursor: pointer;
  margin-bottom: 12px;
}

.history-preview-item:hover {
  background-color: color-mix(in srgb, var(--vscode-toolbar-hoverBackground) 100%, transparent);
  opacity: 1;
  pointer-events: auto;
}

.history-preview-task {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>

