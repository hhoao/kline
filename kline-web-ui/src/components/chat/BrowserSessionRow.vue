<template>
  <div
    ref="containerRef"
    class="p-[10px_6px_10px_15px] relative hover:[&_.checkpoint-controls]:opacity-100"
    :style="{ marginBottom: -10 }"
  >
    <div class="flex gap-2.5 items-center mb-2.5">
      <ProgressIndicator v-if="isBrowsing && !isLastMessageResume" />
      <span v-else class="i-codicon:inspect text-[var(--vscode-foreground)] -mb-[1.5px]"></span>
      <span class="font-bold">
        {{ isAutoApproved ? 'Cline is using the browser:' : 'Cline wants to use the browser:' }}
      </span>
    </div>
    <div
      class="rounded-[3px] border border-[var(--vscode-editorGroup-border)] bg-[var(--vscode-editor-background)] max-w-full mx-auto mb-2.5"
      :style="{ maxWidth: maxWidth }"
    >
      <!-- URL Bar -->
      <div class="m-[5px_auto] w-[calc(100%-10px)] flex items-center gap-1">
        <div
          class="flex-1 bg-[var(--vscode-input-background)] border border-[var(--vscode-input-border)] rounded px-1.5 py-0.5 min-w-0 text-xs"
          :class="displayState.url ? 'text-[var(--vscode-input-foreground)]' : 'text-[var(--vscode-descriptionForeground)]'"
        >
          <div class="overflow-hidden w-full text-center whitespace-nowrap text-ellipsis">
            {{ displayState.url || 'http' }}
          </div>
        </div>
        <BrowserSettingsMenu />
      </div>

      <!-- Screenshot Area -->
      <div
        class="w-full relative bg-[var(--vscode-input-background)]"
        :style="{
          paddingBottom: `${(browserSettings.viewport.height / browserSettings.viewport.width) * 100}%`,
        }"
      >
        <img
          v-if="displayState.screenshot"
          alt="Browser screenshot"
          class="object-contain absolute top-0 left-0 w-full h-full cursor-pointer"
          :src="displayState.screenshot"
          @click="handleScreenshotClick"
        />
        <div v-else class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2">
          <span class="i-codicon:globe text-[80px] text-[var(--vscode-descriptionForeground)]"></span>
        </div>
        <BrowserCursor
          v-if="displayState.mousePosition"
          :style="cursorStyle as Record<string, string | number>"
        />
      </div>

      <!-- Console Logs -->
      <div class="w-full">
        <div
          class="flex gap-1 justify-start items-center px-2 cursor-pointer"
          :class="consoleLogsExpanded ? 'pt-2 pb-0' : 'py-2'"
          @click="consoleLogsExpanded = !consoleLogsExpanded"
        >
          <span :class="`i-codicon:chevron-${consoleLogsExpanded ? 'down' : 'right'}`"></span>
          <span class="text-[0.8em]">Console Logs</span>
        </div>
        <CodeBlock
          v-if="consoleLogsExpanded"
          :source="`\`\`\`shell\n${displayState.consoleLogs || '(No new logs)'}\n\`\`\``"
        />
      </div>
    </div>

    <!-- Action content with min height -->
    <div ref="actionContentRef" :style="{ minHeight: `${maxActionHeight}px` }">
      <BrowserSessionRowContent
        v-for="message in currentPage?.nextAction?.messages"
        :key="message.ts"
        :expanded-rows="expandedRows"
        :is-last="isLast"
        :last-modified-message="lastModifiedMessage"
        :message="message"
        :on-set-quote="onSetQuote"
        :on-toggle-expand="onToggleExpand"
        @set-max-action-height="handleSetMaxActionHeight"
      />
      <BrowserActionBox
        v-if="!isBrowsing && messages.some((m) => m.say === 'browser_action_result') && currentPageIndex === 0"
        action="launch"
        :text="initialUrl"
      />
    </div>

    <!-- Pagination -->
    <div
      v-if="pages.length > 1"
      class="flex justify-between items-center py-2 mt-4 border-t border-[var(--vscode-editorGroup-border)]"
    >
      <div>
        Step {{ currentPageIndex + 1 }} of {{ pages.length }}
      </div>
      <div class="flex gap-1">
        <button
          class="px-3 py-1 text-xs bg-[var(--vscode-button-background)] text-[var(--vscode-button-foreground)] border border-[var(--vscode-button-border)] rounded cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
          :disabled="currentPageIndex === 0 || isBrowsing"
          @click="currentPageIndex = Math.max(0, currentPageIndex - 1)"
        >
          Previous
        </button>
        <button
          class="px-3 py-1 text-xs bg-[var(--vscode-button-background)] text-[var(--vscode-button-foreground)] border border-[var(--vscode-button-border)] rounded cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
          :disabled="currentPageIndex === pages.length - 1 || isBrowsing"
          @click="currentPageIndex = Math.min(pages.length - 1, currentPageIndex + 1)"
        >
          Next
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { fileService } from '@/api/file'
import BrowserSettingsMenu from '@/components/browser/BrowserSettingsMenu.vue'
import CodeBlock from '@/components/common/CodeBlock.vue'
import { BROWSER_VIEWPORT_PRESETS } from '@/shared/BrowserSettings'
import type { BrowserActionResult, ClineMessage, ClineSayBrowserAction } from '@/shared/ExtensionMessage'
import { useExtensionStateStore } from "@/stores/extensionState"
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import BrowserActionBox from './BrowserActionBox.vue'
import BrowserCursor from './BrowserCursor.vue'
import BrowserSessionRowContent from './BrowserSessionRowContent.vue'
import ProgressIndicator from './ProgressIndicator.vue'

