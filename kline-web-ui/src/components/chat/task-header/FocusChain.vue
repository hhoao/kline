<template>
  <div
    v-if="todoInfo"
    class="relative rounded-sm bg-[var(--vscode-toolbar-hoverBackground)]/65 flex flex-col gap-1.5 select-none hover:bg-[var(--vscode-toolbar-hoverBackground)] overflow-hidden opacity-80 hover:opacity-100 transition-[transform,box-shadow] duration-200 cursor-pointer"
    :title="CLICK_TO_EDIT_TITLE"
    @click="handleToggle"
  >
    <!-- ToDoListHeader -->
    <div
      :class="[
        'relative w-full h-full',
        {
          'text-[var(--vscode-testing-iconPassed)]': isCompleted,
        },
      ]"
    >
      <div
        :class="[
          'absolute bottom-0 left-0 transition-[width] duration-300 ease-in-out pointer-events-none z-[1] h-1 bg-[var(--vscode-testing-iconPassed)]',
          {
            'opacity-0': todoInfo.progressPercentage === 0 || todoInfo.progressPercentage === 100,
          },
        ]"
        :style="{ width: `${todoInfo.progressPercentage}%` }"
      />
      <div class="flex z-10 gap-2 justify-between items-center px-1.5 py-2.5">
        <div class="flex flex-1 gap-1.5 items-center min-w-0">
          <span
            :class="[
              'rounded-lg px-2 py-0.25 text-xs inline-block shrink-0 bg-[var(--vscode-badge-foreground)]/20 text-[var(--vscode-foreground)]',
              {
                'bg-[var(--vscode-testing-iconPassed)] text-black': isCompleted,
              },
            ]"
          >
            {{ todoInfo.currentIndex }}/{{ todoInfo.totalCount }}
          </span>
          <div class="header-text text-xs font-medium break-words overflow-hidden text-ellipsis whitespace-nowrap max-w-[calc(100%-60px)]">
            <LightMarkdown :compact="true" :text="displayText" />
          </div>
        </div>
        <div class="flex items-center justify-between text-[var(--vscode-foreground)]">
          <svg
            v-if="isExpanded"
            xmlns="http://www.w3.org/2000/svg"
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            class="ml-0.25"
          >
            <polyline points="6 9 12 15 18 9" />
          </svg>
          <svg
            v-else
            xmlns="http://www.w3.org/2000/svg"
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <polyline points="9 18 15 12 9 6" />
          </svg>
        </div>
      </div>
    </div>
    <div
      v-if="isExpanded"
      class="relative px-1 pb-2 mx-1"
      @click="handleEditClick"
    >
      <ChecklistRenderer :text="props.lastProgressMessageText!" />
      <div
        v-if="isCompleted"
        class="mt-2 text-xs font-semibold text-[var(--vscode-descriptionForeground)]"
      >
        {{ NEW_STEPS_MESSAGE }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { isCompletedFocusChainItem, isFocusChainItem } from '@/shared/focus-chain-utils'
import ChecklistRenderer from '@/components/common/ChecklistRenderer.vue'
import LightMarkdown from '@/components/common/LightMarkdown.vue'
import { fileService } from '@/api/file'

interface TodoInfo {
  readonly currentTodo: { text: string; completed: boolean; index: number } | null
  readonly currentIndex: number
  readonly completedCount: number
  readonly totalCount: number
  readonly progressPercentage: number
}

interface Props {
  readonly lastProgressMessageText?: string
  readonly currentTaskItemId?: string
}

const props = defineProps<Props>()

const COMPLETED_MESSAGE = 'All tasks have been completed!'
const TODO_LIST_LABEL = 'To-Do list'
const NEW_STEPS_MESSAGE = 'New steps will be generated if you continue the task'
const CLICK_TO_EDIT_TITLE = 'Click to edit to-do list in file'

const isExpanded = ref(false)

const todoInfoCache = new Map<string, TodoInfo | null>()
const MAX_CACHE_SIZE = 100

const parseCurrentTodoInfo = (text: string): TodoInfo | null => {
  if (!text) {
    return null
  }

  const cached = todoInfoCache.get(text)
  if (cached !== undefined) {
    return cached
  }

  let completedCount = 0
  let totalCount = 0
  let firstIncompleteIndex = -1
  let firstIncompleteText: string | null = null

  let lineStart = 0
  let lineEnd = text.indexOf('\n')

  while (lineStart < text.length) {
    const line = lineEnd === -1 ? text.substring(lineStart).trim() : text.substring(lineStart, lineEnd).trim()

    if (isFocusChainItem(line)) {
      const isCompleted = isCompletedFocusChainItem(line)

      if (isCompleted) {
        completedCount++
      } else if (firstIncompleteIndex === -1) {
        firstIncompleteIndex = totalCount
        firstIncompleteText = line.substring(5).trim()
      }

      totalCount++
    }

    if (lineEnd === -1) {
      break
    }
    lineStart = lineEnd + 1
    lineEnd = text.indexOf('\n', lineStart)
  }

  if (totalCount === 0) {
    todoInfoCache.set(text, null)
    return null
  }

  const currentTodo = firstIncompleteText ? { text: firstIncompleteText, completed: false, index: firstIncompleteIndex } : null

  const result: TodoInfo = {
    currentTodo,
    currentIndex: firstIncompleteIndex >= 0 ? firstIncompleteIndex + 1 : totalCount,
    completedCount,
    totalCount,
    progressPercentage: (completedCount / totalCount) * 100,
  }

  if (todoInfoCache.size >= MAX_CACHE_SIZE) {
    const firstKey = todoInfoCache.keys().next().value
    if (firstKey) {
      todoInfoCache.delete(firstKey)
    }
  }
  todoInfoCache.set(text, result)
  return result
}

const todoInfo = computed(() => {
  return props.lastProgressMessageText ? parseCurrentTodoInfo(props.lastProgressMessageText) : null
})

const isCompleted = computed(() => {
  return todoInfo.value ? todoInfo.value.completedCount === todoInfo.value.totalCount : false
})

const displayText = computed(() => {
  return isCompleted.value ? COMPLETED_MESSAGE : todoInfo.value?.currentTodo?.text || TODO_LIST_LABEL
})

const handleToggle = () => {
  isExpanded.value = !isExpanded.value
}

const handleEditClick = (e: MouseEvent) => {
  e.preventDefault()
  e.stopPropagation()
  if (props.currentTaskItemId) {
    fileService.openFocusChainFile(props.currentTaskItemId).catch(
      (err: any) => console.error('Error opening focus chain file:', err)
    )
  }
}
</script>
