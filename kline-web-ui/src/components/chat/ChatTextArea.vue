<template>
  <div>
    <div
      ref="containerRef"
      class="flex relative px-3.5 py-2.5 transition-colors duration-100 ease-in-out"
      @dragenter="handleDragEnter"
      @dragleave="handleDragLeave"
      @dragover="onDragOver"
      @drop="onDrop"
    >
      <!-- Voice Recording Pulsing Border -->
      <div
        v-if="isVoiceRecording"
        class="overflow-hidden absolute top-2.5 bottom-2.5 right-3.5 left-3.5 z-10 border-2 border-purple-500 transition-all duration-300 ease-in-out animate-pulse pointer-events-none rounded-xs"
      />

      <!-- Dimension Error -->
      <div
        v-if="showDimensionError"
        class="absolute inset-2.5 bg-[rgba(var(--vscode-errorForeground-rgb),0.1)] border-2 border-error rounded-xs flex items-center justify-center z-10 pointer-events-none"
      >
        <span class="text-xs font-bold text-center text-error">Image dimensions exceed 7500px</span>
      </div>

      <!-- Unsupported File Error -->
      <div
        v-if="showUnsupportedFileError"
        class="absolute inset-2.5 bg-[rgba(var(--vscode-errorForeground-rgb),0.1)] border-2 border-error rounded-xs flex items-center justify-center z-10 pointer-events-none"
      >
        <span class="text-xs font-bold text-error">Files other than images are currently disabled</span>
      </div>

      <!-- Highlight Layer -->
      <div
        ref="highlightLayerRef"
        class="overflow-hidden absolute top-2.5 bottom-2.5 whitespace-pre-wrap break-words rounded-xs bg-input-background"
        :class="[
          isTextAreaFocused || isVoiceRecording
            ? 'left-3.5 right-3.5'
            : 'left-3.5 right-3.5 border border-input-border',
        ]"
        :style="highlightLayerStyle"
      />

      <!-- Textarea -->
      <textarea
        ref="textAreaRef"
        autofocus
        data-testid="chat-input"
        :placeholder="showUnsupportedFileError || showDimensionError ? '' : placeholderText"
        :value="inputValue"
        :style="textareaStyle"
        class="resize-none"
        @blur="handleBlur"
        @focus="handleFocus"
        @keydown="handleKeyDown"
        @keyup="handleKeyUp"
        @mouseup="updateCursorPosition"
        @paste="handlePaste"
        @scroll="updateHighlights"
        @select="updateCursorPosition"
        @input="handleInputChange"
      />

      <!-- Placeholder Text -->
      <div
        v-if="!inputValue && selectedImages.length === 0 && selectedFiles.length === 0"
        class="text-[10px] absolute bottom-5 left-5 right-16 text-[var(--vscode-input-placeholderForeground)]/50 whitespace-nowrap overflow-hidden text-ellipsis pointer-events-none z-1"
      >
        Type @ for context, / for slash commands & workflows, hold shift to drag in files/images
      </div>

      <!-- Thumbnails -->
      <Thumbnails
        v-if="selectedImages.length > 0 || selectedFiles.length > 0"
        :files="selectedFiles"
        :images="selectedImages"
        :on-height-change="handleThumbnailsHeightChange"
        :set-files="setSelectedFiles"
        :set-images="setSelectedImages"
        :style="{
          position: 'absolute',
          paddingTop: '4px',
          bottom: '14px',
          left: '22px',
          right: '47px',
          zIndex: 2,
        }"
      />

      <!-- Action Button: Cancel / Send / Voice -->
      <div
        class="flex absolute right-5 bottom-5 z-10 items-end h-8 text-xs"
        :style="{ height: textAreaBaseHeight }"
      >
        <div class="flex flex-row items-center">
          <!-- Send button: highest priority when has content -->
          <div
            v-if="hasInputContent && !isVoiceRecording"
            class="text-sm input-icon-button i-codicon:send"
            @click="handleActionButtonClick"
          >Send</div>
          <!-- Cancel button: when AI is generating and no input -->
          <div
            v-else-if="sendingDisabled && !isVoiceRecording"
            class="text-sm input-icon-button i-codicon:stop-circle"
            @click="handleCancelClick"
          >取消</div>
          <!-- Voice button: idle with no input -->
          <div
            v-else-if="!isVoiceRecording"
            class="text-sm input-icon-button i-codicon:mic"
            @click="handleVoiceInputClick"
          >语音</div>
        </div>
      </div>
    </div>

    <!-- Bottom Controls -->
    <div class="flex justify-between items-center px-3 pb-2 -mt-1">
      <div class="flex items-center gap-1">
        <ServersToggleModal />
        <!-- Plan/Act Toggle -->
        <Tooltip
        :hint-text="`Toggle w/ ${togglePlanActKeys}`"
        :style="{ zIndex: 1000 }"
        :tip-text="`In ${shownTooltipMode === 'act' ? 'Act' : 'Plan'} mode, Cline will ${shownTooltipMode === 'act' ? 'complete the task immediately' : 'gather information to architect a plan'}`"
        :visible="shownTooltipMode !== null"
      >
        <div
          data-testid="mode-switch"
          :class="[
            'flex items-center bg-[var(--vscode-editor-background)] border border-[var(--vscode-input-border)] rounded-xl overflow-hidden cursor-pointer opacity-100 scale-[0.85] origin-right -ml-[10px] select-none',
            { 'opacity-50 cursor-not-allowed': false },
          ]"
          @click="onModeToggle"
        >
          <div
            :class="[
              'absolute h-full w-1/2 transition-transform duration-200 ease-in-out',
              mode === 'plan' ? 'bg-[var(--vscode-activityWarningBadge-background)]' : 'bg-[var(--vscode-focusBorder)]',
            ]"
            :style="{
              transform: mode === 'act' ? 'translateX(100%)' : 'translateX(0%)',
            }"
          />
          <div
            :class="[
              'px-[8px] py-[2px] z-[1] transition-colors duration-200 ease-in-out text-[12px] w-1/2 text-center',
              mode === 'plan' ? 'text-white' : 'text-[var(--vscode-input-foreground)]',
            ]"
            :aria-checked="mode === 'plan'"
            role="switch"
            @mouseleave="setShownTooltipMode(null)"
            @mouseover="setShownTooltipMode('plan')"
          >
            Plan
          </div>
          <div
            :class="[
              'px-[8px] py-[2px] z-[1] transition-colors duration-200 ease-in-out text-[12px] w-1/2 text-center',
              mode === 'act' ? 'text-white' : 'text-[var(--vscode-input-foreground)]',
            ]"
            :aria-checked="mode === 'act'"
            role="switch"
            @mouseleave="setShownTooltipMode(null)"
            @mouseover="setShownTooltipMode('act')"
          >
            Act
          </div>
        </div>
      </Tooltip>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { fileService } from '@/api/file'
