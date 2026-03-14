<template>
  <div class="text-xs my-2 flex items-center gap-0 justify-between">
    <span class="font-semibold text-sm">Compact the current task?</span>
    <span class="flex gap-1">
      <button
        class="text-xs bg-[var(--vscode-button-secondaryBackground)] text-[var(--vscode-button-secondaryForeground)] hover:bg-[var(--vscode-button-secondaryHoverBackground)] px-3 py-1 rounded border-0 cursor-pointer"
        title="No, keep the task as is"
        type="button"
        @click="handleCancel"
      >
        Cancel
      </button>
      <button
        class="text-xs bg-[var(--vscode-button-background)] text-[var(--vscode-button-foreground)] hover:bg-[var(--vscode-button-hoverBackground)] px-3 py-1 rounded border-0 cursor-pointer"
        title="Yes, compact the task"
        type="button"
        @click="handleConfirm"
      >
        Yes
      </button>
    </span>
  </div>
</template>

<script setup lang="ts">
const emit = defineEmits<{
  confirm: [event: MouseEvent]
  cancel: [event: MouseEvent]
}>()

const handleConfirm = (e: MouseEvent) => {
  e.preventDefault()
  e.stopPropagation()
  emit('confirm', e)
}

const handleCancel = (e: MouseEvent) => {
  e.preventDefault()
  e.stopPropagation()
  emit('cancel', e)
}
</script>

