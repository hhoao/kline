<template>
  <div
    ref="tooltipRef"
    class="inline-block relative"
    @mouseenter="showTooltip = true"
    @mouseleave="showTooltip = false"
  >
    <slot />
    <Teleport to="body">
      <div
        v-if="showTooltip"
        class="fixed z-[1000] bg-[var(--vscode-editor-background)] text-[var(--vscode-editor-foreground)] border border-[var(--vscode-widget-border)] py-1 rounded-[3px] max-w-[calc(100dvw-2rem)] text-xs shadow-sm pointer-events-none"
        :style="tooltipStyle"
      >
        <div class="flex flex-col px-2">
          <div class="flex flex-wrap items-center mb-1 font-bold">
            <div class="flex items-center mr-4 mb-0.5">
              <div
                class="w-[10px] h-[10px] min-w-[10px] min-h-[10px] rounded-sm mr-2 flex-shrink-0 inline-block"
                :style="{ backgroundColor: getColor(message) }"
              />
              {{ getMessageDescription(message) }}
            </div>
            <span
              v-if="getTimestamp(message)"
              class="font-normal"
              style="font-weight: normal; font-size: 10px"
            >
              {{ getTimestamp(message) }}
            </span>
          </div>
          <div
            v-if="getMessageContent(message)"
            class="whitespace-pre-wrap break-words max-h-[150px] overflow-y-auto text-[11px] bg-[var(--vscode-textBlockQuote-background)] p-1 rounded"
            style="font-family: var(--vscode-editor-font-family); scrollbar-width: none"
          >
            {{ getMessageContent(message) }}
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import type { ClineMessage } from '@/shared/ExtensionMessage'
import { ref, Teleport, watch } from 'vue'
import { getColor } from './util'

interface Props {
  message: ClineMessage
}

const props = defineProps<Props>()

const showTooltip = ref(false)
const tooltipRef = ref<HTMLElement | null>(null)
const tooltipStyle = ref<{ top: string; left: string }>({ top: '0px', left: '0px' })

const updateTooltipPosition = () => {
  if (!tooltipRef.value || !showTooltip.value) return
  
  const rect = tooltipRef.value.getBoundingClientRect()
  tooltipStyle.value = {
    top: `${rect.bottom + 8}px`,
    left: `${rect.left}px`,
  }
}

watch(showTooltip, (newVal) => {
  if (newVal) {
    updateTooltipPosition()
  }
})

const getMessageDescription = (message: ClineMessage): string => {
  if (message.type === 'say') {
    switch (message.say) {
      case 'task':
        return 'Task Message'
      case 'user_feedback':
        return 'User Message'
      case 'text':
        return 'Assistant Response'
      case 'tool':
        if (message.text) {
          try {
            const toolData = JSON.parse(message.text)
            if (
              toolData.tool === 'readFile' ||
              toolData.tool === 'listFilesTopLevel' ||
              toolData.tool === 'listFilesRecursive' ||
              toolData.tool === 'listCodeDefinitionNames' ||
              toolData.tool === 'searchFiles'
            ) {
              return `File Read: ${toolData.tool}`
            } else if (toolData.tool === 'editedExistingFile') {
              return `File Edit: ${toolData.path || 'Unknown file'}`
            } else if (toolData.tool === 'newFileCreated') {
              return `New File: ${toolData.path || 'Unknown file'}`
            } else if (toolData.tool === 'webFetch') {
              return `Web Fetch: ${toolData.path || 'Unknown URL'}`
            }
            return `Tool: ${toolData.tool}`
          } catch (_e) {
            return 'Tool Use'
          }
        }
        return 'Tool Use'
      case 'command':
        return 'Terminal Command'
      case 'command_output':
        return 'Terminal Output'
      case 'browser_action':
        return 'Browser Action'
      case 'browser_action_result':
        return 'Browser Result'
      case 'completion_result':
        return 'Task Completed'
      case 'checkpoint_created':
        return 'Checkpoint Created'
      default:
        return message.say || 'Unknown'
    }
  } else if (message.type === 'ask') {
    switch (message.ask) {
      case 'followup':
        return 'Assistant Message'
      case 'plan_mode_respond':
        return 'Planning Response'
      case 'tool':
        if (message.text) {
          try {
            const toolData = JSON.parse(message.text)
            if (
              toolData.tool === 'readFile' ||
              toolData.tool === 'listFilesTopLevel' ||
              toolData.tool === 'listFilesRecursive' ||
              toolData.tool === 'listCodeDefinitionNames' ||
              toolData.tool === 'searchFiles'
            ) {
              return `File Read Approval: ${toolData.tool}`
            } else if (toolData.tool === 'editedExistingFile') {
              return `File Edit Approval: ${toolData.path || 'Unknown file'}`
            } else if (toolData.tool === 'newFileCreated') {
              return `New File Approval: ${toolData.path || 'Unknown file'}`
            } else if (toolData.tool === 'webFetch') {
              return `Web Fetch: ${toolData.path || 'Unknown URL'}`
            }
            return `Tool Approval: ${toolData.tool}`
          } catch (_e) {
            return 'Tool Approval'
          }
        }
        return 'Tool Approval'
      case 'command':
        return 'Terminal Command Approval'
      case 'browser_action_launch':
        return 'Browser Action Approval'
      default:
        return message.ask || 'Unknown'
    }
  }
  return 'Unknown Message Type'
}

const getMessageContent = (message: ClineMessage): string => {
  if (message.text) {
    if (message.type === 'ask' && message.ask === 'plan_mode_respond' && message.text) {
      try {
        const planData = JSON.parse(message.text)
        return planData.response || message.text
      } catch (_e) {
        return message.text
      }
    } else if (message.type === 'say' && message.say === 'tool' && message.text) {
      try {
        const toolData = JSON.parse(message.text)
        return JSON.stringify(toolData, null, 2)
      } catch (_e) {
        return message.text
      }
    }

    if (message.text.length > 200) {
      return message.text.substring(0, 200) + '...'
    }
    return message.text
  }
  return ''
}

const getTimestamp = (message: ClineMessage): string => {
  if (message.ts) {
    const messageDate = new Date(message.ts)
    const today = new Date()

    const todayDate = new Date(today.getFullYear(), today.getMonth(), today.getDate())
    const messageDateOnly = new Date(messageDate.getFullYear(), messageDate.getMonth(), messageDate.getDate())

    const time = messageDate.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit', hour12: true })

    const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
    const monthName = monthNames[messageDate.getMonth()]

    if (messageDateOnly.getTime() === todayDate.getTime()) {
      return `${time}`
    } else if (messageDate.getFullYear() === today.getFullYear()) {
      return `${monthName} ${messageDate.getDate()} ${time}`
    } else {
      return `${monthName} ${messageDate.getDate()}, ${messageDate.getFullYear()} ${time}`
    }
  }
  return ''
}
</script>