import ServersToggleModal from './ServersToggleModal.vue'
import { modelsService } from '@/api/models'
import { stateService } from '@/api/state'
import { CHAT_CONSTANTS } from '@/components/chat/chat-view/constants'
import Thumbnails from '@/components/common/Thumbnails.vue'
import Tooltip from '@/components/common/Tooltip.vue'
import { mentionRegex, mentionRegexGlobal } from '@/shared/context-mentions'
import { FileSearchResults, FileSearchType } from '@/shared/proto/cline/file'
import { PlanActMode } from '@/shared/proto/cline/state'
import { Mode } from '@/shared/storage/types'
import { useExtensionStateStore } from "@/stores/extensionState"
import {
  ContextMenuOptionType,
  getContextMenuOptionIndex,
  getContextMenuOptions,
  insertMention,
  insertMentionDirectly,
  removeMention,
  type SearchResult,
  shouldShowContextMenu,
} from '@/utils/context-mentions'
import { isSafari } from '@/utils/platformUtils'
import {
  getMatchingSlashCommands,
  insertSlashCommand,
  removeSlashCommand,
  shouldShowSlashCommandsMenu,
  type SlashCommand,
  slashCommandDeleteRegex,
  validateSlashCommand,
} from '@/utils/slash-commands'
import { validateApiConfiguration, validateModelId } from '@/utils/validate'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useChatStateStore } from '@/stores/chatState'
import { taskService } from '@/api/task'
import { getButtonConfig } from '@/components/chat/chat-view/shared/buttonConfig'

const buttonConfig = computed(() => {
  return lastMessage.value ? getButtonConfig(lastMessage.value, mode.value) : { sendingDisabled: false, enableButtons: false }
})

const { MAX_IMAGES_AND_FILES_PER_MESSAGE } = CHAT_CONSTANTS

const DEFAULT_CONTEXT_MENU_OPTION = getContextMenuOptionIndex(ContextMenuOptionType.File)

interface Props {
  placeholderText: string
  onSendMessage: () => void
  onSelectFilesAndImages: () => void
  shouldDisableFilesAndImages: boolean
  onHeightChange?: (height: number) => void
}

interface GitCommit {
  type: ContextMenuOptionType.Git
  value: string
  label: string
  description: string
}

const props = defineProps<Props>()

const chatStateStore = useChatStateStore()
const {
  inputValue,
  selectedImages,
  selectedFiles,
  sendingDisabled,
  lastMessage,
} = storeToRefs(chatStateStore)
const {
  setInputValue,
  setSelectedImages,
  setSelectedFiles,
  handleFocusChange,
} = chatStateStore

const extensionStateStore = useExtensionStateStore()
const extensionState = computed(()=> extensionStateStore.extensionState)

const hasInputContent = computed(() => {
  return inputValue.value.trim().length > 0 || selectedImages.value.length > 0 || selectedFiles.value.length > 0
})

const mode = computed(() => extensionState.value?.mode || 'plan')
const apiConfiguration = computed(() => extensionState.value?.apiConfiguration)
const openRouterModels = computed(() => (extensionState.value as any)?.openRouterModels || [])
const platform = computed(() => {
  const plat = extensionState.value?.platform
  if (typeof plat === 'object' && plat !== null && 'togglePlanActKeys' in plat) {
    return plat as { togglePlanActKeys: string }
  }
  return { togglePlanActKeys: 'Meta+P' }
})
const localWorkflowToggles = computed(() => extensionState.value?.localWorkflowToggles || {})
const globalWorkflowToggles = computed(() => extensionState.value?.globalWorkflowToggles || {})
const showModelSelector = ref(false)
const dictationSettings = computed(() => extensionState.value?.dictationSettings)
const clineUser = computed(() => extensionState.value?.userInfo)

// State
const isTextAreaFocused = ref(false)
const isDraggingOver = ref(false)
const gitCommits = ref<GitCommit[]>([])
const isVoiceRecording = ref(false)
const showSlashCommandsMenu = ref(false)
const selectedSlashCommandsIndex = ref(0)
const slashCommandsQuery = ref('')
const thumbnailsHeight = ref(0)
const textAreaBaseHeight = ref<number | undefined>(undefined)
const showContextMenu = ref(false)
const cursorPosition = ref(0)
const searchQuery = ref('')
const isMouseDownOnMenu = ref(false)
const selectedMenuIndex = ref(-1)
const selectedType = ref<ContextMenuOptionType | null>(null)
const justDeletedSpaceAfterMention = ref(false)
const justDeletedSpaceAfterSlashCommand = ref(false)
const intendedCursorPosition = ref<number | null>(null)
const fileSearchResults = ref<SearchResult[]>([])
const searchLoading = ref(false)
const arrowPosition = ref(0)
const menuPosition = ref(0)
const shownTooltipMode = ref<Mode | null>(null)
const pendingInsertions = ref<string[]>([])
const showUnsupportedFileError = ref(false)
const showDimensionError = ref(false)
const prevShowModelSelector = ref(false)

// Refs
const containerRef = ref<HTMLDivElement | null>(null)
const textAreaRef = ref<HTMLTextAreaElement | null>(null)
const highlightLayerRef = ref<HTMLDivElement | null>(null)
const contextMenuContainerRef = ref<HTMLDivElement | null>(null)
const slashCommandsMenuContainerRef = ref<HTMLDivElement | null>(null)
const modelSelectorRef = ref<HTMLDivElement | null>(null)
const buttonRef = ref<HTMLDivElement | null>(null)
const modelSelectorTooltipRef = ref<HTMLDivElement | null>(null)

// Timers
const searchTimeoutRef = ref<NodeJS.Timeout | null>(null)
const unsupportedFileTimerRef = ref<NodeJS.Timeout | null>(null)
const dimensionErrorTimerRef = ref<NodeJS.Timeout | null>(null)
// const _shiftHoldTimerRef = ref<NodeJS.Timeout | null>(null) // Unused for now

