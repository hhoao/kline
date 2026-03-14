<template>
  <div
    v-if="items.length === 0"
    style="white-space: pre-wrap"
  >
    {{ text }}
  </div>
  <div
    v-else
    ref="containerRef"
    class="flex flex-col"
    :style="{
      gap: '2px',
      fontSize: '12px',
      lineHeight: '1.3',
      maxHeight: items.length >= 10 ? '200px' : 'auto',
      overflowY: items.length >= 10 ? 'auto' : 'visible',
    }"
    @scroll="handleScroll"
  >
    <div
      v-for="(item, index) in items"
      :key="`checklist-item-${index}`"
      class="flex gap-1.5 items-start p-0.5"
    >
      <span
        :class="[
          'text-xs shrink-0 mt-0.5',
          item.checked ? 'text-green-500' : 'text-[var(--vscode-foreground)]'
        ]"
      >
        <svg
          v-if="item.checked"
          width="10"
          height="10"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="20 6 9 17 4 12"></polyline>
        </svg>
        <svg
          v-else
          width="10"
          height="10"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <circle cx="12" cy="12" r="10"></circle>
        </svg>
      </span>
      <div
        :class="[
          'text-xs break-words flex-1',
          item.checked ? 'text-[var(--vscode-descriptionForeground)]' : 'text-[var(--vscode-foreground)]'
        ]"
        :style="{
          textDecoration: item.checked ? 'line-through' : 'none',
          lineHeight: '1.3',
        }"
      >
        <LightMarkdown :text="item.text" :compact="true" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import { parseFocusChainItem } from '@/shared/focus-chain-utils'
import LightMarkdown from './LightMarkdown.vue'

interface Props {
  text: string
}

const props = defineProps<Props>()

interface ChecklistItem {
  checked: boolean
  text: string
}

const containerRef = ref<HTMLDivElement | null>(null)
const lastCompletedIndex = ref(-1)
const isUserScrolling = ref(false)
let scrollTimeoutRef: ReturnType<typeof setTimeout> | null = null

const parseChecklistItems = (text: string): ChecklistItem[] => {
  const lines = text.split('\n').filter((line) => line.trim())
  const items: ChecklistItem[] = []

  for (const line of lines) {
    const trimmedLine = line.trim()
    const parsed = parseFocusChainItem(trimmedLine)
    if (parsed) {
      items.push({ checked: parsed.checked, text: parsed.text })
    }
  }

  return items
}

const items = computed(() => parseChecklistItems(props.text))

const handleScroll = () => {
  isUserScrolling.value = true
  if (scrollTimeoutRef) {
    clearTimeout(scrollTimeoutRef)
  }
  scrollTimeoutRef = setTimeout(() => {
    isUserScrolling.value = false
  }, 1000)
}

watch([items, () => isUserScrolling.value], () => {
  if (items.value.length >= 10 && containerRef.value && !isUserScrolling.value) {
    let currentLastCompletedIndex = -1
    for (let i = items.value.length - 1; i >= 0; i--) {
      if (items.value[i].checked) {
        currentLastCompletedIndex = i
        break
      }
    }

    if (currentLastCompletedIndex >= 0 && currentLastCompletedIndex !== lastCompletedIndex.value) {
      lastCompletedIndex.value = currentLastCompletedIndex

      const container = containerRef.value
      const itemElements = container.children
      if (itemElements[currentLastCompletedIndex]) {
        itemElements[currentLastCompletedIndex].scrollIntoView({
          behavior: 'smooth',
          block: 'start',
        })
      }
    }
  }
}, { immediate: true })

onBeforeUnmount(() => {
  if (scrollTimeoutRef) {
    clearTimeout(scrollTimeoutRef)
  }
})
</script>

