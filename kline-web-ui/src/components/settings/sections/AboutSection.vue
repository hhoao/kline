<template>
  <div>
    <SectionHeader>About</SectionHeader>
    <Section>
      <div class="px-4">
        <h2 class="mt-0">Cline v{{ version }}</h2>
        <p>
          An AI assistant that can use your CLI and Editor. Cline can handle complex software development tasks
          step-by-step with tools that let him create & edit files, explore large projects, use the browser, and
          execute terminal commands (after you grant permission).
        </p>
        <h3>Community & Support</h3>
        <p>
          <a href="https://x.com/cline" target="_blank" rel="noopener noreferrer" class="text-[var(--vscode-textLink-foreground)] underline">X</a>
          •
          <a href="https://discord.gg/cline" target="_blank" rel="noopener noreferrer" class="text-[var(--vscode-textLink-foreground)] underline">Discord</a>
          •
          <a href="https://www.reddit.com/r/cline/" target="_blank" rel="noopener noreferrer" class="text-[var(--vscode-textLink-foreground)] underline">r/cline</a>
        </p>
        <h3>Development</h3>
        <p>
          <a href="https://github.com/cline/cline" target="_blank" rel="noopener noreferrer" class="text-[var(--vscode-textLink-foreground)] underline">GitHub</a>
          •
          <a href="https://github.com/cline/cline/issues" target="_blank" rel="noopener noreferrer" class="text-[var(--vscode-textLink-foreground)] underline">Issues</a>
          •
          <a href="https://github.com/cline/cline/discussions/categories/feature-requests?discussions_q=is%3Aopen+category%3A%22Feature+Requests%22+sort%3Atop" target="_blank" rel="noopener noreferrer" class="text-[var(--vscode-textLink-foreground)] underline">Feature Requests</a>
        </p>
        <h3>Resources</h3>
        <p>
          <a href="https://docs.cline.bot/getting-started/for-new-coders" target="_blank" rel="noopener noreferrer" class="text-[var(--vscode-textLink-foreground)] underline">Documentation</a>
          •
          <a href="https://cline.bot/" target="_blank" rel="noopener noreferrer" class="text-[var(--vscode-textLink-foreground)] underline">https://cline.bot</a>
        </p>
      </div>
    </Section>
  </div>
</template>

<script setup lang="ts">
import Section from '../Section.vue'
import SectionHeader from '../SectionHeader.vue'

defineProps<{
  version: string
}>()
</script>