const currentSearchQueryRef = ref('')

// Window size
const viewportWidth = ref(window.innerWidth)
const viewportHeight = ref(window.innerHeight)

onMounted(() => {
  const handleResize = () => {
    viewportWidth.value = window.innerWidth
    viewportHeight.value = window.innerHeight
  }
  window.addEventListener('resize', handleResize)
  onBeforeUnmount(() => {
    window.removeEventListener('resize', handleResize)
  })
})

// Computed
const queryItems = computed(() => {
  return [
    { type: ContextMenuOptionType.Problems, value: 'problems' },
    { type: ContextMenuOptionType.Terminal, value: 'terminal' },
    ...gitCommits.value,
  ]
})

const modelDisplayName = computed(() =>  'unknown model')

const togglePlanActKeys = computed(() => {
  const keys = platform.value.togglePlanActKeys
  const metaKeyChar = keys.includes('Meta') ? '⌘' : 'Ctrl'
  return keys.replace('Meta', metaKeyChar).replace(/.$/, (match: string) => match.toUpperCase())
})

const highlightLayerStyle = computed(() => ({
  position: 'absolute' as const,
  pointerEvents: 'none' as const,
  whiteSpace: 'pre-wrap' as const,
  wordWrap: 'break-word' as const,
  color: 'transparent',
  overflow: 'hidden',
  fontFamily: 'var(--vscode-font-family)',
  fontSize: 'var(--vscode-editor-font-size)',
  lineHeight: 'var(--vscode-editor-line-height)',
  borderRadius: '2px',
  borderLeft: isTextAreaFocused.value || isVoiceRecording.value ? '0' : undefined,
  borderRight: isTextAreaFocused.value || isVoiceRecording.value ? '0' : undefined,
  borderTop: isTextAreaFocused.value || isVoiceRecording.value ? '0' : undefined,
  borderBottom: isTextAreaFocused.value || isVoiceRecording.value ? '0' : undefined,
  padding: `9px 28px ${9 + thumbnailsHeight.value}px 9px`,
}))

const textareaStyle = computed(() => ({
  width: '100%',
  boxSizing: 'border-box' as const,
  backgroundColor: 'transparent',
  color: 'var(--vscode-input-foreground)',
  borderRadius: '2px',
  fontFamily: 'var(--vscode-font-family)',
  fontSize: 'var(--vscode-editor-font-size)',
  lineHeight: 'var(--vscode-editor-line-height)',
  resize: 'none' as const,
  overflowX: 'hidden' as const,
  overflowY: 'scroll' as const,
  scrollbarWidth: 'none' as const,
  borderLeft: '0',
  borderRight: '0',
  borderTop: '0',
  borderBottom: `${thumbnailsHeight.value}px solid transparent`,
  borderColor: 'transparent',
  padding: `9px ${dictationSettings.value?.dictationEnabled ? '48' : '28'}px 9px 9px`,
  cursor: 'text',
  flex: 1,
  zIndex: 1,
  outline:
    isDraggingOver.value && !showUnsupportedFileError.value
      ? '2px dashed var(--vscode-focusBorder)'
      : isTextAreaFocused.value
        ? `1px solid ${mode.value === 'plan' ? 'var(--vscode-activityWarningBadge-background)' : 'var(--vscode-focusBorder)'}`
        : 'none',
  outlineOffset: isDraggingOver.value && !showUnsupportedFileError.value ? '1px' : '0px',
}))

const modelSelectorTooltipStyle = computed(() => ({
  bottom: `calc(100vh - ${menuPosition.value}px + 6px)`,
}))

// Helper functions
const getImageDimensions = (dataUrl: string): Promise<{ width: number; height: number }> => {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => {
      if (img.naturalWidth > 7500 || img.naturalHeight > 7500) {
        reject(new Error('Image dimensions exceed maximum allowed size of 7500px.'))
      } else {
        resolve({ width: img.naturalWidth, height: img.naturalHeight })
      }
    }
    img.onerror = (err) => {
      console.error('Failed to load image for dimension check:', err)
      reject(new Error('Failed to load image to check dimensions.'))
    }
    img.src = dataUrl
  })
}

const showDimensionErrorMessage = () => {
  setShowDimensionError(true)
  if (dimensionErrorTimerRef.value) {
    clearTimeout(dimensionErrorTimerRef.value)
  }
  dimensionErrorTimerRef.value = setTimeout(() => {
    setShowDimensionError(false)
    dimensionErrorTimerRef.value = null
  }, 3000)
}

const setShowDimensionError = (value: boolean) => {
  showDimensionError.value = value
}

// Event Handlers
const handleMentionSelect = (type: ContextMenuOptionType, value?: string) => {
  if (type === ContextMenuOptionType.NoResults) {
    return
  }

  if (
    type === ContextMenuOptionType.File ||
    type === ContextMenuOptionType.Folder ||
    type === ContextMenuOptionType.Git
  ) {
    if (!value) {
      selectedType.value = type
      searchQuery.value = ''
      selectedMenuIndex.value = 0

      // Trigger search with the selected type
      if (type === ContextMenuOptionType.File || type === ContextMenuOptionType.Folder) {
        searchLoading.value = true

        // Map ContextMenuOptionType to FileSearchType enum
        let searchType: FileSearchType | undefined
        if (type === ContextMenuOptionType.File) {
          searchType = FileSearchType.FILE
        } else if (type === ContextMenuOptionType.Folder) {
          searchType = FileSearchType.FOLDER
        }

        fileService.searchFiles(
          {
            query: '',
            mentionsRequestId: '',
            selectedType: searchType,
            workspaceHint: '',
          },
        )
          .then((results: FileSearchResults) => {
            fileSearchResults.value = (results.results || []) as SearchResult[]
            searchLoading.value = false
          })
          .catch((error: any) => {
            console.error('Error searching files:', error)
            fileSearchResults.value = []
            searchLoading.value = false
          })
      }
      return
    }
  }

  showContextMenu.value = false
  selectedType.value = null
  const queryLength = searchQuery.value.length
  searchQuery.value = ''

  if (textAreaRef.value) {
    let insertValue = value || ''
    if (type === ContextMenuOptionType.URL) {
      insertValue = value || ''
    } else if (type === ContextMenuOptionType.File || type === ContextMenuOptionType.Folder) {
      insertValue = value || ''
    } else if (type === ContextMenuOptionType.Problems) {
      insertValue = 'problems'
    } else if (type === ContextMenuOptionType.Terminal) {
      insertValue = 'terminal'
    } else if (type === ContextMenuOptionType.Git) {
      insertValue = value || ''
    }

    const { newValue, mentionIndex } = insertMention(
      textAreaRef.value.value,
      cursorPosition.value,
      insertValue,
      queryLength,
    )

    setInputValue(newValue)
    const newCursorPosition = newValue.indexOf(' ', mentionIndex + insertValue.length) + 1
    cursorPosition.value = newCursorPosition
    intendedCursorPosition.value = newCursorPosition

    setTimeout(() => {
      if (textAreaRef.value) {
        textAreaRef.value.blur()
        textAreaRef.value.focus()
      }
    }, 0)
  }
}

