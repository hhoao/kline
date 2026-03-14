<template>
  <div class="w-4 h-4 flex items-center justify-center">
    <div class="scale-[0.55] origin-center">
      <div class="w-4 h-4 border-2 border-[var(--vscode-progressBar-background)] border-t-transparent rounded-full animate-spin"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
// ProgressIndicator component
</script>

<style scoped>
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
.animate-spin {
  animation: spin 1s linear infinite;
}
</style>


