<template>
  <div ref="containerRef" class="flex relative -mt-px">
    <button
      class="flex justify-center items-center p-0 mr-1 bg-transparent border-0 cursor-pointer browser-info-icon"
      :title="'Browser connection info'"
      @click="toggleInfoPopover"
    >
      <i
        :class="['codicon', getIconClass()]"
        :style="{
          fontSize: '14.5px',
          color: getIconColor(),
        }"
      />
    </button>

    <div
      v-if="showInfoPopover"
      ref="popoverRef"
      class="absolute top-[30px] right-0 z-[100] w-[60dvw] max-w-[250px] rounded p-2.5 shadow-lg"
      :style="{
        backgroundColor: 'var(--vscode-editorWidget-background)',
        border: '1px solid var(--vscode-widget-border)',
        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.15)',
      }"
    >
      <h4 class="m-0 mb-2">Browser Connection</h4>
      <!-- InfoRow - Status row container -->
      <div class="flex flex-wrap mb-1 whitespace-nowrap">
        <!-- InfoLabel - Fixed-width label -->
        <div class="flex-none w-[90px] font-medium">Status:</div>
        <!-- InfoValue - Flexible value container -->
        <div
          class="flex-1 break-words"
          :style="{
            color: connectionInfo.isConnected
              ? 'var(--vscode-charts-green)'
              : 'var(--vscode-errorForeground)',
          }"
        >
          {{ connectionInfo.isConnected ? 'Connected' : 'Disconnected' }}
        </div>
      </div>
      <div v-if="connectionInfo.isConnected" class="flex flex-wrap mb-1 whitespace-nowrap">
        <!-- InfoLabel - Fixed-width label -->
        <div class="flex-none w-[90px] font-medium">Type:</div>
        <!-- InfoValue - Flexible value container -->
        <div class="flex-1 break-words">{{ connectionInfo.isRemote ? 'Remote' : 'Local' }}</div>
      </div>
      <div
        v-if="connectionInfo.isConnected && connectionInfo.isRemote && connectionInfo.host"
        class="flex flex-wrap mb-1 whitespace-nowrap"
      >
        <!-- InfoLabel - Fixed-width label -->
        <div class="flex-none w-[90px] font-medium">Remote Host:</div>
        <!-- InfoValue - Flexible value container -->
        <div class="flex-1 break-words">{{ connectionInfo.host }}</div>
      </div>
    </div>

    <button
      class="flex justify-center items-center p-0 bg-transparent border-0 cursor-pointer"
      @click="openBrowserSettings"
    >
      <i class="i-codicon:settings-gear" style="font-size: 14.5px" />
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { browserService } from '@/api/browser'
import { uiService } from '@/api/ui'
import { useExtensionStateStore } from "@/stores/extensionState"
import type { BrowserConnectionInfo } from '@/shared/proto/cline/browser'

interface ConnectionInfo {
  isConnected: boolean
  isRemote: boolean
  host?: string
}

const extensionState = computed(() => useExtensionStateStore().extensionState)
const router = useRouter()

const containerRef = ref<HTMLDivElement | null>(null)
const popoverRef = ref<HTMLDivElement | null>(null)
const showInfoPopover = ref(false)

const browserSettings = computed(() => extensionState.value?.browserSettings || {
  remoteBrowserEnabled: false,
  remoteBrowserHost: undefined,
})

const connectionInfo = ref<ConnectionInfo>({
  isConnected: false,
  isRemote: !!browserSettings.value.remoteBrowserEnabled,
  host: browserSettings.value.remoteBrowserHost,
})

// Fetch connection info function
const fetchConnectionInfo = async () => {
  try {
    console.log('[DEBUG] SENDING BROWSER CONNECTION INFO REQUEST')
    const info: BrowserConnectionInfo = await browserService.getBrowserConnectionInfo()
    console.log('[DEBUG] GOT BROWSER REPLY:', info, typeof info)
    connectionInfo.value = {
      isConnected: info.isConnected,
      isRemote: info.isRemote,
      host: info.host,
    }
  } catch (error) {
    console.error('Error fetching browser connection info:', error)
  }
}

// Get actual connection info from the browser session
watch(
  [() => browserSettings.value.remoteBrowserHost, () => browserSettings.value.remoteBrowserEnabled],
  () => {
    fetchConnectionInfo()
  },
  { immediate: true }
)

// Close popover when clicking outside
let clickOutsideHandler: ((event: MouseEvent) => void) | null = null

watch(showInfoPopover, (isOpen) => {
  if (isOpen) {
    clickOutsideHandler = (event: MouseEvent) => {
      if (
        popoverRef.value &&
        !popoverRef.value.contains(event.target as Node) &&
        !event.composedPath().some((el) => (el as HTMLElement).classList?.contains('browser-info-icon'))
      ) {
        showInfoPopover.value = false
      }
    }

    document.addEventListener('mousedown', clickOutsideHandler)
  } else {
    if (clickOutsideHandler) {
      document.removeEventListener('mousedown', clickOutsideHandler)
      clickOutsideHandler = null
    }
  }
})

const navigateToSettings = () => {
  router.push({ name: 'Settings' })
}

const openBrowserSettings = () => {
  // First open the settings panel using direct navigation
  navigateToSettings()

  // After a short delay, send a message to scroll to browser settings
  setTimeout(async () => {
    try {
      await uiService.scrollToSettings('browser')
    } catch (error) {
      console.error('Error scrolling to browser settings:', error)
    }
  }, 300) // Give the settings panel time to open
}

const toggleInfoPopover = () => {
  const wasOpen = showInfoPopover.value
  showInfoPopover.value = !showInfoPopover.value

  // Request updated connection info when opening the popover
  if (!wasOpen && showInfoPopover.value) {
    fetchConnectionInfo()
  }
}

// Determine icon based on connection state
const getIconClass = () => {
  if (connectionInfo.value.isRemote) {
    return 'i-codicon:remote'
  } else {
    return connectionInfo.value.isConnected ? 'i-codicon:vm-running' : 'i-codicon:info'
  }
}

// Determine icon color based on connection state
const getIconColor = () => {
  if (connectionInfo.value.isRemote) {
    return connectionInfo.value.isConnected ? 'var(--vscode-charts-blue)' : 'var(--vscode-foreground)'
  } else if (connectionInfo.value.isConnected) {
    return 'var(--vscode-charts-green)'
  } else {
    return 'var(--vscode-foreground)'
  }
}

let connectionInfoIntervalId: ReturnType<typeof setInterval> | null = null

// Check connection status every second to keep icon in sync
onMounted(() => {
  // Request connection info immediately
  fetchConnectionInfo()

  // Set up interval to refresh every second
  connectionInfoIntervalId = setInterval(fetchConnectionInfo, 1000)
})

onBeforeUnmount(() => {
  // Cleanup interval
  if (connectionInfoIntervalId) {
    clearInterval(connectionInfoIntervalId)
  }
  // Cleanup click outside handler
  if (clickOutsideHandler) {
    document.removeEventListener('mousedown', clickOutsideHandler)
  }
})
</script>