const handleSlashCommandsSelect = (command: SlashCommand) => {
  showSlashCommandsMenu.value = false
  const queryLength = slashCommandsQuery.value.length
  slashCommandsQuery.value = ''

  if (textAreaRef.value) {
    const { newValue, commandIndex } = insertSlashCommand(
      textAreaRef.value.value,
      command.name,
      queryLength,
    )
    const newCursorPosition = newValue.indexOf(' ', commandIndex + 1 + command.name.length) + 1

    setInputValue(newValue)
    cursorPosition.value = newCursorPosition
    intendedCursorPosition.value = newCursorPosition

    setTimeout(() => {
      if (textAreaRef.value) {
        textAreaRef.value.blur()
        textAreaRef.value.focus()
      }
    }, 0)
  }
}

const handleKeyDown = (event: KeyboardEvent) => {
  if (showSlashCommandsMenu.value) {
    if (event.key === 'Escape') {
      showSlashCommandsMenu.value = false
      slashCommandsQuery.value = ''
      return
    }

    if (event.key === 'ArrowUp' || event.key === 'ArrowDown') {
      event.preventDefault()
      const direction = event.key === 'ArrowUp' ? -1 : 1
      const allCommands = getMatchingSlashCommands(
        slashCommandsQuery.value,
        localWorkflowToggles.value,
        globalWorkflowToggles.value,
      )

      if (allCommands.length === 0) {
        return
      }

      const totalCommandCount = allCommands.length
      selectedSlashCommandsIndex.value =
        (selectedSlashCommandsIndex.value + direction + totalCommandCount) % totalCommandCount
      return
    }

    if ((event.key === 'Enter' || event.key === 'Tab') && selectedSlashCommandsIndex.value !== -1) {
      event.preventDefault()
      const commands = getMatchingSlashCommands(
        slashCommandsQuery.value,
        localWorkflowToggles.value,
        globalWorkflowToggles.value,
      )
      if (commands.length > 0) {
        handleSlashCommandsSelect(commands[selectedSlashCommandsIndex.value])
      }
      return
    }
  }

  if (showContextMenu.value) {
    if (event.key === 'Escape') {
      selectedType.value = null
      selectedMenuIndex.value = DEFAULT_CONTEXT_MENU_OPTION
      searchQuery.value = ''
      return
    }

    if (event.key === 'ArrowUp' || event.key === 'ArrowDown') {
      event.preventDefault()
      const direction = event.key === 'ArrowUp' ? -1 : 1
      const options = getContextMenuOptions(
        searchQuery.value,
        selectedType.value,
        queryItems.value,
        fileSearchResults.value,
      )

      if (options.length === 0) {
        return
      }

      const selectableOptions = options.filter(
        (option: any) =>
          option.type !== ContextMenuOptionType.URL &&
          option.type !== ContextMenuOptionType.NoResults,
      )

      if (selectableOptions.length === 0) {
        selectedMenuIndex.value = -1
        return
      }

      const currentSelectableIndex = selectableOptions.findIndex(
        (option: any) => option === options[selectedMenuIndex.value],
      )

      const newSelectableIndex =
        (currentSelectableIndex + direction + selectableOptions.length) % selectableOptions.length

      selectedMenuIndex.value = options.findIndex(
        (option: any) => option === selectableOptions[newSelectableIndex],
      )
      return
    }

    if ((event.key === 'Enter' || event.key === 'Tab') && selectedMenuIndex.value !== -1) {
      event.preventDefault()
      const selectedOption = getContextMenuOptions(
        searchQuery.value,
        selectedType.value,
        queryItems.value,
        fileSearchResults.value,
      )[selectedMenuIndex.value]

      if (
        selectedOption &&
        selectedOption.type !== ContextMenuOptionType.URL &&
        selectedOption.type !== ContextMenuOptionType.NoResults
      ) {
        const mentionValue = selectedOption.label?.includes(':')
          ? selectedOption.label
          : selectedOption.value
        handleMentionSelect(selectedOption.type, mentionValue)
      }
      return
    }
  }

  // Safari does not support InputEvent.isComposing (always false), so we need to fallback to keyCode === 229 for it
  const isComposing = isSafari
    ? (event as KeyboardEvent & { keyCode?: number }).keyCode === 229
    : (event as KeyboardEvent & { isComposing?: boolean }).isComposing ?? false

  if (event.key === 'Enter' && !event.shiftKey && !isComposing) {
    event.preventDefault()

    if (!chatStateStore.sendingDisabled) {
      isTextAreaFocused.value = false
      props.onSendMessage()
    }
  }

  if (event.key === 'Backspace' && !isComposing) {
    const charBeforeCursor = chatStateStore.inputValue[cursorPosition.value - 1]
    const charAfterCursor = chatStateStore.inputValue[cursorPosition.value + 1]

    const charBeforeIsWhitespace =
      charBeforeCursor === ' ' || charBeforeCursor === '\n' || charBeforeCursor === '\r\n'
    const charAfterIsWhitespace =
      charAfterCursor === ' ' || charAfterCursor === '\n' || charAfterCursor === '\r\n'

    // Check if we're right after a space that follows a mention or slash command
    if (
      charBeforeIsWhitespace &&
      inputValue.value.slice(0, cursorPosition.value - 1).match(new RegExp(mentionRegex.source + '$'))
    ) {
      const newCursorPosition = cursorPosition.value - 1
      if (!charAfterIsWhitespace) {
        event.preventDefault()
        textAreaRef.value?.setSelectionRange(newCursorPosition, newCursorPosition)
        cursorPosition.value = newCursorPosition
      }
      cursorPosition.value = newCursorPosition
      justDeletedSpaceAfterMention.value = true
      justDeletedSpaceAfterSlashCommand.value = false
    } else if (
      charBeforeIsWhitespace &&
      inputValue.value.slice(0, cursorPosition.value - 1).match(slashCommandDeleteRegex)
    ) {
      const newCursorPosition = cursorPosition.value - 1
      if (!charAfterIsWhitespace) {
        event.preventDefault()
        textAreaRef.value?.setSelectionRange(newCursorPosition, newCursorPosition)
        cursorPosition.value = newCursorPosition
      }
      cursorPosition.value = newCursorPosition
      justDeletedSpaceAfterSlashCommand.value = true
      justDeletedSpaceAfterMention.value = false
    } else if (justDeletedSpaceAfterMention.value) {
      const { newText, newPosition } = removeMention(chatStateStore.inputValue, cursorPosition.value)
      if (newText !== chatStateStore.inputValue) {
        event.preventDefault()
        setInputValue(newText)
        intendedCursorPosition.value = newPosition
      }
      justDeletedSpaceAfterMention.value = false
      showContextMenu.value = false
    } else if (justDeletedSpaceAfterSlashCommand.value) {
      const { newText, newPosition } = removeSlashCommand(chatStateStore.inputValue, cursorPosition.value)
      if (newText !== chatStateStore.inputValue) {
        event.preventDefault()
        setInputValue(newText)
        intendedCursorPosition.value = newPosition
      }
      justDeletedSpaceAfterSlashCommand.value = false
      showSlashCommandsMenu.value = false
    } else {
      justDeletedSpaceAfterMention.value = false
      justDeletedSpaceAfterSlashCommand.value = false
    }
  }
}

