<template>
  <p class="error-message" :style="style">
    {{ message }}
  </p>
</template>

<script setup lang="ts">
defineProps<{
  message: string
  style?: string | Record<string, string>
}>()
</script>

<style scoped>
.error-message {
  margin: -10px 0 4px 0;
  font-size: 12px;
  color: var(--vscode-errorForeground);
}
</style>
