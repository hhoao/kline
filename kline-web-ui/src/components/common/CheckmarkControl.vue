<template>
  <div
    :class="[
      'flex items-center py-1 gap-1 relative min-w-0 min-h-[17px] -mt-2.5 -mb-2.5 transition-opacity hover:opacity-100',
      isCheckpointCheckedOut ? 'opacity-100' : showRestoreConfirm ? 'opacity-100' : 'opacity-50'
    ]"
    @mouseenter="handleControlsMouseEnter"
    @mouseleave="handleControlsMouseLeave"
  >
    <i
      :class="[
        'i-codicon:bookmark text-xs flex-shrink-0',
        isCheckpointCheckedOut ? 'text-[var(--vscode-textLink-foreground)]' : 'text-[var(--vscode-descriptionForeground)]'
      ]"
    />
    <div
      :class="[
        'flex-1 min-w-[5px] h-[1px]',
        isCheckpointCheckedOut ? 'bg-gradient-to-r from-[var(--vscode-textLink-foreground)]' : 'bg-gradient-to-r from-[var(--vscode-descriptionForeground)]',
        'to-transparent bg-[length:4px_1px] bg-repeat-x',
        showRestoreConfirm ? 'hidden' : 'flex'
      ]"
      :style="dottedLineStyle"
    />
    <div
      :class="[
        'flex items-center gap-1 flex-1 min-w-0',
        showRestoreConfirm ? 'flex' : 'hidden group-hover:flex'
      ]"
    >
      <span
        :class="[
          'text-[9px] flex-shrink-0',
          isCheckpointCheckedOut ? 'text-[var(--vscode-textLink-foreground)]' : 'text-[var(--vscode-descriptionForeground)]'
        ]"
      >
        {{ isCheckpointCheckedOut ? 'Checkpoint (restored)' : 'Checkpoint' }}
      </span>
      <div
        class="flex-1 min-w-[5px] h-[1px] bg-[length:4px_1px] bg-repeat-x"
        :style="dottedLineStyle"
      />
      <div class="flex flex-shrink-0 gap-1 items-center">
        <button
          :class="[
            'px-1.5 py-0.5 text-[9px] relative transition-colors',
            compareDisabled
              ? (isCheckpointCheckedOut ? 'bg-[var(--vscode-textLink-foreground)] text-[var(--vscode-editor-background)]' : 'bg-[var(--vscode-descriptionForeground)] text-[var(--vscode-editor-background)]')
              : 'bg-transparent',
            !compareDisabled ? (isCheckpointCheckedOut ? 'text-[var(--vscode-textLink-foreground)]' : 'text-[var(--vscode-descriptionForeground)]') : '',
            compareDisabled ? 'cursor-wait opacity-50' : 'cursor-pointer'
          ]"
          :style="compareButtonStyle"
          :disabled="compareDisabled"
          @click="handleCompare"
        >
          Compare
        </button>
        <div
          class="w-[5px] min-w-[5px] h-[1px] flex-shrink-0 bg-[length:4px_1px] bg-repeat-x"
          :style="dottedLineStyle"
        />
        <div ref="restoreButtonRef" class="relative -mt-0.5">
          <button
            :class="[
              'px-1.5 py-0.5 text-[9px] cursor-pointer relative transition-colors',
              (showRestoreConfirm || restoreTaskDisabled || restoreWorkspaceDisabled || restoreBothDisabled)
                ? (isCheckpointCheckedOut ? 'bg-[var(--vscode-textLink-foreground)] text-[var(--vscode-editor-background)]' : 'bg-[var(--vscode-descriptionForeground)] text-[var(--vscode-editor-background)]')
                : 'bg-transparent',
              !(showRestoreConfirm || restoreTaskDisabled || restoreWorkspaceDisabled || restoreBothDisabled)
                ? (isCheckpointCheckedOut ? 'text-[var(--vscode-textLink-foreground)]' : 'text-[var(--vscode-descriptionForeground)]')
                : ''
            ]"
            :style="restoreButtonStyle"
            @click="showRestoreConfirm = true"
            @mouseenter="cancelCloseRestore"
            @mouseleave="scheduleCloseRestore"
          >
            Restore
          </button>
          <Teleport to="body">
            <div
              v-if="showRestoreConfirm"
              :class="[
                'fixed border border-[var(--vscode-editorGroup-border)] p-3 rounded z-[1000]',
                'w-[min(calc(100vw-54px),600px)]'
              ]"
              :style="tooltipData.style"
              :data-placement="tooltipData.placement"
              @mouseenter="cancelCloseRestore"
              @mouseleave="scheduleCloseRestore"
            >
              <!-- Arrow -->
              <div
                :class="[
                  'absolute w-[10px] h-[10px] border-l border-t border-[var(--vscode-editorGroup-border)] rotate-45 z-[1] right-6',
                  tooltipData.placement === 'top-end' ? 'bottom-[-6px]' : 'top-[-6px]'
                ]"
                :style="{ background: CODE_BLOCK_BG_COLOR }"
              />
              <!-- Invisible padding for hover zone -->
              <div :class="[tooltipData.placement === 'top-end' ? 'bottom-[-8px]' : 'top-[-8px]', 'absolute left-0 right-0 h-2']" />

              <div class="mb-2.5 pb-1 border-b border-[var(--vscode-editorGroup-border)]">
                <button
                  class="w-full mb-2.5 px-4 py-2 rounded border cursor-pointer transition-colors bg-[var(--vscode-button-background)] text-[var(--vscode-button-foreground)] border-[var(--vscode-button-border)] hover:bg-[var(--vscode-button-hoverBackground)] disabled:opacity-50 disabled:cursor-not-allowed"
                  :disabled="restoreWorkspaceDisabled || isCheckpointCheckedOut"
                  :style="{
                    cursor: isCheckpointCheckedOut ? 'not-allowed' : restoreWorkspaceDisabled ? 'wait' : 'pointer'
                  }"
                  @click="handleRestoreWorkspace"
                >
                  Restore Files
                </button>
                <p class="m-0 mb-0.5 text-[var(--vscode-descriptionForeground)] text-[11px] leading-[14px]">
                  Restores your project's files back to a snapshot taken at this point (use "Compare" to see what will be reverted)
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
                  Deletes messages after this point (does not affect workspace files)
                </p>
              </div>
              
              <div>
                <button
                  class="w-full mb-2.5 px-4 py-2 rounded border cursor-pointer transition-colors bg-[var(--vscode-button-background)] text-[var(--vscode-button-foreground)] border-[var(--vscode-button-border)] hover:bg-[var(--vscode-button-hoverBackground)] disabled:opacity-50 disabled:cursor-not-allowed"
                  :disabled="restoreBothDisabled"
                  :style="{ cursor: restoreBothDisabled ? 'wait' : 'pointer' }"
                  @click="handleRestoreBoth"
                >
                  Restore Files & Task
                </button>
                <p class="m-0 -mb-0.5 text-[var(--vscode-descriptionForeground)] text-[11px] leading-[14px]">
                  Restores your project's files and deletes all messages after this point
                </p>
              </div>
            </div>
          </Teleport>
        </div>
        <div
          class="w-[5px] min-w-[5px] h-[1px] flex-shrink-0 bg-[length:4px_1px] bg-repeat-x"
          :style="dottedLineStyle"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ClineCheckpointRestore } from '@/shared/WebviewMessage'
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { CODE_BLOCK_BG_COLOR } from './CodeBlock'
import { checkpointsService } from '@/api/checkpoints'
import { useExtensionStateStore } from '@/stores/extensionState'

