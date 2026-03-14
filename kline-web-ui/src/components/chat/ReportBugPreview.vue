<template>
  <div class="bg-[var(--vscode-badge-background)] text-[var(--vscode-badge-foreground)] rounded-[3px] p-[14px]">
    <h3 class="font-bold text-base mb-3 mt-0">{{ bugData.title || 'Bug Report' }}</h3>

    <div class="space-y-3 text-sm">
      <div v-if="bugData.what_happened">
        <div class="font-semibold">What Happened?</div>
        <MarkdownBlock :markdown="bugData.what_happened" />
      </div>

      <div v-if="bugData.steps_to_reproduce">
        <div class="font-semibold">Steps to Reproduce</div>
        <MarkdownBlock :markdown="bugData.steps_to_reproduce" />
      </div>

      <div v-if="bugData.api_request_output">
        <div class="font-semibold">Relevant API Request Output</div>
        <MarkdownBlock :markdown="bugData.api_request_output" />
      </div>

      <div v-if="bugData.provider_and_model">
        <div class="font-semibold">Provider/Model</div>
        <MarkdownBlock :markdown="bugData.provider_and_model" />
      </div>

      <div v-if="bugData.operating_system">
        <div class="font-semibold">Operating System</div>
        <MarkdownBlock :markdown="bugData.operating_system" />
      </div>

      <div v-if="bugData.system_info">
        <div class="font-semibold">System Info</div>
        <MarkdownBlock :markdown="bugData.system_info" />
      </div>

      <div v-if="bugData.cline_version">
        <div class="font-semibold">Cline Version</div>
        <MarkdownBlock :markdown="bugData.cline_version" />
      </div>

      <div v-if="bugData.additional_context">
        <div class="font-semibold">Additional Context</div>
        <MarkdownBlock :markdown="bugData.additional_context" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import MarkdownBlock from '@/components/common/MarkdownBlock.vue'

interface Props {
  data: string
}

const props = defineProps<Props>()

const bugData = computed(() => {
  try {
    return JSON.parse(props.data || '{}')
  } catch (e) {
    console.error('Failed to parse bug report data', e)
    return {}
  }
})
</script>
