<template>
  <div
    class="bg-[var(--vscode-inactiveSelectionBackground)] rounded-[3px] p-3 px-4 m-[5px_15px] relative flex-shrink-0"
  >
    <button
      data-testid="close-button"
      class="flex absolute top-2 right-2 justify-center items-center p-0 bg-transparent border-0 cursor-pointer"
      @click="hideAnnouncement"
    >
      <span class="i-codicon:close"></span>
    </button>
  </div>
</template>

<script setup lang="ts">

interface Props {
  version: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  hideAnnouncement: []
}>()

const hideAnnouncement = () => {
  emit('hideAnnouncement')
}
</script>