const handleInputChange = (e: Event) => {
  const target = e.target as HTMLTextAreaElement
  const newValue = target.value
  const newCursorPosition = target.selectionStart || 0
  setInputValue(newValue)
  cursorPosition.value = newCursorPosition

  let showMenu = shouldShowContextMenu(newValue, newCursorPosition)
  const showSlashCommandsMenuValue = shouldShowSlashCommandsMenu(newValue, newCursorPosition)

  if (showSlashCommandsMenuValue) {
    showMenu = false
  }

  showSlashCommandsMenu.value = showSlashCommandsMenuValue
  showContextMenu.value = showMenu

  if (showSlashCommandsMenuValue) {
    const slashIndex = newValue.indexOf('/')
    const query = newValue.slice(slashIndex + 1, newCursorPosition)
    slashCommandsQuery.value = query
    selectedSlashCommandsIndex.value = 0
  } else {
    slashCommandsQuery.value = ''
    selectedSlashCommandsIndex.value = 0
  }

  if (showMenu) {
    const lastAtIndex = newValue.lastIndexOf('@', newCursorPosition - 1)
    const query = newValue.slice(lastAtIndex + 1, newCursorPosition)
    searchQuery.value = query
    currentSearchQueryRef.value = query

    if (query.length > 0) {
      selectedMenuIndex.value = 0

      if (searchTimeoutRef.value) {
        clearTimeout(searchTimeoutRef.value)
      }

      searchLoading.value = true

      const searchType =
        selectedType.value === ContextMenuOptionType.File
          ? FileSearchType.FILE
          : selectedType.value === ContextMenuOptionType.Folder
            ? FileSearchType.FOLDER
            : undefined

      let workspaceHint: string | undefined
      let searchQueryValue = query
      const workspaceHintMatch = query.match(/^([\w-]+):\/(.*)$/)
      if (workspaceHintMatch) {
        workspaceHint = workspaceHintMatch[1]
        searchQueryValue = workspaceHintMatch[2]
      }

      searchTimeoutRef.value = setTimeout(() => {
        fileService.searchFiles(
          {
            query: searchQueryValue,
            mentionsRequestId: query,
            selectedType: searchType,
            workspaceHint: workspaceHint,
          },
        )
          .then((results: any) => {
            fileSearchResults.value = (results.results || []) as SearchResult[]
            searchLoading.value = false
          })
          .catch((error: any) => {
            console.error('Error searching files:', error)
            fileSearchResults.value = []
            searchLoading.value = false
          })
      }, 200)
    } else {
      selectedMenuIndex.value = DEFAULT_CONTEXT_MENU_OPTION
    }
  } else {
    searchQuery.value = ''
    selectedMenuIndex.value = -1
    fileSearchResults.value = []
  }
}

const handleBlur = () => {
  if (!isMouseDownOnMenu.value) {
    showContextMenu.value = false
    showSlashCommandsMenu.value = false
  }
  isTextAreaFocused.value = false
  handleFocusChange(false)
}

const handleFocus = () => {
  isTextAreaFocused.value = true
  handleFocusChange(true)
}

// Height change handler - Vue textarea doesn't have this event, we'll use ResizeObserver
onMounted(() => {
  if (textAreaRef.value) {
    const resizeObserver = new ResizeObserver(() => {
      if (textAreaRef.value) {
        const height = textAreaRef.value.offsetHeight
        if (textAreaBaseHeight.value === undefined || height < textAreaBaseHeight.value) {
          textAreaBaseHeight.value = height
        }
        props.onHeightChange?.(height)
      }
    })
    resizeObserver.observe(textAreaRef.value)
    onBeforeUnmount(() => {
      resizeObserver.disconnect()
    })
  }
})

const handleThumbnailsHeightChange = (height: number) => {
  thumbnailsHeight.value = height
}

const handleMenuMouseDown = () => {
  isMouseDownOnMenu.value = true
}

const updateHighlights = () => {
  if (!textAreaRef.value || !highlightLayerRef.value) {
    return
  }

  let processedText = textAreaRef.value.value

  processedText = processedText
    .replace(/\n$/, '\n\n')
    .replace(/[<>&]/g, (c) => ({ '<': '&lt;', '>': '&gt;', '&': '&amp;' })[c] || c)
    .replace(mentionRegexGlobal, '<mark class="mention-context-textarea-highlight">$&</mark>')

  if (/^\s*\//.test(processedText)) {
    const slashIndex = processedText.indexOf('/')
    const spaceIndex = processedText.indexOf(' ', slashIndex)
    const endIndex = spaceIndex > -1 ? spaceIndex : processedText.length
    const commandText = processedText.substring(slashIndex + 1, endIndex)
    const isValidCommand = validateSlashCommand(
      commandText,
      localWorkflowToggles.value,
      globalWorkflowToggles.value,
    )

    if (isValidCommand) {
      const fullCommand = processedText.substring(slashIndex, endIndex)
      const highlighted = `<mark class="mention-context-textarea-highlight">${fullCommand}</mark>`
      processedText =
        processedText.substring(0, slashIndex) + highlighted + processedText.substring(endIndex)
    }
  }

  highlightLayerRef.value.innerHTML = processedText
  highlightLayerRef.value.scrollTop = textAreaRef.value.scrollTop
  highlightLayerRef.value.scrollLeft = textAreaRef.value.scrollLeft
}

