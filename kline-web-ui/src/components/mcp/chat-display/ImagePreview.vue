<template>
  <div
    v-if="error"
    class="p-3 border rounded border-[var(--vscode-editorWidget-border)] text-[var(--vscode-errorForeground)] cursor-pointer"
    @click="openInBrowser"
  >
    <div class="font-bold">Failed to load image</div>
    <div class="text-[12px] mt-1">{{ hostname }}</div>
    <div class="text-[11px] mt-2 text-[var(--vscode-textLink-foreground)]">
      Click to open in browser
    </div>
  </div>
  <div
    v-else
    class="my-2.5 max-w-full cursor-pointer"
    @click="openInBrowser"
  >
    <img
      v-if="isDataUrl || !error"
      :src="safeUrl"
      :alt="`Image from ${hostname}`"
      class="max-w-[85%] h-auto rounded"
      @error="error = 'Failed to load'"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { checkIfImageUrl, getSafeHostname, formatUrlForOpening } from './utils/mcpRichUtil'

const props = defineProps<{ url: string }>()

const error = ref<string | null>(null)
const safeUrl = computed(() => formatUrlForOpening(props.url))
const hostname = computed(() => getSafeHostname(props.url))
const isDataUrl = computed(() => props.url.startsWith('data:image/'))

function openInBrowser() {
  try {
    window.open(safeUrl.value, '_blank', 'noopener,noreferrer')
  } catch (e) {
    console.error('Error opening URL:', e)
  }
}

onMounted(async () => {
  if (props.url.startsWith('data:image/')) return
  const ok = await checkIfImageUrl(props.url)
  if (!ok) error.value = 'Not an image'
})
</script>