interface Props {
  messageTs?: number
  isCheckpointCheckedOut?: boolean
}

const props = defineProps<Props>()

const compareDisabled = ref(false)
const restoreTaskDisabled = ref(false)
const restoreWorkspaceDisabled = ref(false)
const restoreBothDisabled = ref(false)
const showRestoreConfirm = ref(false)

const restoreButtonRef = ref<HTMLDivElement | null>(null)
const scrollVersion = ref(0)

let closeMenuTimeoutRef: ReturnType<typeof setTimeout> | null = null

const extensionStateStore = useExtensionStateStore()

const dottedLineColor = computed(() =>
  props.isCheckpointCheckedOut ? 'var(--vscode-textLink-foreground)' : 'var(--vscode-descriptionForeground)'
)
const dottedLineStyle = computed(() => ({
  backgroundImage: `linear-gradient(to right, ${dottedLineColor.value} 50%, transparent 50%)`
}))

const compareButtonStyle = computed(() => {
  if (compareDisabled.value) return { border: 'none' }
  const color = props.isCheckpointCheckedOut ? 'var(--vscode-textLink-foreground)' : 'var(--vscode-descriptionForeground)'
  return { border: `1px dashed ${color}` }
})

const restoreButtonStyle = computed(() => {
  const isActive = showRestoreConfirm.value || restoreTaskDisabled.value || restoreWorkspaceDisabled.value || restoreBothDisabled.value
  if (isActive) return { border: 'none' }
  const color = props.isCheckpointCheckedOut ? 'var(--vscode-textLink-foreground)' : 'var(--vscode-descriptionForeground)'
  return { border: `1px dashed ${color}` }
})

