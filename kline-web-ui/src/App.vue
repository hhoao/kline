<template>
  <div class="flex overflow-hidden w-full h-screen relative">
    <McpConfigurationView
      v-if="showMcp"
      :initial-tab="mcpTab"
      @done="closeMcpView"
    />
    <div :class="{ hidden: showMcp }" class="flex-1 min-w-0 min-h-0">
      <RouterView />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useExtensionStateStore } from './stores/extensionState'
import { McpConfigurationView } from '@/components/mcp'

const store = useExtensionStateStore()
const showMcp = computed(() => store.showMcp)
const mcpTab = computed(() => store.mcpTab)

function closeMcpView() {
  store.showMcp = false
  store.setMcpTab(undefined)
}

onMounted(() => {
  store.init()
})
</script>