interface Props {
  messages: ClineMessage[]
  expandedRows: Record<number, boolean>
  onToggleExpand: (messageTs: number) => void
  lastModifiedMessage?: ClineMessage
  isLast: boolean
  onHeightChange: (isTaller: boolean) => void
  onSetQuote: (text: string) => void
}

const props = defineProps<Props>()

const browserSettings = computed(() => useExtensionStateStore().extensionState?.browserSettings || {
  viewport: { width: 900, height: 600 },
})

const containerRef = ref<HTMLDivElement | null>(null)
const actionContentRef = ref<HTMLDivElement | null>(null)
const prevHeightRef = ref(0)
const maxActionHeight = ref(0)
const consoleLogsExpanded = ref(false)
const currentPageIndex = ref(0)

const isLastApiReqInterrupted = computed(() => {
  const lastApiReqStarted = [...props.messages].reverse().find((m) => m.say === 'api_req_started')
  if (lastApiReqStarted?.text != null) {
    const info = JSON.parse(lastApiReqStarted.text)
    if (info.cancelReason != null) {
      return true
    }
  }
  const lastApiReqFailed = props.isLast && props.lastModifiedMessage?.ask === 'api_req_failed'
  return lastApiReqFailed
})

const isLastMessageResume = computed(() => {
  return props.lastModifiedMessage?.ask === 'resume_task' || props.lastModifiedMessage?.ask === 'resume_completed_task'
})

const isBrowsing = computed(() => {
  return props.isLast && props.messages.some((m) => m.say === 'browser_action_result') && !isLastApiReqInterrupted.value
})

const pages = computed(() => {
  const result: {
    currentState: {
      url?: string
      screenshot?: string
      mousePosition?: string
      consoleLogs?: string
      messages: ClineMessage[]
    }
    nextAction?: {
      messages: ClineMessage[]
    }
  }[] = []

  let currentStateMessages: ClineMessage[] = []
  let nextActionMessages: ClineMessage[] = []

  props.messages.forEach((message) => {
    if (message.ask === 'browser_action_launch' || message.say === 'browser_action_launch') {
      currentStateMessages = [message]
    } else if (message.say === 'browser_action_result') {
      if (message.text === '') {
        return
      }
      currentStateMessages.push(message)
      const resultData = JSON.parse(message.text || '{}') as BrowserActionResult

      result.push({
        currentState: {
          url: resultData.currentUrl,
          screenshot: resultData.screenshot,
          mousePosition: resultData.currentMousePosition,
          consoleLogs: resultData.logs,
          messages: [...currentStateMessages],
        },
        nextAction:
          nextActionMessages.length > 0
            ? {
                messages: [...nextActionMessages],
              }
            : undefined,
      })

      currentStateMessages = []
      nextActionMessages = []
    } else if (
      message.say === 'api_req_started' ||
      message.say === 'text' ||
      message.say === 'reasoning' ||
      message.say === 'browser_action' ||
      message.say === 'error_retry'
    ) {
      nextActionMessages.push(message)
    } else {
      currentStateMessages.push(message)
    }
  })

  if (currentStateMessages.length > 0 || nextActionMessages.length > 0) {
    result.push({
      currentState: {
        messages: [...currentStateMessages],
      },
      nextAction:
        nextActionMessages.length > 0
          ? {
              messages: [...nextActionMessages],
            }
          : undefined,
    })
  }

  return result
})

watch(() => pages.value.length, () => {
  currentPageIndex.value = pages.value.length - 1
}, { immediate: true })

