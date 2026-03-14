<template>
  <div
    class="checkpoint-controls absolute top-[3px] right-[30px] flex gap-[6px] opacity-0 bg-[var(--vscode-sideBar-background)] py-[3px] pl-[3px]"
    @mouseleave="handleControlsMouseLeave"
  >
    <button
      class="w-6 h-6 relative bg-[var(--vscode-button-secondaryBackground)] text-[var(--vscode-button-secondaryForeground)] border border-[var(--vscode-button-border)] rounded cursor-pointer transition-colors hover:bg-[var(--vscode-button-secondaryHoverBackground)] disabled:opacity-50 disabled:cursor-not-allowed"
      :disabled="compareDisabled"
      :style="{ cursor: compareDisabled ? 'wait' : 'pointer' }"
      title="Compare"
      @click="handleCompare"
    >
      <i class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 i-codicon:diff-multiple" />
    </button>
    <div ref="containerRef" class="relative">
      <button
        class="w-6 h-6 relative bg-[var(--vscode-button-secondaryBackground)] text-[var(--vscode-button-secondaryForeground)] border border-[var(--vscode-button-border)] rounded cursor-pointer transition-colors hover:bg-[var(--vscode-button-secondaryHoverBackground)]"
        title="Restore"
        @click="showRestoreConfirm = true"
      >
        <i class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 i-codicon:discard" />
      </button>
      <div
        v-if="showRestoreConfirm"
        ref="tooltipRef"
        class="absolute top-[calc(100%-0.5px)] right-0 bg-[var(--vscode-editor-background)] border border-[var(--vscode-editorGroup-border)] p-3 rounded-[3px] mt-2 w-[calc(100vw-57px)] min-w-0 max-w-screen z-[1000]"
        :style="{ background: CODE_BLOCK_BG_COLOR }"
        @mouseenter="handleMouseEnter"
        @mouseleave="handleMouseLeave"
      >
        <div class="absolute right-0 left-0 -top-2 h-2" />
        <div
          class="absolute -top-[6px] right-[6px] w-[10px] h-[10px] border-l border-t border-[var(--vscode-editorGroup-border)] rotate-45 z-[1]"
          :style="{ background: CODE_BLOCK_BG_COLOR }"
        />
        
        <div class="mb-2.5 pb-1 border-b border-[var(--vscode-editorGroup-border)]">
          <button
            class="w-full mb-2.5 px-4 py-2 rounded border cursor-pointer transition-colors bg-[var(--vscode-button-background)] text-[var(--vscode-button-foreground)] border-[var(--vscode-button-border)] hover:bg-[var(--vscode-button-hoverBackground)] disabled:opacity-50 disabled:cursor-not-allowed"
            :disabled="restoreBothDisabled"
            :style="{ cursor: restoreBothDisabled ? 'wait' : 'pointer' }"
            @click="handleRestoreBoth"
          >
            Restore Task and Workspace
          </button>
          <p class="m-0 mb-0.5 text-[var(--vscode-descriptionForeground)] text-[11px] leading-[14px]">
            Restores the task and your project's files back to a snapshot taken at this point
          </p>
        </div>
        
        <div class="mb-2.5 pb-1 border-b border-[var(--vscode-editorGroup-border)]">
          <button
            class="w-full mb-2.5 px-4 py-2 rounded border cursor-pointer transition-colors bg-[var(--vscode-button-background)] text-[var(--vscode-button-foreground)] border-[var(--vscode-button-border)] hover:bg-[var(--vscode-button-hoverBackground)] disabled:opacity-50 disabled:cursor-not-allowed"
            :disabled="restoreTaskDisabled"
            :style="{ cursor: restoreTaskDisabled ? 'wait' : 'pointer' }"
            @click="handleRestoreTask"
          >
            Restore Task Only
          </button>
          <p class="m-0 mb-0.5 text-[var(--vscode-descriptionForeground)] text-[11px] leading-[14px]">
            Deletes messages after this point (does not affect workspace)
          </p>
        </div>
        
        <div>
          <button
            class="w-full mb-2.5 px-4 py-2 rounded border cursor-pointer transition-colors bg-[var(--vscode-button-background)] text-[var(--vscode-button-foreground)] border-[var(--vscode-button-border)] hover:bg-[var(--vscode-button-hoverBackground)] disabled:opacity-50 disabled:cursor-not-allowed"
            :disabled="restoreWorkspaceDisabled"
            :style="{ cursor: restoreWorkspaceDisabled ? 'wait' : 'pointer' }"
            @click="handleRestoreWorkspace"
          >
            Restore Workspace Only
          </button>
          <p class="m-0 -mb-0.5 text-[var(--vscode-descriptionForeground)] text-[11px] leading-[14px]">
            Restores your project's files to a snapshot taken at this point (task may become out of sync)
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { CODE_BLOCK_BG_COLOR } from './CodeBlock'
import { checkpointsService } from '@/api/checkpoints'
import { useExtensionStateStore } from '@/stores/extensionState'

