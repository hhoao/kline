<template>
  <div class="bg-[var(--vscode-badge-background)] text-[var(--vscode-badge-foreground)] rounded-[3px] p-[14px] pb-[6px]">
    <span class="font-bold">Task</span>
    <MarkdownBlock :markdown="context" />
  </div>
</template>

<script setup lang="ts">
import MarkdownBlock from '@/components/common/MarkdownBlock.vue'

interface Props {
  context: string
}

defineProps<Props>()
</script>
