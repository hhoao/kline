<template>
  <a
    :class="['block no-underline text-inherit', isLoading ? 'cursor-wait' : 'cursor-pointer']"
    :href="item.githubUrl"
    style="padding: 14px 16px; display: flex; flex-direction: column; gap: 12px"
    @click.prevent="handleCardClick"
  >
    <div class="flex gap-3">
      <img
        v-if="item.logoUrl"
        :alt="`${item.name} logo`"
        :src="item.logoUrl"
        class="w-[42px] h-[42px] rounded"
      />
      <div class="flex-1 min-w-0 flex flex-col justify-between">
        <div class="flex justify-between items-center gap-4">
          <h3 class="m-0 text-[13px] font-semibold">{{ item.name }}</h3>
          <button
            type="button"
            :class="[
              'text-[12px] font-medium py-0.5 px-1.5 rounded border-none cursor-pointer',
              isInstalled
                ? 'bg-[var(--vscode-button-secondaryBackground)]'
                : 'bg-[var(--vscode-button-background)]',
              (isInstalled || isDownloading) && 'opacity-50 cursor-default'
            ]"
            :disabled="isInstalled || isDownloading"
            @click.stop="handleInstallClick"
          >
            {{ isInstalled ? 'Installed' : isDownloading ? 'Installing...' : 'Install' }}
          </button>
        </div>
        <div
          class="flex items-center gap-2 text-[12px] text-[var(--vscode-descriptionForeground)] flex-wrap min-w-0"
        >
          <a
            :href="githubAuthorUrl"
            class="flex items-center text-[var(--vscode-foreground)] min-w-0 opacity-70 no-underline hover:opacity-100 hover:text-[var(--vscode-textLink-activeForeground)]"
            @click.stop
          >
            <span class="i-codicon:github text-sm mr-1" />
            <span class="overflow-hidden text-ellipsis break-all min-w-0">{{ item.author }}</span>
          </a>
          <div class="flex items-center gap-1 min-w-0 shrink-0">
            <span class="i-codicon:star-full" />
            <span class="break-all">{{ (item.githubStars ?? 0).toLocaleString() }}</span>
          </div>
          <div class="flex items-center gap-1 min-w-0 shrink-0">
            <span class="i-codicon:cloud-download" />
            <span class="break-all">{{ (item.downloadCount ?? 0).toLocaleString() }}</span>
          </div>
          <span
            v-if="item.requiresApiKey"
            class="i-codicon:key shrink-0"
            title="Requires API key"
          />
        </div>
      </div>
    </div>
    <div class="flex flex-col gap-3">
      <p class="text-[13px] m-0">{{ item.description }}</p>
      <div
        class="flex gap-1.5 flex-nowrap overflow-x-auto overflow-y-hidden relative"
        style="scrollbar-width: none"
      >
        <span
          class="text-[10px] py-0.5 px-1 rounded border border-[var(--vscode-descriptionForeground)]/50 text-[var(--vscode-descriptionForeground)] whitespace-nowrap"
        >
          {{ item.category }}
        </span>
        <span
          v-for="tag in item.tags"
          :key="tag"
          class="text-[10px] py-0.5 px-1 rounded border border-[var(--vscode-descriptionForeground)]/50 text-[var(--vscode-descriptionForeground)] whitespace-nowrap inline-flex"
        >
          {{ tag }}
        </span>
        <div
          class="absolute right-0 top-0 bottom-0 w-8 pointer-events-none"
          style="background: linear-gradient(to right, transparent, var(--vscode-sideBar-background))"
        />
      </div>
    </div>
  </a>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { McpMarketplaceItem, McpServer } from '@/shared/mcp'
import { useExtensionStateStore } from '@/stores/extensionState'
import { mcpService } from '@/api/mcp'

const props = defineProps<{
  item: McpMarketplaceItem
  installedServers: McpServer[]
  setError: (err: string | null) => void
}>()

const store = useExtensionStateStore()
const isDownloading = ref(false)
const isLoading = ref(false)

const isInstalled = computed(() =>
  props.installedServers.some((s) => s.name === props.item.mcpId)
)

const githubAuthorUrl = computed(() => {
  try {
    const url = new URL(props.item.githubUrl)
    const parts = url.pathname.split('/')
    if (parts.length >= 2) return `${url.origin}/${parts[1]}`
  } catch {}
  return props.item.githubUrl
})

function handleCardClick() {
  if (!isLoading.value) window.open(props.item.githubUrl, '_blank')
}

async function handleInstallClick() {
  if (props.isInstalled || isDownloading.value) return
  isDownloading.value = true
  try {
    const res = await mcpService.downloadMcp(props.item.mcpId)
    if (res.error) {
      props.setError(res.error)
    } else {
      props.setError(null)
    }
    const latest = await mcpService.getLatestMcpServers()
    store.updateMcpServers(latest)
  } catch (e) {
    console.error('Failed to download MCP:', e)
    props.setError(e instanceof Error ? e.message : 'Download failed')
  } finally {
    isDownloading.value = false
  }
}
</script>
