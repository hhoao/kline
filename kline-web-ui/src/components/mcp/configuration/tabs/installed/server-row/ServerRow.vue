<template>
  <div class="mb-2.5">
    <div
      :class="[
        'flex items-center p-2 rounded',
        server.error ? '' : isExpandable ? 'cursor-pointer' : '',
        isExpanded || server.error ? 'rounded-b-none' : '',
        server.disabled ? 'opacity-60' : ''
      ]"
      style="background: var(--vscode-textCodeBlock-background)"
      @click="handleRowClick"
    >
      <span
        v-if="!server.error && isExpandable"
        :class="['mr-2', isExpanded ? 'i-codicon:chevron-down' : 'i-codicon:chevron-right']"
      />
      <span
        class="flex-1 overflow-hidden break-all whitespace-normal flex items-center mr-1"
      >
        {{ displayName }}
      </span>
      <div v-if="!server.error" class="flex items-center gap-1 ml-2">
        <button
          type="button"
          class="p-1 rounded border-none cursor-pointer bg-transparent hover:bg-black/10 disabled:opacity-50"
          :disabled="server.status === 'connecting' || isRestarting"
          :title="'Restart Server'"
          @click.stop="handleRestart"
        >
          <span class="i-codicon:sync" />
        </button>
        <button
          v-if="hasTrashIcon"
          type="button"
          class="p-1 rounded border-none cursor-pointer bg-transparent hover:bg-black/10 disabled:opacity-50"
          :disabled="isDeleting"
          title="Delete Server"
          @click.stop="handleDelete"
        >
          <span class="i-codicon:trash" />
        </button>
      </div>
      <div
        class="flex items-center ml-2"
        role="switch"
        :aria-checked="!server.disabled"
        tabindex="0"
        @click.stop="handleToggleMcpServer"
        @keydown.enter.space.prevent="handleToggleMcpServer"
      >
        <div
          :class="[
            'w-5 h-2.5 rounded-full cursor-pointer transition-colors relative',
            server.disabled ? 'opacity-50' : 'opacity-90'
          ]"
          :style="{
            backgroundColor: server.disabled
              ? 'var(--vscode-titleBar-inactiveForeground)'
              : 'var(--vscode-testing-iconPassed)',
          }"
        >
          <div
            class="absolute top-0.5 w-1.5 h-1.5 rounded-full bg-white border border-gray-500/65 transition-[left]"
            :style="{ left: server.disabled ? '2px' : '12px' }"
          />
        </div>
      </div>
      <div
        class="w-2 h-2 rounded-full ml-2"
        :style="{ background: statusColor }"
      />
    </div>

    <template v-if="server.error">
      <div
        class="text-[13px] rounded-b rounded-t-none w-full"
        style="background: var(--vscode-textCodeBlock-background)"
      >
        <div
          class="text-[var(--vscode-testing-iconFailed)] mb-2 px-2.5 overflow-wrap-anywhere"
        >
          {{ server.error }}
        </div>
        <button
          type="button"
          class="w-[calc(100%-20px)] mx-2.5 mb-2.5 px-4 py-2 rounded border cursor-pointer bg-[var(--vscode-button-secondaryBackground)] disabled:opacity-50"
          :disabled="server.status === 'connecting'"
          @click="handleRestart"
        >
          {{ server.status === 'connecting' || isRestarting ? 'Retrying...' : 'Retry Connection' }}
        </button>
        <DangerButton
          class="w-[calc(100%-20px)] mx-2.5 mb-2.5"
          :disabled="isDeleting"
          @click="handleDelete"
        >
          {{ isDeleting ? 'Deleting...' : 'Delete Server' }}
        </DangerButton>
      </div>
    </template>

    <template v-else-if="isExpanded">
      <div
        class="px-2.5 pb-2.5 pt-0 text-[13px] rounded-b"
        style="background: var(--vscode-textCodeBlock-background)"
      >
        <div class="flex border-b border-[var(--vscode-editorGroup-border)] mb-2">
          <button
            type="button"
            :class="['px-3 py-1.5 text-[13px]', activePanel === 'tools' ? 'border-b-2 border-[var(--vscode-foreground)]' : '']"
            @click="activePanel = 'tools'"
          >
            Tools ({{ server.tools?.length ?? 0 }})
          </button>
          <button
            type="button"
            :class="['px-3 py-1.5 text-[13px]', activePanel === 'resources' ? 'border-b-2 border-[var(--vscode-foreground)]' : '']"
            @click="activePanel = 'resources'"
          >
            Resources ({{ resourcesCount }})
          </button>
        </div>
        <div v-if="activePanel === 'tools'" class="flex flex-col gap-2 w-full">
          <template v-if="server.tools?.length">
            <McpToolRow
              v-for="t in server.tools"
              :key="t.name"
              :tool="t"
              :server-name="server.name"
            />
            <label
              v-if="server.name && showAutoApproveAll"
              class="flex items-center gap-2 text-[13px] mb-[-10px] cursor-pointer"
            >
              <input
                type="checkbox"
                :checked="allToolsAutoApprove"
                @change="handleAutoApproveAllChange"
              />
              Auto-approve all tools
            </label>
          </template>
          <div v-else class="py-2.5 text-[var(--vscode-descriptionForeground)]">
            No tools found
          </div>
        </div>
        <div v-else class="flex flex-col gap-2 w-full">
          <template v-if="resourcesCount">
            <McpResourceRow
              v-for="item in resourcesList"
              :key="'uriTemplate' in item ? item.uriTemplate : item.uri"
              :item="item"
            />
          </template>
          <div v-else class="py-2.5 text-[var(--vscode-descriptionForeground)]">
            No resources found
          </div>
        </div>

        <div class="my-2.5 mx-1.5">
          <label class="block mb-1 text-[13px]">Request Timeout</label>
          <select
            v-model="timeoutValue"
            class="w-full px-2 py-1.5 rounded border border-[var(--vscode-select-border)] bg-[var(--vscode-select-background)] text-[var(--vscode-select-foreground)]"
            @change="handleTimeoutChange"
          >
            <option v-for="opt in timeoutOptions" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </option>
          </select>
        </div>
        <button
          type="button"
          class="w-[calc(100%-14px)] mx-1.5 mb-0.75 px-4 py-2 rounded border cursor-pointer bg-[var(--vscode-button-secondaryBackground)] disabled:opacity-50"
          :disabled="server.status === 'connecting' || isRestarting"
          @click="handleRestart"
        >
          {{ server.status === 'connecting' || isRestarting ? 'Restarting...' : 'Restart Server' }}
        </button>
        <DangerButton
          class="w-[calc(100%-14px)] mx-1.5 mt-1.25 mb-0.75"
          :disabled="isDeleting"
          @click="handleDelete"
        >
          {{ isDeleting ? 'Deleting...' : 'Delete Server' }}
        </DangerButton>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { McpServer } from '@/shared/mcp'
