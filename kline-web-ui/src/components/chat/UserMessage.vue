<template>
  <div
    :class="[
      'rounded-[3px] p-2.25 whitespace-pre-line break-words',
      isEditing ? '' : 'bg-[var(--vscode-badge-background)] text-[var(--vscode-badge-foreground)]',
    ]"
    @click="handleClick"
  >
    <template v-if="isEditing">
      <textarea
        ref="textAreaRef"
        v-model="editedText"
        class="w-full bg-[var(--vscode-input-background)] text-[var(--vscode-input-foreground)] border border-[var(--vscode-input-border)] rounded-[2px] px-1.5 py-1.5 font-inherit leading-inherit box-border resize-none overflow-x-hidden overflow-y-scroll"
        :style="{
          scrollbarWidth: 'none',
          minHeight: '60px',
          maxHeight: '300px',
          height: textAreaHeight + 'px',
        }"
        @blur="handleBlur"
        @keydown="handleKeyDown"
        @input="handleInput"
      ></textarea>
      <div class="flex gap-2 justify-end mt-2">
        <button
          v-if="!checkpointManagerErrorMessage"
          ref="restoreAllButtonRef"
          class="border-0 px-2 py-1 rounded-[2px] text-[9px] cursor-pointer bg-[var(--vscode-button-secondaryBackground,var(--vscode-descriptionForeground))] text-[var(--vscode-button-secondaryForeground,var(--vscode-foreground))]"
          title="Restore both the chat and workspace files to this checkpoint and send your edited message"
          @click.stop="handleRestoreWorkspace('taskAndWorkspace')"
        >
          Restore All
        </button>
        <button
          ref="restoreChatButtonRef"
          class="border-0 px-2 py-1 rounded-[2px] text-[9px] cursor-pointer bg-[var(--vscode-button-background)] text-[var(--vscode-button-foreground)]"
          title="Restore just the chat to this checkpoint and send your edited message"
          @click.stop="handleRestoreWorkspace('task')"
        >
          Restore Chat
        </button>
      </div>
    </template>
    <template v-else>
      <span class="block ph-no-capture">
        <component :is="highlightedText" />
      </span>
    </template>
    <Thumbnails
      v-if="(images && images.length > 0) || (files && files.length > 0)"
      :files="files ?? []"
      :images="images ?? []"
      :style="{ marginTop: '8px' }"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import type { ClineCheckpointRestore } from '@/shared/WebviewMessage'
import { useExtensionStateStore } from "@/stores/extensionState"
import Thumbnails from '@/components/common/Thumbnails.vue'
import type { CheckpointRestoreRequest } from '@/shared/proto/cline/checkpoints'
import { highlightText } from './task-header/Highlights'
import { checkpointsService } from '@/api/checkpoints'

interface Props {
  text?: string
  files?: string[]
  images?: string[]
  messageTs?: number
  sendMessageFromChatRow?: (text: string, images: string[], files: string[]) => void
}

const props = defineProps<Props>()

const extensionState = computed(() => useExtensionStateStore().extensionState)
const checkpointManagerErrorMessage = computed(() => extensionState.value?.checkpointManagerErrorMessage)

const isEditing = ref(false)
const editedText = ref(props.text || '')
const textAreaRef = ref<HTMLTextAreaElement | null>(null)
const restoreAllButtonRef = ref<HTMLButtonElement | null>(null)
const restoreChatButtonRef = ref<HTMLButtonElement | null>(null)
const textAreaHeight = ref(60)

const highlightedText = computed(() => {
  const result = highlightText(editedText.value || props.text)
  if (typeof result === 'string') {
    return () => result
  }
  return () => result
})

const handleClick = () => {
  if (!isEditing.value) {
    isEditing.value = true
  }
}

// Select all text when entering edit mode
watch(isEditing, async (newVal) => {
  if (newVal && textAreaRef.value) {
    await nextTick()
    textAreaRef.value.select()
    adjustTextAreaHeight()
  }
})

const adjustTextAreaHeight = () => {
  if (textAreaRef.value) {
    textAreaRef.value.style.height = 'auto'
    const scrollHeight = textAreaRef.value.scrollHeight
    textAreaHeight.value = Math.min(Math.max(scrollHeight, 60), 300)
  }
}

const handleInput = () => {
  adjustTextAreaHeight()
}

const handleRestoreWorkspace = async (type: ClineCheckpointRestore) => {
  const delay = type === 'task' ? 500 : 1000
  isEditing.value = false

  if (props.text === editedText.value) {
    return
  }

  try {
    const extensionStateStore = useExtensionStateStore()
    const request: CheckpointRestoreRequest = {
      taskId: extensionStateStore.conversationId ?? undefined,
      number: props.messageTs || 0,
      restoreType: type,
      offset: 1,
    }
    await checkpointsService.checkpointRestore(request)

    setTimeout(() => {
      props.sendMessageFromChatRow?.(editedText.value, props.images || [], props.files || [])
    }, delay)
  } catch (err) {
    console.error('Checkpoint restore error:', err)
  }
}

const handleBlur = (e: FocusEvent) => {
  // Check if focus is moving to one of our button elements
  if (
    e.relatedTarget === restoreAllButtonRef.value ||
    e.relatedTarget === restoreChatButtonRef.value
  ) {
    // Don't close edit mode if focus is moving to one of our buttons
    return
  }

  // Otherwise, close edit mode
  isEditing.value = false
}

const handleKeyDown = (e: KeyboardEvent) => {
  if (e.key === 'Escape') {
    isEditing.value = false
  } else if (e.key === 'Enter' && (e.metaKey || e.ctrlKey) && !checkpointManagerErrorMessage.value) {
    e.preventDefault()
    handleRestoreWorkspace('taskAndWorkspace')
  } else if (
    e.key === 'Enter' &&
    !e.shiftKey &&
    !(e as any).isComposing &&
    (e as any).keyCode !== 229
  ) {
    e.preventDefault()
    handleRestoreWorkspace('task')
  }
}
</script>

