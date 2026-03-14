<template>
  <div
    class="relative rounded border overflow-hidden z-0 font-mono text-[var(--vscode-editor-font-size)]"
    :style="{ backgroundColor: CODE_BLOCK_BG_COLOR, color: 'var(--vscode-editor-foreground)', borderColor: 'var(--vscode-editorGroup-border)' }"
  >
    <div
      class="flex justify-between items-center py-2.25 px-2.5 text-[var(--vscode-descriptionForeground)] cursor-pointer select-none border-b border-dashed border-[var(--vscode-editorGroup-border)] mb-2"
      :style="{ borderBottom: isExpanded ? '1px dashed var(--vscode-editorGroup-border)' : 'none', marginBottom: isExpanded ? '8px' : '0' }"
      @click="isExpanded = !isExpanded"
    >
      <div class="flex overflow-hidden items-center mr-2 whitespace-nowrap text-ellipsis">
        <span :class="['mr-1.5', isExpanded ? 'i-codicon:chevron-down' : 'i-codicon:chevron-right']" />
        Response
      </div>
      <div
        :style="{ minWidth: isExpanded ? 'auto' : '0', visibility: isExpanded ? 'visible' : 'hidden' }"
        @click.stop
      >
        <McpDisplayModeDropdown
          :model-value="mcpDisplayMode"
          class="min-w-[120px]"
          @update:model-value="handleDisplayModeChange"
        />
      </div>
    </div>
    <div v-if="isExpanded" class="overflow-x-auto overflow-y-hidden p-2.5 max-w-full response-content">
      <template v-if="isLoading && mcpDisplayMode === 'rich'">
        <div class="flex justify-center items-center h-[50px]">
          <span class="text-xl animate-spin i-codicon:loading" />
        </div>
      </template>
      <template v-else-if="mcpDisplayMode === 'plain'">
        <div class="whitespace-pre-wrap break-all overflow-wrap-break font-mono text-[var(--vscode-editor-font-size)]">
          {{ responseText }}
        </div>
      </template>
      <template v-else-if="mcpDisplayMode === 'markdown'">
        <MarkdownBlock :markdown="responseText" />
      </template>
      <template v-else-if="error">
        <div class="text-[var(--vscode-errorForeground)] mb-2.5">{{ error }}</div>
        <div class="font-mono whitespace-pre-wrap break-all overflow-wrap-break">
          {{ responseText }}
        </div>
      </template>
      <template v-else-if="mcpDisplayMode === 'rich'">
        <template v-for="seg in displaySegments" :key="seg.key">
          <div
            v-if="seg.type === 'text' || seg.type === 'url'"
            class="whitespace-pre-wrap break-all overflow-wrap-break font-mono text-[var(--vscode-editor-font-size)]"
          >
            {{ seg.content }}
          </div>
          <ImagePreview v-else-if="seg.type === 'image' && seg.url" :url="seg.url" />
          <div v-else-if="seg.type === 'link' && seg.url" class="my-2.5">
            <LinkPreview :url="seg.url" />
          </div>
          <div
            v-else-if="seg.type === 'error'"
            class="my-2.5 p-2 text-[var(--vscode-errorForeground)] border border-[var(--vscode-editorError-foreground)] rounded h-32 overflow-auto"
          >
            {{ seg.content }}
          </div>
        </template>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import type { McpDisplayMode } from '@/shared/McpDisplayMode'
import { CODE_BLOCK_BG_COLOR } from '@/components/common/CodeBlock'
import { useExtensionStateStore } from '@/stores/extensionState'
import { stateService } from '@/api/state'
import MarkdownBlock from '@/components/common/MarkdownBlock.vue'
import McpDisplayModeDropdown from './McpDisplayModeDropdown.vue'
import ImagePreview from './ImagePreview.vue'
import LinkPreview from './LinkPreview.vue'
import {
  processResponseUrls,
  buildDisplaySegments,
  type DisplaySegment,
} from './utils/mcpRichUtil'

const MAX_URLS = 50

const props = defineProps<{ responseText: string }>()

const store = useExtensionStateStore()
const mcpResponsesCollapsed = computed(
  () => store.extensionState?.mcpResponsesCollapsed ?? false
)
const mcpDisplayMode = computed<McpDisplayMode>(
  () => store.extensionState?.mcpDisplayMode ?? 'plain'
)

const isExpanded = ref(!mcpResponsesCollapsed.value)
const isLoading = ref(false)
const urlMatches = ref<import('./utils/mcpRichUtil').UrlMatch[]>([])
const error = ref<string | null>(null)

let cleanup: (() => void) | undefined

watch(
  () => mcpResponsesCollapsed.value,
  (v) => {
    isExpanded.value = !v
  },
  { immediate: true }
)

watch(
  () => [props.responseText, mcpDisplayMode.value, isExpanded.value] as const,
  ([text, mode, expanded]) => {
    if (!expanded || mode === 'plain' || mode === 'markdown') {
      isLoading.value = false
      urlMatches.value = []
      return
    }
    isLoading.value = true
    error.value = null
    cleanup?.()
    cleanup = processResponseUrls(
      text || '',
      MAX_URLS,
      (matches) => {
        urlMatches.value = matches
        isLoading.value = false
      },
      (matches) => {
        urlMatches.value = matches
      },
      (err) => {
        error.value = err
        isLoading.value = false
      }
    )
  },
  { immediate: true }
)

onUnmounted(() => {
  cleanup?.()
})

const displaySegments = computed((): DisplaySegment[] => {
  if (mcpDisplayMode.value !== 'rich' || !isExpanded.value) return []
  return buildDisplaySegments(props.responseText, urlMatches.value)
})

function handleDisplayModeChange(mode: McpDisplayMode) {
  stateService.updateSettings({ mcpDisplayMode: mode }).catch((e) => {
    console.error('Error updating mcp display mode', e)
  })
}
</script>

<style scoped>
.overflow-wrap-break {
  overflow-wrap: break-word;
}
</style>
