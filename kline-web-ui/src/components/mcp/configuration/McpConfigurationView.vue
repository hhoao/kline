<template>
  <div class="fixed inset-0 flex flex-col">
    <div class="flex justify-between items-center px-5 pt-2.5 pb-1.25">
      <h3 class="m-0" :style="{ color: environmentColor }">MCP Servers</h3>
      <button
        type="button"
        class="px-4 py-2 rounded border cursor-pointer bg-[var(--vscode-button-background)] text-[var(--vscode-button-foreground)] border-[var(--vscode-button-background)] hover:opacity-90"
        @click="emit('done')"
      >
        Done
      </button>
    </div>
    <div class="flex-1 overflow-auto">
      <div
        class="flex gap-px px-5 border-b border-[var(--vscode-panel-border)]"
      >
        <McpTabButton
          v-if="mcpMarketplaceEnabled"
          :is-active="activeTab === 'marketplace'"
          @click="activeTab = 'marketplace'"
        >
          Marketplace
        </McpTabButton>
        <McpTabButton
          :is-active="activeTab === 'addRemote'"
          @click="activeTab = 'addRemote'"
        >
          Remote Servers
        </McpTabButton>
        <McpTabButton
          :is-active="activeTab === 'configure'"
          @click="activeTab = 'configure'"
        >
          Configure
        </McpTabButton>
      </div>
      <div class="w-full">
        <McpMarketplaceView v-if="mcpMarketplaceEnabled && activeTab === 'marketplace'" />
        <AddRemoteServerForm
          v-if="activeTab === 'addRemote'"
          @server-added="activeTab = 'configure'"
        />
        <ConfigureServersView v-if="activeTab === 'configure'" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import type { McpViewTab } from '@/shared/mcp'
import { useExtensionStateStore } from '@/stores/extensionState'
import { mcpService } from '@/api/mcp'
import McpTabButton from './tabs/McpTabButton.vue'
import ConfigureServersView from './tabs/installed/ConfigureServersView.vue'
import AddRemoteServerForm from './tabs/add-server/AddRemoteServerForm.vue'
import McpMarketplaceView from './tabs/marketplace/McpMarketplaceView.vue'

const props = withDefaults(
  defineProps<{ initialTab?: McpViewTab }>(),
  { initialTab: undefined }
)
const emit = defineEmits<{ done: [] }>()

const store = useExtensionStateStore()
const mcpMarketplaceEnabled = ref(store.extensionState?.mcpMarketplaceEnabled ?? false)
const activeTab = ref<McpViewTab>(
  props.initialTab ?? (mcpMarketplaceEnabled.value ? 'marketplace' : 'configure')
)

const environmentColor = 'var(--vscode-foreground)'

watch(
  () => store.extensionState?.mcpMarketplaceEnabled,
  (v) => {
    mcpMarketplaceEnabled.value = v ?? false
    if (!mcpMarketplaceEnabled.value && activeTab.value === 'marketplace') {
      activeTab.value = 'configure'
    }
  },
  { immediate: true }
)

onMounted(() => {
  if (mcpMarketplaceEnabled.value) {
    mcpService
      .refreshMcpMarketplace()
      .then((catalog) => store.setMcpMarketplaceCatalog(catalog))
      .catch((e) => console.error('Error refreshing MCP marketplace:', e))
    mcpService
      .getLatestMcpServers()
      .then((res) => store.updateMcpServers(res))
      .catch((e) => console.error('Failed to fetch MCP servers:', e))
  }
})
</script>