const initialUrl = computed(() => {
  const launchMessage = props.messages.find(
    (m) => m.ask === 'browser_action_launch' || m.say === 'browser_action_launch'
  )
  return launchMessage?.text || ''
})

const isAutoApproved = computed(() => {
  const launchMessage = props.messages.find(
    (m) => m.ask === 'browser_action_launch' || m.say === 'browser_action_launch'
  )
  return launchMessage?.say === 'browser_action_launch'
})

const latestState = computed(() => {
  for (let i = pages.value.length - 1; i >= 0; i--) {
    const page = pages.value[i]
    if (page.currentState.url || page.currentState.screenshot) {
      return {
        url: page.currentState.url,
        mousePosition: page.currentState.mousePosition,
        consoleLogs: page.currentState.consoleLogs,
        screenshot: page.currentState.screenshot,
      }
    }
  }
  return {
    url: undefined,
    mousePosition: undefined,
    consoleLogs: undefined,
    screenshot: undefined,
  }
})

const currentPage = computed(() => pages.value[currentPageIndex.value])
const isLastPage = computed(() => currentPageIndex.value === pages.value.length - 1)

const defaultMousePosition = computed(
  () => `${browserSettings.value.viewport.width * 0.7},${browserSettings.value.viewport.height * 0.5}`
)

const displayState = computed(() => {
  if (isLastPage.value) {
    return {
      url: currentPage.value?.currentState.url || latestState.value.url || initialUrl.value,
      mousePosition:
        currentPage.value?.currentState.mousePosition ||
        latestState.value.mousePosition ||
        defaultMousePosition.value,
      consoleLogs: currentPage.value?.currentState.consoleLogs,
      screenshot: currentPage.value?.currentState.screenshot || latestState.value.screenshot,
    }
  }
  return {
    url: currentPage.value?.currentState.url || initialUrl.value,
    mousePosition: currentPage.value?.currentState.mousePosition || defaultMousePosition.value,
    consoleLogs: currentPage.value?.currentState.consoleLogs,
    screenshot: currentPage.value?.currentState.screenshot,
  }
})

const latestClickPosition = computed(() => {
  if (!isBrowsing.value) {
    return undefined
  }

  const actions = currentPage.value?.nextAction?.messages || []
  for (let i = actions.length - 1; i >= 0; i--) {
    const message = actions[i]
    if (message.say === 'browser_action') {
      const browserAction = JSON.parse(message.text || '{}') as ClineSayBrowserAction
      if (browserAction.action === 'click' && browserAction.coordinate) {
        return browserAction.coordinate
      }
    }
  }
  return undefined
})

const mousePosition = computed(() => {
  return isBrowsing.value ? latestClickPosition.value || displayState.value.mousePosition : displayState.value.mousePosition
})

const cursorStyle = computed(() => {
  if (!displayState.value.mousePosition) return {}
  const [x, y] = mousePosition.value.split(',').map(Number)
  return {
    position: 'absolute',
    top: `${(y / browserSettings.value.viewport.height) * 100}%`,
    left: `${(x / browserSettings.value.viewport.width) * 100}%`,
    transition: 'top 0.3s ease-out, left 0.3s ease-out',
  }
})

const maxWidth = computed(() => {
  return browserSettings.value.viewport.width < BROWSER_VIEWPORT_PRESETS['Small Desktop (900x600)'].width ? 200 : undefined
})

const handleScreenshotClick = () => {
  if (displayState.value.screenshot) {
    fileService.openImage(displayState.value.screenshot).catch(
      (err: any) => console.error('Failed to open image:', err)
    )
  }
}

const handleSetMaxActionHeight = (height: number) => {
  if (height > maxActionHeight.value) {
    maxActionHeight.value = height
  }
}

let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  if (containerRef.value) {
    resizeObserver = new ResizeObserver(() => {
      if (containerRef.value && props.isLast) {
        const height = containerRef.value.offsetHeight
        const isInitialRender = prevHeightRef.value === 0
        if (height !== 0 && height !== Infinity && height !== prevHeightRef.value) {
          if (!isInitialRender) {
            props.onHeightChange(height > prevHeightRef.value)
          }
          prevHeightRef.value = height
        }
      }
    })
    resizeObserver.observe(containerRef.value)
  }

  if (actionContentRef.value) {
    const actionObserver = new ResizeObserver(() => {
      if (actionContentRef.value) {
        const height = actionContentRef.value.offsetHeight
        if (height !== 0 && height !== Infinity && height > maxActionHeight.value) {
          maxActionHeight.value = height
        }
      }
    })
    actionObserver.observe(actionContentRef.value)
  }
})

onBeforeUnmount(() => {
  if (resizeObserver) {
    resizeObserver.disconnect()
  }
})
</script>