const updateCursorPosition = () => {
  if (textAreaRef.value) {
    cursorPosition.value = textAreaRef.value.selectionStart
  }
}

const handleKeyUp = (e: KeyboardEvent) => {
  if (['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown', 'Home', 'End'].includes(e.key)) {
    updateCursorPosition()
  }
}

const submitApiConfig = async () => {
  const apiValidationResult = validateApiConfiguration(mode.value, apiConfiguration.value)
  const modelIdValidationResult = validateModelId(mode.value, apiConfiguration.value, openRouterModels.value)

  if (!apiValidationResult && !modelIdValidationResult && apiConfiguration.value) {
    try {
      await modelsService.updateApiConfigurationProto(
        {
          apiConfiguration: apiConfiguration.value,
        },
      )
    } catch (error) {
      console.error('Failed to update API configuration:', error)
    }
  } else {
    stateService.getLatestState()
      .then(() => {
        console.log('State refreshed')
      })
      .catch((error) => {
        console.error('Error refreshing state:', error)
      })
  }
}

const onModeToggle = () => {
  let changeModeDelay = 0
  if (showModelSelector.value) {
    submitApiConfig()
    changeModeDelay = 250
  }
  setTimeout(async () => {
    const convertedProtoMode = mode.value === 'plan' ? PlanActMode.ACT : PlanActMode.PLAN
    await stateService.updateSettings({
      mode: convertedProtoMode
    })
    setTimeout(() => {
      setInputValue('')
      textAreaRef.value?.focus()
    }, 100)
  }, changeModeDelay)
}

const handleContextButtonClick = () => {
  textAreaRef.value?.focus()

  if (!chatStateStore.inputValue.trim()) {
    const newValue = '@'
    setInputValue(newValue)
    cursorPosition.value = 1
    handleInputChange({ target: { value: newValue, selectionStart: 1 } } as any)
    updateHighlights()
    return
  }

  if (chatStateStore.inputValue.endsWith(' ')) {
    const newValue = chatStateStore.inputValue + '@'
    setInputValue(newValue)
    cursorPosition.value = newValue.length
    handleInputChange({ target: { value: newValue, selectionStart: newValue.length } } as any)
    updateHighlights()
    return
  }

  const newValue = chatStateStore.inputValue + ' @'
  setInputValue(newValue)
  cursorPosition.value = newValue.length
  handleInputChange({ target: { value: newValue, selectionStart: newValue.length } } as any)
  updateHighlights()
}

const handleFilesButtonClick = () => {
  if (!props.shouldDisableFilesAndImages) {
    props.onSelectFilesAndImages()
  }
}

const handleModelButtonClick = () => {
  showModelSelector.value = !showModelSelector.value
}

const handleActionButtonClick = () => {
  if (hasInputContent.value) {
    isTextAreaFocused.value = false
    props.onSendMessage()
  }
}

const handleCancelClick = async () => {
  const taskId = extensionStateStore.conversationId
  if (taskId) {
    try {
      await taskService.cancelTask(taskId)
    } catch (error) {
      console.error('Failed to cancel task:', error)
    }
  }
}

const handleVoiceInputClick = () => {
  if (dictationSettings.value?.featureEnabled && dictationSettings.value?.dictationEnabled) {
    isVoiceRecording.value = true
  } else {
    console.warn('Voice input is not enabled')
  }
}

const handleProcessingStateChange = (isProcessing: boolean, message?: string) => {
  if (isProcessing && message) {
    setInputValue(`${chatStateStore.inputValue} [${message}]`.trim())
  }
}

const handleTranscription = (text: string) => {
  const processingPattern = /\s*\[Transcribing\.\.\.\]$/
  const cleanedValue = chatStateStore.inputValue.replace(processingPattern, '')

  if (!text) {
    setInputValue(cleanedValue)
    return
  }

  const newValue = cleanedValue + (cleanedValue ? ' ' : '') + text
  setInputValue(newValue)
  setTimeout(() => {
    if (textAreaRef.value) {
      textAreaRef.value.focus()
      const length = newValue.length
      textAreaRef.value.setSelectionRange(length, length)
    }
  }, 0)
}

const handlePaste = async (e: ClipboardEvent) => {
  const items = e.clipboardData?.items || []

  const pastedText = e.clipboardData?.getData('text') || ''
  const urlRegex = /^\S+:\/\/\S+$/
  if (urlRegex.test(pastedText.trim())) {
    e.preventDefault()
    const trimmedUrl = pastedText.trim()
    const newValue =
      inputValue.value.slice(0, cursorPosition.value) +
      trimmedUrl +
      ' ' +
      inputValue.value.slice(cursorPosition.value)
    setInputValue(newValue)
    const newCursorPosition = cursorPosition.value + trimmedUrl.length + 1
    cursorPosition.value = newCursorPosition
    intendedCursorPosition.value = newCursorPosition
    showContextMenu.value = false

    setTimeout(() => {
      if (textAreaRef.value) {
        textAreaRef.value.blur()
        textAreaRef.value.focus()
      }
    }, 0)
    return
  }

  const acceptedTypes = ['png', 'jpeg', 'webp']
  const imageItems = Array.from(items).filter((item) => {
    const [type, subtype] = item.type.split('/')
    return type === 'image' && acceptedTypes.includes(subtype)
  })

  if (!props.shouldDisableFilesAndImages && imageItems.length > 0) {
    e.preventDefault()
    const imagePromises = imageItems.map((item) => {
      return new Promise<string | null>((resolve) => {
        const blob = item.getAsFile()
        if (!blob) {
          resolve(null)
          return
        }
        const reader = new FileReader()
        reader.onloadend = async () => {
          if (reader.error) {
            console.error('Error reading file:', reader.error)
            resolve(null)
          } else {
            const result = reader.result
            if (typeof result === 'string') {
              try {
                await getImageDimensions(result)
                resolve(result)
              } catch (error) {
                console.warn((error as Error).message)
                showDimensionErrorMessage()
                resolve(null)
              }
            } else {
              resolve(null)
            }
          }
        }
        reader.readAsDataURL(blob)
      })
    })
    const imageDataArray = await Promise.all(imagePromises)
    const dataUrls = imageDataArray.filter((dataUrl): dataUrl is string => dataUrl !== null)

    if (dataUrls.length > 0) {
      const filesAndImagesLength = chatStateStore.selectedImages.length + chatStateStore.selectedFiles.length
      const availableSlots = MAX_IMAGES_AND_FILES_PER_MESSAGE - filesAndImagesLength

      if (availableSlots > 0) {
        const imagesToAdd = Math.min(dataUrls.length, availableSlots)
        setSelectedImages([...chatStateStore.selectedImages, ...dataUrls.slice(0, imagesToAdd)])
      }
    } else {
      console.warn('No valid images were processed')
    }
  }
}

