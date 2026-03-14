<template>
  <p class="model-info-view">
    <template v-for="(node, i) in infoNodes" :key="i">
      <component :is="node" />
      <br v-if="i < infoNodes.length - 1" />
    </template>
  </p>
</template>

<script setup lang="ts">
import { computed, h } from 'vue'
import type { ModelInfo } from '@/shared/api'

const props = defineProps<{
  selectedModelId: string
  modelInfo: ModelInfo
  isPopup?: boolean
}>()

function supportNode(isSupported: boolean, supportsLabel: string, doesNotSupportLabel: string) {
  return h('span', {
    class: 'font-medium',
    style: {
      color: isSupported ? 'var(--vscode-charts-green)' : 'var(--vscode-errorForeground)',
    },
  }, [
    h('i', {
      class: `codicon codicon-${isSupported ? 'check' : 'x'}`,
      style: {
        marginRight: '4px',
        marginBottom: isSupported ? '1px' : '-1px',
        fontSize: isSupported ? '11px' : '13px',
        fontWeight: 700,
        display: 'inline-block',
        verticalAlign: 'bottom',
      },
    }),
    isSupported ? supportsLabel : doesNotSupportLabel,
  ])
}

const infoNodes = computed(() => {
  const info: ReturnType<typeof h>[] = []
  const m = props.modelInfo
  info.push(supportNode(!!m.supportsImages, 'Supports images', 'Does not support images'))
  info.push(supportNode(!!m.supportsPromptCache, 'Supports prompt caching', 'Does not support prompt caching'))
  if (m.contextWindow !== undefined && m.contextWindow > 0) {
    info.push(h('span', null, [
      h('span', { class: 'font-medium' }, 'Context Window: '),
      `${m.contextWindow.toLocaleString()} tokens`,
    ]))
  }
  if (m.maxTokens !== undefined && m.maxTokens > 0) {
    info.push(h('span', null, [
      h('span', { class: 'font-medium' }, 'Max output: '),
      `${m.maxTokens.toLocaleString()} tokens`,
    ]))
  }
  return info
})
</script>

<style scoped>
.model-info-view {
  font-size: 12px;
  margin-top: 2px;
  color: var(--vscode-descriptionForeground);
}
</style>
