<template>
  <div>
    <SectionHeader>Debug</SectionHeader>
    <Section>
      <button
        type="button"
        class="mt-[5px] px-4 py-2 rounded border-0 cursor-pointer text-[13px] font-inherit"
        style="background: var(--vscode-errorForeground); color: black"
        @click="emit('resetState', false)"
      >
        Reset Workspace State
      </button>
      <button
        type="button"
        class="mt-[5px] block px-4 py-2 rounded border-0 cursor-pointer text-[13px] font-inherit"
        style="background: var(--vscode-errorForeground); color: black"
        @click="emit('resetState', true)"
      >
        Reset Global State
      </button>
      <p class="text-xs mt-[5px] text-[var(--vscode-descriptionForeground)]">
        This will reset all global state and secret storage in the extension.
      </p>
    </Section>
  </div>
</template>

<script setup lang="ts">
import Section from '../Section.vue'
import SectionHeader from '../SectionHeader.vue'

const emit = defineEmits<{ resetState: [global?: boolean] }>()
</script>
