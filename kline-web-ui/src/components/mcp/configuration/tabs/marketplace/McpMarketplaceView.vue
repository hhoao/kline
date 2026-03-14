<template>
  <div class="flex flex-col w-full">
    <div class="py-5 px-5 pb-1.25 flex flex-col gap-4">
      <input
        v-model="searchQuery"
        type="text"
        placeholder="Search MCPs..."
        class="w-full px-2 py-1.5 rounded border border-[var(--vscode-input-border)] bg-[var(--vscode-input-background)] text-[var(--vscode-input-foreground)]"
      />
      <div class="flex items-center gap-2">
        <span class="text-[11px] text-[var(--vscode-descriptionForeground)] uppercase font-medium shrink-0">
          Filter:
        </span>
        <select
          v-model="selectedCategory"
          class="flex-1 relative z-[2] px-2 py-1.5 rounded border border-[var(--vscode-select-border)] bg-[var(--vscode-select-background)] text-[var(--vscode-select-foreground)]"
        >
          <option value="">All Categories</option>
          <option v-for="cat in categories" :key="cat" :value="cat">
            {{ cat }}
          </option>
        </select>
      </div>
      <div class="flex gap-2">
        <span class="text-[11px] text-[var(--vscode-descriptionForeground)] uppercase font-medium mt-0.5">
          Sort:
        </span>
        <div class="flex flex-wrap gap-2 mt-[-2.5px]">
          <label class="flex items-center gap-1.5 cursor-pointer text-[13px]">
            <input v-model="sortBy" type="radio" value="downloadCount" />
            Most Installs
          </label>
          <label class="flex items-center gap-1.5 cursor-pointer text-[13px]">
            <input v-model="sortBy" type="radio" value="newest" />
            Newest
          </label>
          <label class="flex items-center gap-1.5 cursor-pointer text-[13px]">
            <input v-model="sortBy" type="radio" value="stars" />
            GitHub Stars
          </label>
          <label class="flex items-center gap-1.5 cursor-pointer text-[13px]">
            <input v-model="sortBy" type="radio" value="name" />
            Name
          </label>
        </div>
      </div>
    </div>
    <div v-if="isLoading || isRefreshing" class="flex justify-center items-center h-full py-5">
      <span class="i-codicon:loading animate-spin text-2xl" />
    </div>
    <div
      v-else-if="error"
      class="flex flex-col justify-center items-center h-full py-5 gap-3"
    >
      <div class="text-[var(--vscode-errorForeground)]">{{ error }}</div>
      <button
        type="button"
        class="px-4 py-2 rounded border cursor-pointer bg-[var(--vscode-button-secondaryBackground)] flex items-center gap-1.5"
        @click="fetchMarketplace(true)"
      >
        <span class="i-codicon:refresh" />
        Retry
      </button>
    </div>
    <div v-else class="flex flex-col">
      <div
        v-if="!filteredItems.length"
        class="flex justify-center items-center h-full py-5 text-[var(--vscode-descriptionForeground)]"
      >
        {{ searchQuery || selectedCategory ? 'No matching MCP servers found' : 'No MCP servers found in the marketplace' }}
      </div>
      <McpMarketplaceCard
        v-for="item in filteredItems"
        :key="item.mcpId"
        :item="item"
        :installed-servers="mcpServers"
        :set-error="(e) => (error = e ?? null)"
      />
      <McpSubmitCard />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import type { McpMarketplaceItem } from '@/shared/mcp'
import { useExtensionStateStore } from '@/stores/extensionState'
import { mcpService } from '@/api/mcp'
import McpMarketplaceCard from './McpMarketplaceCard.vue'
import McpSubmitCard from './McpSubmitCard.vue'

type SortBy = 'newest' | 'stars' | 'name' | 'downloadCount'

const store = useExtensionStateStore()
const isLoading = ref(true)
const error = ref<string | null>(null)
const isRefreshing = ref(false)
const searchQuery = ref('')
const selectedCategory = ref('')
const sortBy = ref<SortBy>('newest')

const mcpServers = computed(() => store.mcpServers)
const items = computed(() => store.mcpMarketplaceCatalog?.items ?? [])

const categories = computed(() => {
  const set = new Set(items.value.map((i) => i.category))
  return Array.from(set).sort()
})

const filteredItems = computed(() => {
  return items.value
    .filter((item) => {
      const q = searchQuery.value.toLowerCase()
      const matchesSearch =
        !q ||
        item.name.toLowerCase().includes(q) ||
        item.description.toLowerCase().includes(q) ||
        item.tags.some((t) => t.toLowerCase().includes(q))
      const matchesCategory = !selectedCategory.value || item.category === selectedCategory.value
      return matchesSearch && matchesCategory
    })
    .sort((a, b) => {
      switch (sortBy.value) {
        case 'downloadCount':
          return b.downloadCount - a.downloadCount
        case 'stars':
          return b.githubStars - a.githubStars
        case 'name':
          return a.name.localeCompare(b.name)
        case 'newest':
          return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
        default:
          return 0
      }
    })
})

watch(
  () => store.mcpMarketplaceCatalog?.items,
  (items) => {
    if (items?.length != null) {
      isLoading.value = false
      isRefreshing.value = false
      error.value = null
    }
  }
)

function fetchMarketplace(forceRefresh = false) {
  if (forceRefresh) isRefreshing.value = true
  else isLoading.value = true
  error.value = null
  mcpService
    .refreshMcpMarketplace()
    .then((catalog) => store.setMcpMarketplaceCatalog(catalog))
    .catch((e) => {
      console.error('Error refreshing MCP marketplace:', e)
      error.value = 'Failed to load marketplace data'
    })
    .finally(() => {
      isLoading.value = false
      isRefreshing.value = false
    })
}

onMounted(() => fetchMarketplace())
</script>