import { DEFAULT_MCP_TIMEOUT_SECONDS } from '@/shared/mcp'
import { useExtensionStateStore } from '@/stores/extensionState'
import { mcpService } from '@/api/mcp'
import { getMcpServerDisplayName } from '@/utils/mcp'
import DangerButton from '@/components/common/DangerButton.vue'
import McpToolRow from './McpToolRow.vue'
import McpResourceRow from './McpResourceRow.vue'

const props = withDefaults(
  defineProps<{
    server: McpServer
    isExpandable?: boolean
    hasTrashIcon?: boolean
  }>(),
  { isExpandable: true, hasTrashIcon: true }
)

const store = useExtensionStateStore()
const isExpanded = ref(false)
const isDeleting = ref(false)
const isRestarting = ref(false)
const activePanel = ref<'tools' | 'resources'>('tools')

const timeoutOptions = [
  { value: '30', label: '30 seconds' },
  { value: '60', label: '1 minute' },
  { value: '300', label: '5 minutes' },
  { value: '600', label: '10 minutes' },
  { value: '1800', label: '30 minutes' },
  { value: '3600', label: '1 hour' },
]

function getInitialTimeout(): string {
  try {
    const config = JSON.parse(props.server.config)
    return (config.timeout ?? DEFAULT_MCP_TIMEOUT_SECONDS).toString()
  } catch {
    return DEFAULT_MCP_TIMEOUT_SECONDS.toString()
  }
}
const timeoutValue = ref(getInitialTimeout())

