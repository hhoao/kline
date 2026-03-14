<template>
  <div
    class="flex relative flex-col gap-1 px-3 py-2 m-4 mt-1.5 mb-1 w-auto text-sm text-left no-underline border-0 transition-colors cursor-pointer shrink-0 hover:brightness-120"
    style="background-color: var(--vscode-list-inactiveSelectionBackground); border-radius: 3px; color: var(--vscode-foreground)"
    @click="handleBannerClick"
  >
    <h4 class="flex gap-2 items-center m-0" style="padding-right: 18px">
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5.882V19.24a1.76 1.76 0 01-3.417.592l-2.147-6.15M18 13a3 3 0 100-6M5.436 13.683A4.001 4.001 0 017 6h1.832c4.1 0 7.625-1.234 9.168-3v14c-1.543-1.766-5.067-3-9.168-3H7a3.988 3.988 0 01-1.564-.317z" />
      </svg>
      Claude Haiku 4.5
    </h4>
    <p class="m-0">
      Anthropic's fastest model with frontier-level coding intelligence at a fraction of the cost.
      <span class="cursor-pointer text-link">{{ user ? 'Try new model' : 'Try with Cline account' }} →</span>
    </p>

    <!-- Close button -->
    <button
      class="absolute top-1.5 right-1.5 p-1 bg-transparent border-0 cursor-pointer hover:opacity-80 text-inherit"
      data-testid="info-banner-close-button"
      @click.stop="handleClose"
    >
      <span class="i-codicon:close"></span>
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { accountService } from '@/api/account'
import { stateService } from '@/api/state'
export const CURRENT_MODEL_BANNER_VERSION = 2

// TODO: Replace with actual composables/stores when available
const user = ref<any>(null) // clineUser
const openRouterModels = ref<Record<string, any>>({})
const setShowChatModelSelector = (value: boolean) => {
  // TODO: Implement
  console.log('setShowChatModelSelector', value)
}
const refreshOpenRouterModels = () => {
  // TODO: Implement
  console.log('refreshOpenRouterModels')
}
const handleFieldsChange = (fields: Record<string, any>) => {
  // TODO: Implement useApiConfigurationHandlers
  console.log('handleFieldsChange', fields)
}

onMounted(() => {
  refreshOpenRouterModels()
})

const handleClose = (e?: MouseEvent) => {
  e?.preventDefault()
  e?.stopPropagation()

  stateService.updateModelBannerVersion(CURRENT_MODEL_BANNER_VERSION).catch(
    console.error
  )
}

const setNewModel = () => {
  const modelId = 'anthropic/claude-haiku-4.5'
  handleFieldsChange({
    planModeOpenRouterModelId: modelId,
    actModeOpenRouterModelId: modelId,
    planModeOpenRouterModelInfo: openRouterModels.value[modelId],
    actModeOpenRouterModelInfo: openRouterModels.value[modelId],
    planModeApiProvider: 'cline',
    actModeApiProvider: 'cline',
  })

  setTimeout(() => {
    setShowChatModelSelector(true)
  }, 10)

  setTimeout(() => {
    handleClose()
  }, 50)
}

const handleShowAccount = () => {
  accountService.accountLoginClicked().catch((err: any) =>
    console.error('Failed to get login URL:', err)
  )
}

const handleBannerClick = () => {
  if (user.value) {
    setNewModel()
  } else {
    handleShowAccount()
  }
}
</script>