interface Props {
  messageTs?: number
}

const props = defineProps<Props>()

const compareDisabled = ref(false)
const restoreTaskDisabled = ref(false)
const restoreWorkspaceDisabled = ref(false)
const restoreBothDisabled = ref(false)
const showRestoreConfirm = ref(false)
const hasMouseEntered = ref(false)

const containerRef = ref<HTMLDivElement | null>(null)
const tooltipRef = ref<HTMLDivElement | null>(null)

const extensionStateStore = useExtensionStateStore()

const handleCompare = async () => {
  compareDisabled.value = true
  try {
    await checkpointsService.checkpointDiff(
      props.messageTs || 0
    )
  } catch (err) {
    console.error('CheckpointDiff error:', err)
  } finally {
    compareDisabled.value = false
  }
}

const handleRestoreTask = async () => {
  restoreTaskDisabled.value = true
  try {
    await checkpointsService.checkpointRestore({
      taskId: extensionStateStore.conversationId ?? undefined,
      number: props.messageTs || 0,
      restoreType: 'task',
    })
  } catch (err) {
    console.error('Checkpoint restore task error:', err)
    restoreTaskDisabled.value = false
  }
}

const handleRestoreWorkspace = async () => {
  restoreWorkspaceDisabled.value = true
  try {
    await checkpointsService.checkpointRestore({
      taskId: extensionStateStore.conversationId ?? undefined,
      number: props.messageTs || 0,
      restoreType: 'workspace',
    })
  } catch (err) {
    console.error('Checkpoint restore workspace error:', err)
    restoreWorkspaceDisabled.value = false
  }
}

const handleRestoreBoth = async () => {
  restoreBothDisabled.value = true
  try {
    await checkpointsService.checkpointRestore({
      taskId: extensionStateStore.conversationId ?? undefined,
      number: props.messageTs || 0,
      restoreType: 'taskAndWorkspace',
    })
  } catch (err) {
    console.error('Checkpoint restore both error:', err)
    restoreBothDisabled.value = false
  }
}

const handleMouseEnter = () => {
  hasMouseEntered.value = true
}

const handleMouseLeave = () => {
  if (hasMouseEntered.value) {
    showRestoreConfirm.value = false
    hasMouseEntered.value = false
  }
}

const handleControlsMouseLeave = (e: MouseEvent) => {
  const tooltipElement = tooltipRef.value

  if (tooltipElement && showRestoreConfirm.value) {
    const tooltipRect = tooltipElement.getBoundingClientRect()

    if (
      e.clientY >= tooltipRect.top &&
      e.clientY <= tooltipRect.bottom &&
      e.clientX >= tooltipRect.left &&
      e.clientX <= tooltipRect.right
    ) {
      return
    }
  }

  showRestoreConfirm.value = false
  hasMouseEntered.value = false
}

const handleClickAway = (e: MouseEvent) => {
  if (
    showRestoreConfirm.value &&
    containerRef.value &&
    !containerRef.value.contains(e.target as Node)
  ) {
    showRestoreConfirm.value = false
    hasMouseEntered.value = false
  }
}

watch(showRestoreConfirm, (newVal) => {
  if (newVal) {
    document.addEventListener('mousedown', handleClickAway)
  } else {
    document.removeEventListener('mousedown', handleClickAway)
  }
})

let cleanupRelinquishControl: (() => void) | null = null

onMounted(() => {
  cleanupRelinquishControl = extensionStateStore.onRelinquishControl(() => {
    compareDisabled.value = false
    restoreTaskDisabled.value = false
    restoreWorkspaceDisabled.value = false
    restoreBothDisabled.value = false
    showRestoreConfirm.value = false
  })
})

onBeforeUnmount(() => {
  document.removeEventListener('mousedown', handleClickAway)
  if (cleanupRelinquishControl) {
    cleanupRelinquishControl()
  }
})
</script>
