<template>
  <div class="px-5 py-4">
    <div
      class="text-[var(--vscode-foreground)] text-[13px] mb-4 mt-1.25"
    >
      The
      <a
        href="https://github.com/modelcontextprotocol"
        class="text-[var(--vscode-textLink-foreground)]"
        target="_blank"
        rel="noopener noreferrer"
      >Model Context Protocol</a>
      enables communication with locally running MCP servers that provide additional tools and resources. You can use
      <a
        href="https://github.com/modelcontextprotocol/servers"
        class="text-[var(--vscode-textLink-foreground)]"
        target="_blank"
        rel="noopener noreferrer"
      >community-made servers</a>
      or ask the assistant to create new tools specific to your workflow.
    </div>
    <ServersToggleList :servers="servers" :has-trash-icon="false" :is-expandable="true" />
    <div class="mb-5 mt-2.5">
      <button
        type="button"
        class="w-full mb-1.25 px-4 py-2 rounded border cursor-pointer bg-[var(--vscode-button-secondaryBackground)] text-[var(--vscode-button-foreground)] border-[var(--vscode-button-border)] hover:opacity-90 flex items-center justify-center gap-1.5"
        @click="openMcpSettings"
      >
        <span class="i-codicon:server text-base" />
        Configure MCP Servers
      </button>
      <div class="text-center">
        <button
          type="button"
          class="text-[12px] text-[var(--vscode-textLink-foreground)] bg-transparent border-none cursor-pointer underline"
          @click="navigateToAdvancedSettings"
        >
          Advanced MCP Settings
        </button>
      </div>
      <p class="text-[12px] text-[var(--vscode-descriptionForeground)] mt-2 mb-0">
        To add a local MCP server, configure <code class="px-0.5 rounded bg-[var(--vscode-textCodeBlock-background)]">cline_mcp_settings.json</code> and use the button above to open it.
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useExtensionStateStore } from '@/stores/extensionState'
import { mcpService } from '@/api/mcp'
import { uiService } from '@/api/ui'
import ServersToggleList from './ServersToggleList.vue'

const store = useExtensionStateStore()
const servers = computed(() => store.mcpServers)

async function openMcpSettings() {
  try {
    await mcpService.openMcpSettings()
  } catch (e) {
    console.error('Error opening MCP settings:', e)
  }
}

async function navigateToAdvancedSettings() {
  store.showSettings = true
  try {
    await uiService.scrollToSettings('features')
  } catch (e) {
    console.error('Error scrolling to mcp settings:', e)
  }
}
</script>
