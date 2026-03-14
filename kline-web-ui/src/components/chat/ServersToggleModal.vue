<template>
  <div ref="modalRef">
    <Tooltip tip-text="Manage MCP Servers" :visible="isVisible ? false : undefined">
      <button
        type="button"
        ref="buttonRef"
        class="inline-flex min-w-0 max-w-full p-0 h-5 items-center gap-1 text-xs whitespace-nowrap bg-transparent border-none cursor-pointer text-[var(--vscode-foreground)]"
        :aria-label="isVisible ? 'Hide MCP Servers' : 'Show MCP Servers'"
        @click="isVisible = !isVisible"
      >
        <span class="i-codicon:server flex items-center text-[12.5px] mb-px" />
      </button>
    </Tooltip>
    <Teleport to="body">
      <div
        v-if="isVisible"
        ref="popoverRef"
        class="fixed left-4 right-4 border border-[var(--vscode-editorGroup-border)] p-3 rounded z-[1000] overflow-y-auto max-h-[calc(100vh-100px)]"
        :style="{
          bottom: `calc(100vh - ${menuPosition}px + 6px)`,
          background: codeBlockBgColor,
        }"
        @click.stop
      >
        <div class="flex justify-between items-center mb-2.5">
          <div class="m-0 text-base font-semibold">MCP Servers</div>
          <button
            type="button"
            class="p-1 rounded border-none cursor-pointer bg-transparent hover:bg-black/10"
            aria-label="Go to MCP server settings"
            @click="openConfigure"
          >
            <span class="i-codicon:gear text-[10px]" />
          </button>
        </div>
        <div class="mb-[-10px]">
          <ServersToggleList
            :servers="mcpServers"
            :has-trash-icon="false"
            :is-expandable="false"
            list-gap="small"
          />
        </div>
      </div>
    </Teleport>
    <div
      v-if="isVisible"
      class="fixed inset-0 z-[999]"
      @click="isVisible = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { CODE_BLOCK_BG_COLOR } from '@/components/common/CodeBlock'
import Tooltip from '@/components/common/Tooltip.vue'
import ServersToggleList from '@/components/mcp/configuration/tabs/installed/ServersToggleList.vue'
import { useExtensionStateStore } from '@/stores/extensionState'
import { mcpService } from '@/api/mcp'

const store = useExtensionStateStore()
const isVisible = ref(false)
const buttonRef = ref<HTMLElement | null>(null)
const modalRef = ref<HTMLElement | null>(null)
const popoverRef = ref<HTMLElement | null>(null)
const menuPosition = ref(0)

const codeBlockBgColor = CODE_BLOCK_BG_COLOR
const mcpServers = computed(() => store.mcpServers || [])

function updatePosition() {
  if (isVisible.value && buttonRef.value) {
    const rect = buttonRef.value.getBoundingClientRect()
    menuPosition.value = rect.top + 1
  }
}

watch(isVisible, (v) => {
  if (v) {
    mcpService.getLatestMcpServers().then((res) => store.updateMcpServers(res)).catch((e) => console.error('Failed to fetch MCP servers', e))
    updatePosition()
  }
})

onMounted(() => {
  window.addEventListener('resize', updatePosition)
})
onUnmounted(() => {
  window.removeEventListener('resize', updatePosition)
})

function openConfigure() {
  isVisible.value = false
  store.showMcp = true
  store.setMcpTab('configure')
}
</script>
