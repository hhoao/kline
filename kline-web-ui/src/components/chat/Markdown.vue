<template>
  <div
    class="break-words overflow-wrap-anywhere -mb-[15px] -mt-[15px] overflow-hidden"
  >
    <MarkdownBlock :markdown="markdown" />
  </div>
</template>

<script setup lang="ts">
import MarkdownBlock from '@/components/common/MarkdownBlock.vue'

interface Props {
  markdown?: string
}

defineProps<Props>()
</script>


