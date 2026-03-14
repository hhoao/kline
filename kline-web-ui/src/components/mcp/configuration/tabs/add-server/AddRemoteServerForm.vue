<template>
  <div class="p-4 px-5">
    <div class="text-[var(--vscode-foreground)] mb-2">
      Add a remote MCP server by providing a name and its URL endpoint. Learn more
      <a
        href="https://modelcontextprotocol.io/docs/concepts/transports#sse-transport"
        class="text-[var(--vscode-textLink-foreground)]"
        target="_blank"
        rel="noopener noreferrer"
      >here.</a>
    </div>
    <form @submit.prevent="handleSubmit">
      <div class="mb-2">
        <label class="block text-[13px] mb-1">Server Name</label>
        <input
          v-model="serverName"
          type="text"
          placeholder="mcp-server"
          class="w-full px-2 py-1.5 rounded border border-[var(--vscode-input-border)] bg-[var(--vscode-input-background)] text-[var(--vscode-input-foreground)]"
          :disabled="isSubmitting"
          @input="error = ''"
        />
      </div>
      <div class="mb-2">
        <label class="block text-[13px] mb-1">Server URL</label>
        <input
          v-model="serverUrl"
          type="text"
          placeholder="https://example.com/mcp-server"
          class="w-full px-2 py-1.5 rounded border border-[var(--vscode-input-border)] bg-[var(--vscode-input-background)] text-[var(--vscode-input-foreground)]"
          :disabled="isSubmitting"
          @input="error = ''"
        />
      </div>
      <div v-if="error" class="mb-3 text-[var(--vscode-errorForeground)]">
        {{ error }}
      </div>
      <div class="flex items-center mt-3 w-full gap-3">
        <button
          type="submit"
          class="flex-1 px-4 py-2 rounded border cursor-pointer bg-[var(--vscode-button-background)] text-[var(--vscode-button-foreground)] disabled:opacity-50"
          :disabled="isSubmitting"
        >
          {{ isSubmitting ? 'Adding...' : 'Add Server' }}
        </button>
        <span
          v-if="showConnectingMessage"
          class="text-[var(--vscode-descriptionForeground)] text-sm"
        >
          Connecting to server... This may take a few seconds.
        </span>
      </div>
      <button
        type="button"
        class="w-full mb-1.25 mt-4 px-4 py-2 rounded border cursor-pointer bg-[var(--vscode-button-secondaryBackground)] text-[var(--vscode-button-foreground)] hover:opacity-90"
        @click="openMcpSettings"
      >
        Edit Configuration
      </button>
    </form>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useExtensionStateStore } from '@/stores/extensionState'
import { mcpService } from '@/api/mcp'

const emit = defineEmits<{ serverAdded: [] }>()
const store = useExtensionStateStore()

const serverName = ref('')
const serverUrl = ref('')
const isSubmitting = ref(false)
const error = ref('')
const showConnectingMessage = ref(false)

async function handleSubmit() {
  const name = serverName.value.trim()
  const url = serverUrl.value.trim()
  if (!name) {
    error.value = 'Server name is required'
    return
  }
  if (!url) {
    error.value = 'Server URL is required'
    return
  }
  try {
    new URL(url)
  } catch {
    error.value = 'Invalid URL format'
    return
  }
  error.value = ''
  isSubmitting.value = true
  showConnectingMessage.value = true
  try {
    const res = await mcpService.addRemoteMcpServer({ serverName: name, serverUrl: url })
    store.updateMcpServers(res)
    serverName.value = ''
    serverUrl.value = ''
    emit('serverAdded')
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to add server'
  } finally {
    isSubmitting.value = false
    showConnectingMessage.value = false
  }
}

async function openMcpSettings() {
  try {
    await mcpService.openMcpSettings()
  } catch (e) {
    console.error('Error opening MCP settings:', e)
  }
}
</script>
