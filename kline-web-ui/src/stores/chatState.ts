import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { ClineMessage, ClineAsk } from '@/shared/ExtensionMessage'
import { useExtensionStateStore } from './extensionState'
import { getButtonConfig } from '@/components/chat/chat-view/shared/buttonConfig'

export const useChatStateStore = defineStore('chatState', () => {
  const extensionStateStore = useExtensionStateStore()

  const inputValue = ref('')
  const activeQuote = ref<string | null>(null)
  const isTextAreaFocused = ref(false)
  const selectedImages = ref<string[]>([])
  const selectedFiles = ref<string[]>([])

  const sendingDisabled = ref(false)
  const enableButtons = ref(false)
  const primaryButtonText = ref<string | undefined>('Approve')
  const secondaryButtonText = ref<string | undefined>('Reject')
  const expandedRows = ref<Record<number, boolean>>({})

  const messages = computed((): ClineMessage[] => {
    return extensionStateStore.extensionState?.clineMessages || []
  })

  const lastMessage = computed((): ClineMessage | undefined => {
    const msgs = messages.value
    return msgs.length > 0 ? msgs[msgs.length - 1] : undefined
  })

  const secondLastMessage = computed((): ClineMessage | undefined => {
    const msgs = messages.value
    return msgs.length > 1 ? msgs[msgs.length - 2] : undefined
  })

  const clineAsk = computed((): ClineAsk | undefined => {
    const last = lastMessage.value
    return last?.type === 'ask' ? last.ask : undefined
  })

  const task = computed((): ClineMessage | undefined => {
    const msgs = messages.value
    return msgs.length > 0 ? msgs[0] : undefined
  })

  function setInputValue(value: string): void {
    inputValue.value = value
  }

  function setActiveQuote(value: string | null): void {
    activeQuote.value = value
  }

  function setIsTextAreaFocused(value: boolean): void {
    isTextAreaFocused.value = value
  }

  function setSelectedImages(value: string[]): void {
    selectedImages.value = value
  }

  function setSelectedFiles(value: string[]): void {
    selectedFiles.value = value
  }

  function setSendingDisabled(value: boolean): void {
    sendingDisabled.value = value
  }

  function setEnableButtons(value: boolean): void {
    enableButtons.value = value
  }

  function setPrimaryButtonText(value: string | undefined): void {
    primaryButtonText.value = value
  }

  function setSecondaryButtonText(value: string | undefined): void {
    secondaryButtonText.value = value
  }

  function setExpandedRows(value: Record<number, boolean>): void {
    expandedRows.value = value
  }

  function handleFocusChange(isFocused: boolean): void {
    isTextAreaFocused.value = isFocused
  }

  function clearExpandedRows(): void {
    expandedRows.value = {}
  }

  function resetState(): void {
    inputValue.value = ''
    activeQuote.value = null
    selectedImages.value = []
    selectedFiles.value = []
  }

  extensionStateStore.$subscribe((_mutation, _state) => {
    const buttonConfig = getButtonConfig(lastMessage.value, undefined)
    setSendingDisabled(buttonConfig.sendingDisabled)
    setEnableButtons(buttonConfig.enableButtons)
    setPrimaryButtonText(buttonConfig.primaryText)
    setSecondaryButtonText(buttonConfig.secondaryText)
  })

  return {
    inputValue,
    activeQuote,
    isTextAreaFocused,
    selectedImages,
    selectedFiles,
    sendingDisabled,
    enableButtons,
    primaryButtonText,
    secondaryButtonText,
    expandedRows,
    messages,
    lastMessage,
    secondLastMessage,
    clineAsk,
    task,
    setInputValue,
    setActiveQuote,
    setIsTextAreaFocused,
    setSelectedImages,
    setSelectedFiles,
    setSendingDisabled,
    setEnableButtons,
    setPrimaryButtonText,
    setSecondaryButtonText,
    setExpandedRows,
    handleFocusChange,
    clearExpandedRows,
    resetState,
  }
})
