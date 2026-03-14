<template>
  <nav
    id="cline-navbar-container"
    class="inline-flex z-10 flex-none gap-1 justify-end items-center mr-4 mb-1 bg-transparent border-none"
    style="gap: 4px"
  >
    <HeroTooltip
      v-for="tab in settingsTabs"
      :key="`navbar-tooltip-${tab.id}`"
      :content="tab.tooltip"
      placement="bottom"
    >
      <button
        :aria-label="tab.tooltip"
        :data-testid="`tab-${tab.id}`"
        class="flex gap-1 items-center p-0 w-full min-w-0 h-5 text-xs whitespace-nowrap bg-transparent border-0 cursor-pointer"
        @click="tab.navigate"
      >
        <component :is="tab.iconComponent" />
      </button>
    </HeroTooltip>
  </nav>
</template>

<script setup lang="ts">
import { computed, h } from 'vue'
import { useRouter } from 'vue-router'
import HeroTooltip from '@/components/common/HeroTooltip.vue'
import { taskService } from '@/api/task'
import { useExtensionStateStore } from '@/stores/extensionState'

// Icon components using render functions
const PlusIcon = () =>
  h('svg', {
    xmlns: 'http://www.w3.org/2000/svg',
    width: 18,
    height: 18,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    'stroke-width': 1,
    'stroke-linecap': 'round',
    'stroke-linejoin': 'round',
    class: 'text-[var(--vscode-foreground)]',
  }, [
    h('line', { x1: 12, y1: 5, x2: 12, y2: 19 }),
    h('line', { x1: 5, y1: 12, x2: 19, y2: 12 }),
  ])

const HistoryIcon = () =>
  h('svg', {
    xmlns: 'http://www.w3.org/2000/svg',
    width: 18,
    height: 18,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    'stroke-width': 1,
    'stroke-linecap': 'round',
    'stroke-linejoin': 'round',
    class: 'text-[var(--vscode-foreground)]',
  }, [
    h('circle', { cx: 12, cy: 12, r: 10 }),
    h('polyline', { points: '12 6 12 12 16 14' }),
  ])

const SettingsIcon = () =>
  h('svg', {
    xmlns: 'http://www.w3.org/2000/svg',
    width: 18,
    height: 18,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    'stroke-width': 1,
    'stroke-linecap': 'round',
    'stroke-linejoin': 'round',
    class: 'text-[var(--vscode-foreground)]',
  }, [
    h('circle', { cx: 12, cy: 12, r: 3 }),
    h('path', { d: 'M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z' }),
  ])

const router = useRouter()
const extensionStateStore = useExtensionStateStore()

// Navigation functions
const navigateToChat = async () => {
  try {
    const currentTaskId = extensionStateStore.conversationId
    if (currentTaskId) {
      await taskService.clearTask(currentTaskId)
    }
  } catch (error: any) {
    console.error('Failed to clear task:', error)
  } finally {
    router.push({ name: 'Chat' })
  }
}

const navigateToHistory = () => {
  router.push({ name: 'History' })
}

const navigateToSettings = () => {
  router.push({ name: 'Settings' })
}

const settingsTabs = computed(() => [
  {
    id: 'chat',
    name: 'Chat',
    tooltip: 'New Task',
    iconComponent: PlusIcon,
    navigate: navigateToChat,
  },
  {
    id: 'history',
    name: 'History',
    tooltip: 'History',
    iconComponent: HistoryIcon,
    navigate: navigateToHistory,
  },
  {
    id: 'settings',
    name: 'Settings',
    tooltip: 'Settings',
    iconComponent: SettingsIcon,
    navigate: navigateToSettings,
  },
])
</script>

