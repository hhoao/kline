<template>
  <div v-if="checkpointManagerErrorMessage && !dismissed" class="flex items-center justify-center w-full">
    <div
      class="rounded-sm border-0 bg-[var(--vscode-inputValidation-errorBackground)] text-[var(--vscode-inputValidation-errorForeground)] pl-1 pr-1.5 py-1 flex items-center gap-2"
    >
      <div class="flex gap-2 flex-1">
        <button
          v-if="messages.showDisableButton"
          class="underline cursor-pointer bg-transparent border-0 p-0 text-inherit"
          @click="handleCheckpointSettingsClick"
        >
          Disable Checkpoints
        </button>
        <a
          v-if="messages.showGitInstructions"
          class="text-[var(--vscode-textLink-foreground)] underline"
          href="https://github.com/cline/cline/wiki/Installing-Git-for-Checkpoints"
        >
          See instructions
        </a>
      </div>
      <button
        aria-label="Dismiss"
        class="inline-flex opacity-100 hover:bg-transparent hover:opacity-60 p-0 bg-transparent border-0 cursor-pointer"
        title="Dismiss Checkpoint Error"
        @click="dismissed = true"
      >
        <svg
          xmlns="http://www.w3.org/2000/svg"
          width="12"
          height="12"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M18 6L6 18" />
          <path d="M6 6l12 12" />
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

interface Props {
  checkpointManagerErrorMessage?: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  checkpointSettingsClick: []
}>()

const dismissed = ref(false)

const messages = computed(() => {
  const message = props.checkpointManagerErrorMessage?.replace(/disabling checkpoints\.$/, '')
  const showDisableButton =
    props.checkpointManagerErrorMessage?.endsWith('disabling checkpoints.') ||
    props.checkpointManagerErrorMessage?.includes('multi-root workspaces')
  const showGitInstructions = props.checkpointManagerErrorMessage?.includes('Git must be installed to use checkpoints.')
  return { message, showDisableButton, showGitInstructions }
})

const handleCheckpointSettingsClick = () => {
  emit('checkpointSettingsClick')
}
</script>

