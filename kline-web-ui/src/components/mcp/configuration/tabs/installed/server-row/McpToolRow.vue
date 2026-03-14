<template>
  <div class="py-0.75">
    <div
      class="flex items-center justify-between"
      data-testid="tool-row-container"
      @click.stop
    >
      <div class="flex items-center">
        <span class="i-codicon:symbol-method mr-1.5" />
        <span class="font-medium">{{ tool.name }}</span>
      </div>
      <label
        v-if="serverName && showAutoApprove"
        class="flex items-center gap-1.5 cursor-pointer text-[13px]"
      >
        <input
          type="checkbox"
          :checked="tool.autoApprove ?? false"
          :data-tool="tool.name"
          @change="handleAutoApproveChange"
        />
        Auto-approve
      </label>
    </div>
    <div
      v-if="tool.description"
      class="ml-0 mt-1 opacity-80 text-[12px]"
    >
      {{ tool.description }}
    </div>
    <div
      v-if="hasParams"
      class="mt-2 text-[12px] border border-[var(--vscode-descriptionForeground)]/30 rounded px-2 py-2"
    >
      <div class="mb-1 opacity-80 text-[11px] uppercase">Parameters</div>
      <div
        v-for="(schema, paramName) in paramEntries"
        :key="paramName"
        class="flex items-baseline mt-1"
      >
        <code class="text-[var(--vscode-textPreformat-foreground)] mr-2">
          {{ paramName }}
          <span v-if="isRequired(paramName)" class="text-[var(--vscode-errorForeground)]">*</span>
        </code>
        <span class="opacity-80 break-words">
          {{ (schema as { description?: string }).description || 'No description' }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { McpTool } from '@/shared/mcp'
import { useExtensionStateStore } from '@/stores/extensionState'
import { mcpService } from '@/api/mcp'

const props = defineProps<{
  tool: McpTool
  serverName?: string
}>()

const store = useExtensionStateStore()
const autoApprovalSettings = computed(
  () => store.extensionState?.autoApprovalSettings
)
const showAutoApprove = computed(
  () =>
    autoApprovalSettings.value?.enabled &&
    autoApprovalSettings.value?.actions?.useMcp
)

const hasParams = computed(() => {
  const schema = props.tool.inputSchema as { properties?: Record<string, unknown> } | undefined
  return schema && 'properties' in schema && schema.properties && Object.keys(schema.properties).length > 0
})

const paramEntries = computed(() => {
  const schema = props.tool.inputSchema as { properties?: Record<string, unknown> } | undefined
  return schema && 'properties' in schema ? schema.properties || {} : {}
})

function isRequired(paramName: string): boolean {
  const schema = props.tool.inputSchema as { required?: string[] } | undefined
  return (
    !!schema &&
    'required' in schema &&
    Array.isArray(schema.required) &&
    schema.required.includes(paramName)
  )
}

function handleAutoApproveChange() {
  if (!props.serverName) return
  mcpService
    .toggleToolAutoApprove({
      serverName: props.serverName,
      toolNames: [props.tool.name],
      autoApprove: !(props.tool.autoApprove ?? false),
    })
    .then((res) => store.updateMcpServers(res))
    .catch((e) => console.error('Error toggling tool auto-approve', e))
}
</script>
