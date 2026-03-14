<template>
  <div class="py-0.75">
    <div class="flex items-center mb-1">
      <span class="i-codicon:symbol-file mr-1.5" />
      <span class="font-medium break-all">{{ uri }}</span>
    </div>
    <div class="text-[12px] opacity-80 my-1">
      {{ descriptionText }}
    </div>
    <div class="text-[12px]">
      <span class="opacity-80">Returns </span>
      <code
        class="text-[var(--vscode-textPreformat-foreground)] bg-[var(--vscode-textPreformat-background)] px-1 py-0.5 rounded"
      >
        {{ item.mimeType || 'Unknown' }}
      </code>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { McpResource, McpResourceTemplate } from '@/shared/mcp'

const props = defineProps<{
  item: McpResource | McpResourceTemplate
}>()

const uri = computed(() =>
  'uri' in props.item ? props.item.uri : props.item.uriTemplate
)

const descriptionText = computed(() => {
  const { item } = props
  if (item.name && item.description) return `${item.name}: ${item.description}`
  if (item.description) return item.description
  if (item.name) return item.name
  return 'No description'
})
</script>