const scheduleCloseRestore = () => {
  if (closeMenuTimeoutRef) clearTimeout(closeMenuTimeoutRef)
  closeMenuTimeoutRef = setTimeout(() => {
    showRestoreConfirm.value = false
  }, 350)
}

const cancelCloseRestore = () => {
  if (closeMenuTimeoutRef) {
    clearTimeout(closeMenuTimeoutRef)
    closeMenuTimeoutRef = null
  }
}

const handleControlsMouseEnter = () => cancelCloseRestore()
const handleControlsMouseLeave = () => scheduleCloseRestore()

const updateTooltipPosition = () => {
  if (!restoreButtonRef.value || !showRestoreConfirm.value) return
  scrollVersion.value++
}

const tooltipData = computed(() => {
  scrollVersion.value
  if (!restoreButtonRef.value || !showRestoreConfirm.value) {
    return { style: {} as Record<string, string>, placement: 'bottom-end' as const }
  }
  const buttonRect = restoreButtonRef.value.getBoundingClientRect()
  const spaceBelow = window.innerHeight - buttonRect.bottom
  const spaceAbove = buttonRect.top
  const useTop = spaceBelow < 300 && spaceAbove > spaceBelow
  const right = `${window.innerWidth - buttonRect.right + 10}px`
  if (useTop) {
    return {
      style: {
        bottom: `${window.innerHeight - buttonRect.top + 8}px`,
        right,
        background: CODE_BLOCK_BG_COLOR
      },
      placement: 'top-end' as const
    }
  }
  return {
    style: {
      top: `${buttonRect.bottom + 8}px`,
      right,
      background: CODE_BLOCK_BG_COLOR
    },
    placement: 'bottom-end' as const
  }
})

const handleCompare = async () => {
  compareDisabled.value = true
  try {
    await checkpointsService.checkpointDiff(
      props.messageTs || 0,
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
    const restoreType: ClineCheckpointRestore = 'task'
    await checkpointsService.checkpointRestore({
      taskId: extensionStateStore.conversationId ?? undefined,
      number: props.messageTs || 0,
      restoreType,
    })
  } catch (err) {
    console.error('Checkpoint restore task error:', err)
    restoreTaskDisabled.value = false
  }
}

const handleRestoreWorkspace = async () => {
  restoreWorkspaceDisabled.value = true
  try {
    const restoreType: ClineCheckpointRestore = 'workspace'
    await checkpointsService.checkpointRestore({
      taskId: extensionStateStore.conversationId ?? undefined,
      number: props.messageTs || 0,
      restoreType,
    })
  } catch (err) {
    console.error('Checkpoint restore workspace error:', err)
    restoreWorkspaceDisabled.value = false
  }
}

const handleRestoreBoth = async () => {
  restoreBothDisabled.value = true
  try {
    const restoreType: ClineCheckpointRestore = 'taskAndWorkspace'
    await checkpointsService.checkpointRestore({
      taskId: extensionStateStore.conversationId ?? undefined,
      number: props.messageTs || 0,
      restoreType,
    })
  } catch (err) {
    console.error('Checkpoint restore both error:', err)
    restoreBothDisabled.value = false
  }
}

const handleScroll = () => updateTooltipPosition()

watch(() => props.isCheckpointCheckedOut, (newVal) => {
  if (!newVal && restoreWorkspaceDisabled.value) {
    restoreWorkspaceDisabled.value = false
  }
})

watch(showRestoreConfirm, async (newVal) => {
  if (newVal) {
    await nextTick()
    updateTooltipPosition()
    window.addEventListener('scroll', handleScroll, true)
  } else {
    window.removeEventListener('scroll', handleScroll, true)
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
  if (closeMenuTimeoutRef) clearTimeout(closeMenuTimeoutRef)
  window.removeEventListener('scroll', handleScroll, true)
  cleanupRelinquishControl?.()
})
</script>