const showUnsupportedFileErrorMessage = () => {
  showUnsupportedFileError.value = true
  if (unsupportedFileTimerRef.value) {
    clearTimeout(unsupportedFileTimerRef.value)
  }
  unsupportedFileTimerRef.value = setTimeout(() => {
    showUnsupportedFileError.value = false
    unsupportedFileTimerRef.value = null
  }, 3000)
}

const handleDragEnter = (e: DragEvent) => {
  e.preventDefault()
  isDraggingOver.value = true

  if (e.dataTransfer?.types.includes('Files')) {
    const items = Array.from(e.dataTransfer.items)
    const hasNonImageFile = items.some((item) => {
      if (item.kind === 'file') {
        const type = item.type.split('/')[0]
        return type !== 'image'
      }
      return false
    })

    if (hasNonImageFile) {
      showUnsupportedFileErrorMessage()
    }
  }
}

const onDragOver = (e: DragEvent) => {
  e.preventDefault()
  if (!isDraggingOver.value) {
    isDraggingOver.value = true
  }
}

const handleDragLeave = (e: DragEvent) => {
  e.preventDefault()
  const dropZone = e.currentTarget as HTMLElement
  if (!dropZone.contains(e.relatedTarget as Node)) {
    isDraggingOver.value = false
  }
}

const readImageFiles = (imageFiles: File[]): Promise<(string | null)[]> => {
  return Promise.all(
    imageFiles.map(
      (file) =>
        new Promise<string | null>((resolve) => {
          const reader = new FileReader()
          reader.onloadend = async () => {
            if (reader.error) {
              console.error('Error reading file:', reader.error)
              resolve(null)
            } else {
              const result = reader.result
              if (typeof result === 'string') {
                try {
                  await getImageDimensions(result)
                  resolve(result)
                } catch (error) {
                  console.warn((error as Error).message)
                  showDimensionErrorMessage()
                  resolve(null)
                }
              } else {
                resolve(null)
              }
            }
          }
          reader.readAsDataURL(file)
        }),
    ),
  )
}

const handleTextDrop = (text: string) => {
  const newValue =
    inputValue.value.slice(0, cursorPosition.value) +
    text +
    inputValue.value.slice(cursorPosition.value)
  setInputValue(newValue)
  const newCursorPosition = cursorPosition.value + text.length
  cursorPosition.value = newCursorPosition
  intendedCursorPosition.value = newCursorPosition
}

const onDrop = async (e: DragEvent) => {
  e.preventDefault()
  isDraggingOver.value = false

  showUnsupportedFileError.value = false
  if (unsupportedFileTimerRef.value) {
    clearTimeout(unsupportedFileTimerRef.value)
    unsupportedFileTimerRef.value = null
  }

  let uris: string[] = []
  const resourceUrlsData = e.dataTransfer?.getData('resourceurls')
  const vscodeUriListData = e.dataTransfer?.getData('application/vnd.code.uri-list')

  if (resourceUrlsData) {
    try {
      uris = JSON.parse(resourceUrlsData)
      uris = uris.map((uri) => decodeURIComponent(uri))
    } catch (error) {
      console.error('Failed to parse resourceurls JSON:', error)
      uris = []
    }
  }

  if (uris.length === 0 && vscodeUriListData) {
    uris = vscodeUriListData.split('\n').map((uri) => uri.trim())
  }

  const validUris = uris.filter(
    (uri) => uri && (uri.startsWith('vscode-file:') || uri.startsWith('file:')),
  )

  if (validUris.length > 0) {
    pendingInsertions.value = []
    let initialCursorPos = inputValue.value.length
    if (textAreaRef.value) {
      initialCursorPos = textAreaRef.value.selectionStart
    }
    intendedCursorPosition.value = initialCursorPos

    fileService.getRelativePaths({ uris: validUris })
      .then((response: any) => {
        if (response.paths.length > 0) {
          pendingInsertions.value = [...pendingInsertions.value, ...response.paths]
        }
      })
      .catch((error: any) => {
        console.error('Error getting relative paths:', error)
      })
    return
  }

  const text = e.dataTransfer?.getData('text')
  if (text) {
    handleTextDrop(text)
    return
  }

  const files = Array.from(e.dataTransfer?.files || [])
  const acceptedTypes = ['png', 'jpeg', 'webp']
  const imageFiles = files.filter((file) => {
    const [type, subtype] = file.type.split('/')
    return type === 'image' && acceptedTypes.includes(subtype)
  })

  if (props.shouldDisableFilesAndImages || imageFiles.length === 0) {
    return
  }

  const imageDataArray = await readImageFiles(imageFiles)
  const dataUrls = imageDataArray.filter((dataUrl): dataUrl is string => dataUrl !== null)

  if (dataUrls.length > 0) {
      const filesAndImagesLength = selectedImages.value.length + selectedFiles.value.length
    const availableSlots = MAX_IMAGES_AND_FILES_PER_MESSAGE - filesAndImagesLength

    if (availableSlots > 0) {
      const imagesToAdd = Math.min(dataUrls.length, availableSlots)
        setSelectedImages([...selectedImages.value, ...dataUrls.slice(0, imagesToAdd)])
    }
  } else {
    console.warn('No valid images were processed')
  }
}

const setShownTooltipMode = (mode: Mode | null) => {
  shownTooltipMode.value = mode
}

