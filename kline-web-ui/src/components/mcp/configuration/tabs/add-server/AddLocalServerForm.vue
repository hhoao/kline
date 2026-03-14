<template>
  <div class="py-4 px-5 flex flex-col gap-2">
    <div class="text-[var(--vscode-foreground)] text-[13px]">
      Add a local MCP server by configuring it in <code class="px-1 py-0.5 rounded bg-[var(--vscode-textCodeBlock-background)]">cline_mcp_settings.json</code>.
      You'll need to specify the server name, command, arguments, and any required environment variables in the JSON configuration.
      Learn more
      <a
        href="https://modelcontextprotocol.io/docs/concepts/servers"
        class="text-[var(--vscode-textLink-foreground)]"
        target="_blank"
        rel="noopener noreferrer"
      >here.</a>
    </div>
    <button
      type="button"
      class="w-full mb-1.25 mt-2 px-4 py-2 rounded border cursor-pointer bg-[var(--vscode-button-background)] text-[var(--vscode-button-foreground)] hover:opacity-90"
      @click="openMcpSettings"
    >
      Open cline_mcp_settings.json
    </button>
  </div>
</template>

<script setup lang="ts">
import { mcpService } from '@/api/mcp'

defineEmits<{ serverAdded: [] }>()

async function openMcpSettings() {
  try {
    await mcpService.openMcpSettings()
  } catch (e) {
    console.error('Error opening MCP settings:', e)
  }
}
</script>
