<template>
  <div
    v-if="isContainerExpanded"
    class="w-full relative overflow-visible border-t border-white/7 bg-[var(--vscode-editor-background)] rounded-bl-md rounded-br-md"
    :style="{
      paddingBottom: lineCount > 5 ? '16px' : '0',
    }"
  >
    <div
      ref="outputRef"
      class="text-white overflow-y-auto scroll-smooth bg-[var(--vscode-editor-background)]"
      :style="{
        maxHeight: shouldAutoShow ? 'none' : isOutputFullyExpanded ? '200px' : '75px',
        overflowY: shouldAutoShow ? 'visible' : 'auto',
      }"
    >
      <div class="bg-[var(--vscode-editor-background)]">
        <CodeBlock :force-wrap="true" :source="`\`\`\`shell\n${output}\n\`\`\``" />
      </div>
    </div>
    <div
      v-if="lineCount > 5"
      class="absolute -bottom-2.5 left-1/2 -translate-x-1/2 flex justify-center items-center px-3.5 py-0.5 cursor-pointer bg-[var(--vscode-descriptionForeground)] rounded-[3px_3px_6px_6px] transition-opacity duration-100 ease-in border border-black/10 hover:opacity-80"
      @click="$emit('toggle')"
    >
      <span
        :class="`i-codicon:triangle-${isOutputFullyExpanded ? 'up' : 'down'}`"
        class="text-[11px] text-[var(--vscode-editor-background)]"
      ></span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import CodeBlock from '@/components/common/CodeBlock.vue'

interface Props {
  output: string
  isOutputFullyExpanded: boolean
  isContainerExpanded: boolean
}

const props = defineProps<Props>()

defineEmits<{
  toggle: []
}>()

const outputRef = ref<HTMLDivElement | null>(null)

const outputLines = computed(() => props.output.split('\n'))
const lineCount = computed(() => outputLines.value.length)
const shouldAutoShow = computed(() => lineCount.value <= 5)

// Auto-scroll to bottom when output changes (only when showing limited output)
watch(
  [() => props.output, () => props.isOutputFullyExpanded],
  async () => {
    if (!props.isOutputFullyExpanded && outputRef.value) {
      await nextTick()
      outputRef.value.scrollTop = outputRef.value.scrollHeight
      setTimeout(() => {
        if (outputRef.value) {
          outputRef.value.scrollTop = outputRef.value.scrollHeight
        }
      }, 50)
    }
  },
  { immediate: true }
)
</script>

