<template>
  <div v-if="isLaunchMessage" class="flex gap-2.5 items-center mb-2.5">
    <span class="font-bold">Browser Session Started</span>
  </div>
  <div v-if="isLaunchMessage" class="rounded-[3px] border border-[var(--vscode-editorGroup-border)] overflow-hidden bg-[var(--vscode-editor-background)]">
    <CodeBlock :force-wrap="true" :source="`\`\`\`shell\n${message.text}\n\`\`\``" />
  </div>

  <template v-else>
    <template v-if="message.type === 'say'">
      <div v-if="isChatRowMessage" class="px-0 py-2.5">
        <!-- TODO: Use ChatRowContent component when it's converted to Vue -->
        <div class="text-sm">ChatRowContent placeholder - needs ChatRowContent.vue</div>
      </div>
      <BrowserActionBox
        v-else-if="message.say === 'browser_action'"
        :action="browserAction?.action || 'launch'"
        :coordinate="browserAction?.coordinate || ''"
        :text="browserAction?.text || ''"
      />
    </template>
  </template>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ClineMessage, ClineSayBrowserAction } from '@/shared/ExtensionMessage'
import CodeBlock from '@/components/common/CodeBlock.vue'
import BrowserActionBox from './BrowserActionBox.vue'

interface Props {
  message: ClineMessage
  expandedRows: Record<number, boolean>
  onToggleExpand: (messageTs: number) => void
  lastModifiedMessage?: ClineMessage
  isLast: boolean
  onSetQuote: (text: string) => void
}

const props = defineProps<Props>()

const emit = defineEmits<{
  setMaxActionHeight: [height: number]
}>()

const isLaunchMessage = computed(() => {
  return props.message.ask === 'browser_action_launch' || props.message.say === 'browser_action_launch'
})

const isChatRowMessage = computed(() => {
  return (
    props.message.say === 'api_req_started' ||
    props.message.say === 'text' ||
    props.message.say === 'reasoning' ||
    props.message.say === 'error_retry'
  )
})

const browserAction = computed(() => {
  if (props.message.say === 'browser_action') {
    return JSON.parse(props.message.text || '{}') as ClineSayBrowserAction
  }
  return null
})

const handleToggle = () => {
  if (props.message.say === 'api_req_started') {
    emit('setMaxActionHeight', 0)
  }
  props.onToggleExpand(props.message.ts)
}
</script>


