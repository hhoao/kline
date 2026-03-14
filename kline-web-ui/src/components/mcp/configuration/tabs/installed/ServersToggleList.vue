<template>
  <div v-if="servers.length" :class="['flex flex-col', gapClass]">
    <ServerRow
      v-for="server in servers"
      :key="server.name"
      :server="server"
      :has-trash-icon="hasTrashIcon"
      :is-expandable="isExpandable"
    />
  </div>
  <div
    v-else
    class="flex flex-col items-center gap-3 my-5 text-[var(--vscode-descriptionForeground)]"
  >
    No MCP servers installed
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { McpServer } from '@/shared/mcp'
import ServerRow from './server-row/ServerRow.vue'

const props = withDefaults(
  defineProps<{
    servers: McpServer[]
    isExpandable: boolean
    hasTrashIcon: boolean
    listGap?: 'small' | 'medium' | 'large'
  }>(),
  { listGap: 'medium' }
)

const gapClass = computed(() => {
  const map = { small: 'gap-0', medium: 'gap-2.5', large: 'gap-5' }
  return map[props.listGap]
})
</script>
