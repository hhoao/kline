<template>
  <div class="flex flex-1 flex-col overflow-hidden">
    <div class="flex justify-between items-center gap-2 px-5 pt-2 pb-1">
      <h3 class="m-0" :style="{ color: titleColor }">Settings</h3>
      <button
        type="button"
        class="px-4 py-2 rounded border cursor-pointer bg-[var(--vscode-button-background)] text-[var(--vscode-button-foreground)] hover:opacity-90"
        @click="emit('done')"
      >
        Done
      </button>
    </div>
    <div ref="containerRef" class="flex flex-1 overflow-hidden" :class="{ narrow: isCompactMode }">
      <div
        class="w-48 flex-shrink-0 flex flex-col overflow-y-auto overflow-x-hidden border-r border-[var(--vscode-sideBar-background)]"
      >
        <SettingsTabButton
          v-for="tab in visibleTabs"
          :key="tab.id"
          :tab-id="tab.id"
          :is-active="activeTab === tab.id"
          :compact="isCompactMode"
          :title="tab.tooltipText ?? tab.headerText ?? tab.name"
          @click="activeTab = tab.id"
        >
          <span class="flex items-center gap-2">
            <i :class="[iconClass(tab), 'w-4 h-4 flex-shrink-0']" />
            <span class="tab-label">{{ tab.name }}</span>
          </span>
        </SettingsTabButton>
      </div>
      <div class="flex-1 overflow-auto">
        <ApiConfigurationSection v-if="activeTab === 'api-config'" />
        <GeneralSettingsSection v-else-if="activeTab === 'general'" />
        <FeatureSettingsSection v-else-if="activeTab === 'features'" />
        <BrowserSettingsSection v-else-if="activeTab === 'browser'" />
        <TerminalSettingsSection v-else-if="activeTab === 'terminal'" />
        <AboutSection v-else-if="activeTab === 'about'" :version="version" />
        <DebugSection v-else-if="activeTab === 'debug'" @reset-state="handleResetState" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useExtensionStateStore } from '@/stores/extensionState'
import SettingsTabButton from './SettingsTabButton.vue'
import ApiConfigurationSection from './sections/ApiConfigurationSection.vue'
import GeneralSettingsSection from './sections/GeneralSettingsSection.vue'
import FeatureSettingsSection from './sections/FeatureSettingsSection.vue'
import BrowserSettingsSection from './sections/BrowserSettingsSection.vue'
import TerminalSettingsSection from './sections/TerminalSettingsSection.vue'
import AboutSection from './sections/AboutSection.vue'
import DebugSection from './sections/DebugSection.vue'

interface SettingsTab {
  id: string
  name: string
  headerText: string
  tooltipText?: string
  icon: string
}
const SETTINGS_TABS: SettingsTab[] = [
  { id: 'api-config', name: 'API Configuration', headerText: 'API Configuration', icon: 'SlidersHorizontal' },
  { id: 'features', name: 'Features', headerText: 'Feature Settings', icon: 'CheckCheck' },
  { id: 'browser', name: 'Browser', headerText: 'Browser Settings', icon: 'SquareMousePointer' },
  { id: 'terminal', name: 'Terminal', headerText: 'Terminal Settings', icon: 'SquareTerminal' },
  { id: 'general', name: 'General', headerText: 'General Settings', icon: 'Wrench' },
  { id: 'about', name: 'About', headerText: 'About', tooltipText: 'About Cline', icon: 'Info' },
]
const IS_DEV = import.meta.env.DEV
if (IS_DEV) {
  SETTINGS_TABS.splice(4, 0, { id: 'debug', name: 'Debug', headerText: 'Debug', tooltipText: 'Debug Tools', icon: 'FlaskConical' })
}

const props = withDefaults(
  defineProps<{ targetSection?: string }>(),
  { targetSection: undefined }
)
const emit = defineEmits<{ done: [] }>()

const store = useExtensionStateStore()
const containerRef = ref<HTMLElement | null>(null)
const activeTab = ref(props.targetSection ?? SETTINGS_TABS[0].id)
const isCompactMode = ref(true)

const visibleTabs = computed(() => SETTINGS_TABS.filter((t) => t.id !== 'debug' || IS_DEV))

const version = computed(() => store.extensionState?.version ?? '')

const titleColor = 'var(--vscode-foreground)'

const ICON_CLASS: Record<string, string> = {
  SlidersHorizontal: 'codicon codicon-settings-gear',
  CheckCheck: 'codicon codicon-check-all',
  SquareMousePointer: 'codicon codicon-device-mobile',
  SquareTerminal: 'codicon codicon-terminal',
  Wrench: 'codicon codicon-tools',
  Info: 'codicon codicon-info',
  FlaskConical: 'codicon codicon-beaker',
}
function iconClass(tab: SettingsTab) {
  return ICON_CLASS[tab.icon] ?? 'codicon codicon-settings-gear'
}

watch(
  () => props.targetSection,
  (v) => { if (v) activeTab.value = v },
  { immediate: true }
)

let resizeObserver: ResizeObserver | null = null
onMounted(() => {
  const el = containerRef.value
  if (!el) return
  const checkCompact = () => {
    isCompactMode.value = el.offsetWidth < 500
  }
  resizeObserver = new ResizeObserver(checkCompact)
  resizeObserver.observe(el)
  checkCompact()
})
onUnmounted(() => {
  resizeObserver?.disconnect()
})

async function handleResetState(global?: boolean) {
  try {
    const reset = (store as { resetState?: (global?: boolean) => Promise<unknown> }).resetState
    await (reset?.(global) ?? Promise.resolve())
  } catch (e) {
    console.error('Failed to reset state:', e)
  }
}
</script>

<style scoped>
.narrow :deep(.tab-label) {
  display: none;
}
</style>