watch(
  () => props.server.config,
  () => {
    try {
      const config = JSON.parse(props.server.config)
      timeoutValue.value = (config.timeout ?? DEFAULT_MCP_TIMEOUT_SECONDS).toString()
    } catch {
      timeoutValue.value = DEFAULT_MCP_TIMEOUT_SECONDS.toString()
    }
  },
  { immediate: true }
)

const displayName = computed(() =>
  getMcpServerDisplayName(props.server.name, store.mcpMarketplaceCatalog)
)

const statusColor = computed(() => {
  switch (props.server.status) {
    case 'connected':
      return 'var(--vscode-testing-iconPassed)'
    case 'connecting':
      return 'var(--vscode-charts-yellow)'
    default:
      return 'var(--vscode-testing-iconFailed)'
  }
})

const resourcesCount = computed(() => {
  const t = props.server.resourceTemplates || []
  const r = props.server.resources || []
  return t.length + r.length
})

const resourcesList = computed(() => {
  const t = props.server.resourceTemplates || []
  const r = props.server.resources || []
  return [...t, ...r]
})

const autoApprovalSettings = computed(
  () => store.extensionState?.autoApprovalSettings
)
const showAutoApproveAll = computed(
  () =>
    autoApprovalSettings.value?.enabled &&
    autoApprovalSettings.value?.actions?.useMcp
)
const allToolsAutoApprove = computed(
  () => props.server.tools?.every((t) => t.autoApprove) ?? false
)

function handleRowClick() {
  if (!props.server.error && props.isExpandable) {
    isExpanded.value = !isExpanded.value
  }
}

function handleTimeoutChange(ev: Event) {
  const value = (ev.target as HTMLSelectElement).value
  const num = parseInt(value, 10)
  timeoutValue.value = value
  mcpService
    .updateMcpTimeout({ serverName: props.server.name, timeout: num })
    .then((res) => store.updateMcpServers(res))
    .catch((e) => console.error('Error updating MCP server timeout', e))
}

function handleRestart() {
  isRestarting.value = true
  mcpService
    .restartMcpServer(props.server.name)
    .then((res) => {
      store.updateMcpServers(res)
      isRestarting.value = false
    })
    .catch(() => {
      isRestarting.value = false
      console.error('Error restarting MCP server')
    })
}

function handleDelete() {
  isDeleting.value = true
  mcpService
    .deleteMcpServer(props.server.name)
    .then((res) => {
      store.updateMcpServers(res)
      isDeleting.value = false
    })
    .catch(() => {
      isDeleting.value = false
      console.error('Error deleting MCP server')
    })
}

function handleAutoApproveAllChange() {
  if (!props.server.name || !props.server.tools?.length) return
  const next = !allToolsAutoApprove.value
  mcpService
    .toggleToolAutoApprove({
      serverName: props.server.name,
      toolNames: props.server.tools.map((t) => t.name),
      autoApprove: next,
    })
    .then((res) => store.updateMcpServers(res))
    .catch((e) => console.error('Error toggling all tools auto-approve', e))
}

function handleToggleMcpServer() {
  mcpService
    .toggleMcpServer({
      serverName: props.server.name,
      disabled: !props.server.disabled,
    })
    .then((res) => store.updateMcpServers(res))
    .catch((e) => console.error('Error toggling MCP server', e))
}
</script>
