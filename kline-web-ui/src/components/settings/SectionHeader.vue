<template>
  <div class="text-[var(--vscode-foreground)] px-5 py-3" :class="className">
    <h4 class="m-0"><slot /></h4>
    <p v-if="description" class="text-[var(--vscode-descriptionForeground)] text-sm mt-2 mb-0">
      {{ description }}
    </p>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  description?: string
  className?: string
}>()
</script>