// Watchers and Effects
watch(
  () => selectedType.value,
  (newType) => {
    if (newType === ContextMenuOptionType.Git || /^[a-f0-9]+$/i.test(searchQuery.value)) {
      fileService.searchCommits(searchQuery.value || '')
        .then((response: any) => {
          if (response.commits) {
            const commits: GitCommit[] = response.commits.map(
              (commit: {
                hash: string
                shortHash: string
                subject: string
                author: string
                date: string
              }) => ({
                type: ContextMenuOptionType.Git,
                value: commit.hash,
                label: commit.subject,
                description: `${commit.shortHash} by ${commit.author} on ${commit.date}`,
              }),
            )
            gitCommits.value = commits
          }
        })
        .catch((error: any) => {
          console.error('Error searching commits:', error)
        })
    }
  },
)

watch(
  () => showContextMenu.value,
  (newValue) => {
    if (!newValue) {
      selectedType.value = null
    }
  },
)

watch(
  () => showModelSelector.value,
  (newValue, oldValue) => {
    if (oldValue && !newValue) {
      submitApiConfig()
    }
    prevShowModelSelector.value = newValue
  },
)

watch(
  () => [selectedImages.value, selectedFiles.value],
  () => {
    if (selectedImages.value.length === 0 && selectedFiles.value.length === 0) {
      thumbnailsHeight.value = 0
    }
  },
)

watch(
  () => chatStateStore.inputValue,
  () => {
    updateHighlights()
  },
)

watch(
  () => intendedCursorPosition.value,
  () => {
    if (intendedCursorPosition.value !== null && textAreaRef.value) {
      nextTick(() => {
        if (textAreaRef.value) {
          textAreaRef.value.setSelectionRange(
            intendedCursorPosition.value!,
            intendedCursorPosition.value!,
          )
          intendedCursorPosition.value = null
        }
      })
    }
  },
)

watch(
  () => pendingInsertions.value,
  () => {
    if (pendingInsertions.value.length === 0 || !textAreaRef.value) {
      return
    }

    const path = pendingInsertions.value[0]
    const currentTextArea = textAreaRef.value
    const currentValue = currentTextArea.value
    const currentCursorPos =
      intendedCursorPosition.value ??
      (currentTextArea.selectionStart >= 0 ? currentTextArea.selectionStart : currentValue.length)

    const { newValue, mentionIndex } = insertMentionDirectly(currentValue, currentCursorPos, path)

    setInputValue(newValue)

    const newCursorPosition = mentionIndex + path.length + 2
    intendedCursorPosition.value = newCursorPosition

    pendingInsertions.value = pendingInsertions.value.slice(1)
  },
)

watch(
  () => showModelSelector.value,
  () => {
    if (showModelSelector.value && buttonRef.value) {
      const buttonRect = buttonRef.value.getBoundingClientRect()
      const buttonCenter = buttonRect.left + buttonRect.width / 2
      const rightPosition = document.documentElement.clientWidth - buttonCenter - 5
      arrowPosition.value = rightPosition
      menuPosition.value = buttonRect.top + 1
    }
  },
)

// Click away handler - implemented manually
onMounted(() => {
  const handleClickOutside = (event: MouseEvent) => {
    if (
      modelSelectorRef.value &&
      !modelSelectorRef.value.contains(event.target as Node)
    ) {
      showModelSelector.value = false
    }
  }

  watch(
    () => showModelSelector.value,
    (isVisible) => {
      if (isVisible) {
        document.addEventListener('mousedown', handleClickOutside)
      } else {
        document.removeEventListener('mousedown', handleClickOutside)
      }
    },
  )

  onBeforeUnmount(() => {
    document.removeEventListener('mousedown', handleClickOutside)
  })
})

// Global drag end handler
onMounted(() => {
  const handleGlobalDragEnd = () => {
    isDraggingOver.value = false
  }

  document.addEventListener('dragend', handleGlobalDragEnd)

  onBeforeUnmount(() => {
    document.removeEventListener('dragend', handleGlobalDragEnd)
  })
})

// Click outside handlers
onMounted(() => {
  const handleClickOutside = (event: MouseEvent) => {
    if (
      contextMenuContainerRef.value &&
      !contextMenuContainerRef.value.contains(event.target as Node)
    ) {
      showContextMenu.value = false
    }
  }

  const handleClickOutsideSlashMenu = (event: MouseEvent) => {
    if (
      slashCommandsMenuContainerRef.value &&
      !slashCommandsMenuContainerRef.value.contains(event.target as Node)
    ) {
      showSlashCommandsMenu.value = false
    }
  }

  watch(
    () => showContextMenu.value,
    (isVisible) => {
      if (isVisible) {
        document.addEventListener('mousedown', handleClickOutside)
      } else {
        document.removeEventListener('mousedown', handleClickOutside)
      }
    },
  )

  watch(
    () => showSlashCommandsMenu.value,
    (isVisible) => {
      if (isVisible) {
        document.addEventListener('mousedown', handleClickOutsideSlashMenu)
      } else {
        document.removeEventListener('mousedown', handleClickOutsideSlashMenu)
      }
    },
  )

  onBeforeUnmount(() => {
    document.removeEventListener('mousedown', handleClickOutside)
    document.removeEventListener('mousedown', handleClickOutsideSlashMenu)
    if (searchTimeoutRef.value) {
      clearTimeout(searchTimeoutRef.value)
    }
    if (unsupportedFileTimerRef.value) {
      clearTimeout(unsupportedFileTimerRef.value)
    }
    if (dimensionErrorTimerRef.value) {
      clearTimeout(dimensionErrorTimerRef.value)
    }
  })
})
</script>

<style scoped>
.model-display-button {
  padding: 0;
  height: 20px;
  width: 100%;
  min-width: 0;
  cursor: pointer;
  text-decoration: none;
  color: var(--vscode-descriptionForeground);
  display: flex;
  align-items: center;
  font-size: 10px;
  outline: none;
  user-select: none;
  opacity: 1;
  pointer-events: auto;
}

.model-display-button:hover,
.model-display-button:focus {
  color: var(--vscode-foreground);
  text-decoration: underline;
  outline: none;
}

.model-display-button.is-active {
  text-decoration: underline;
  color: var(--vscode-foreground);
}

.model-display-button.disabled {
  opacity: 0.5;
  cursor: not-allowed;
  pointer-events: none;
}

.model-display-button.disabled:hover,
.model-display-button.disabled:focus {
  color: var(--vscode-descriptionForeground);
  text-decoration: none;
}
</style>
