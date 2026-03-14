<template>
  <Teleport to="body">
    <div
      v-if="isVisible"
      ref="modalRef"
      class="overflow-hidden"
    >
      <div
        :class="[
          'fixed left-[15px] right-[15px] border border-[var(--vscode-editorGroup-border)] rounded z-[1000] flex flex-col'
        ]"
        :style="modalStyle"
      >
        <!-- Arrow indicator -->
        <div
          class="fixed w-[10px] h-[10px] z-[-1] rotate-45 border-r border-b border-[var(--vscode-editorGroup-border)]"
          :style="arrowStyle"
        />
        
        <!-- Scrollable content container -->
        <div class="overflow-y-auto overscroll-contain flex-1 p-3 min-h-0">
          <div class="flex justify-between items-center mb-3">
            <div
              class="inline-block relative group"
              :title="'Auto-approve allows Cline to perform the following actions without asking for permission. Please use with caution and only enable if you understand the risks.'"
            >
              <div class="mb-1 text-base font-semibold">Auto-approve Settings</div>
            </div>
            <button
              class="p-1 cursor-pointer vscode-button appearance-icon hover:opacity-80"
              @click="$emit('update:isVisible', false)"
              aria-label="Close"
            >
              <span class="i-codicon:close text-[10px]"></span>
            </button>
          </div>

          <div class="mb-2.5">
            <span class="text-[color:var(--vscode-foreground)] font-medium">Actions:</span>
          </div>

          <div
            ref="itemsContainerRef"
            class="relative mb-6"
            :style="{
              columnCount: containerWidth > breakpoint ? 2 : 1,
              columnGap: '4px',
            }"
          >
            <!-- Vertical separator line - only visible in two-column mode -->
            <div
              v-if="containerWidth > breakpoint"
              class="absolute left-1/2 top-0 bottom-0 w-[0.5px] opacity-20 bg-[var(--vscode-titleBar-inactiveForeground)]"
              style="transform: translateX(-50%)"
            />

            <!-- All items in a single list - CSS Grid will handle the column distribution -->
            <AutoApproveMenuItem
              v-for="action in props.actionMetadata"
              :key="action.id"
              :action="action"
              :is-checked="isChecked"
              :is-favorited="isFavorited"
              :on-toggle="updateAction"
              :on-toggle-favorite="toggleFavorite"
            />
          </div>

          <div class="mb-2.5">
            <span class="text-[color:var(--vscode-foreground)] font-medium">Quick Settings:</span>
          </div>

          <AutoApproveMenuItem
            :action="props.notificationsSetting"
            :is-checked="isChecked"
            :is-favorited="isFavorited"
            :on-toggle="updateAction"
            :on-toggle-favorite="toggleFavorite"
          />

          <div
            class="inline-block relative group"
            :title="'Cline will automatically make this many API requests before asking for approval to proceed with the task.'"
          >
            <div class="flex items-center pl-1.5 my-2">
              <span class="i-codicon:settings text-[#CCCCCC] text-[14px]" />
              <span class="text-[#CCCCCC] text-xs font-medium ml-2">Max Requests:</span>
              <input
                type="text"
                :value="autoApprovalSettings.maxRequests.toString()"
                class="flex-1 w-full pr-[35px] ml-4 vscode-text-field"
                @input="handleMaxRequestsInput"
                @keydown="handleMaxRequestsKeydown"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { CODE_BLOCK_BG_COLOR } from '@/components/common/CodeBlock'
import { useAutoApproveActions } from '@/hooks/useAutoApproveActions'
import AutoApproveMenuItem from './AutoApproveMenuItem.vue'
import type { ActionMetadata } from './types'

interface Props {
  isVisible: boolean
  buttonRef: { value: HTMLElement | null }
  actionMetadata: ActionMetadata[]
  notificationsSetting: ActionMetadata
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:isVisible': [visible: boolean]
}>()

const { autoApprovalSettings, isChecked, isFavorited, toggleFavorite, updateAction, updateMaxRequests } = useAutoApproveActions()

const modalRef = ref<HTMLDivElement | null>(null)
const itemsContainerRef = ref<HTMLDivElement | null>(null)
const arrowPosition = ref(0)
const menuPosition = ref(0)
const containerWidth = ref(0)
const viewportWidth = ref(window.innerWidth)
const viewportHeight = ref(window.innerHeight)

const breakpoint = 500

// Calculate modal style
const modalStyle = computed(() => {
  const originalBottom = viewportHeight.value - menuPosition.value + 6
  const availableSpace = viewportHeight.value - originalBottom
  const minTopMargin = 15
  const maxAvailableHeight = availableSpace - minTopMargin
  const originalMaxHeight = viewportHeight.value - 100

  let finalMaxHeight: number
  if (menuPosition.value <= minTopMargin) {
    finalMaxHeight = maxAvailableHeight
  } else {
    finalMaxHeight = Math.min(originalMaxHeight, maxAvailableHeight)
  }

  return {
    bottom: `${originalBottom}px`,
    maxHeight: `${Math.max(finalMaxHeight, 200)}px`,
    background: CODE_BLOCK_BG_COLOR,
    overscrollBehavior: 'contain' as const,
  }
})

// Calculate arrow style
const arrowStyle = computed(() => {
  return {
    bottom: `calc(100vh - ${menuPosition.value}px)`,
    right: `${arrowPosition.value}px`,
    background: CODE_BLOCK_BG_COLOR,
  }
})


// Calculate positions for modal and arrow
const updatePositions = () => {
  if (props.isVisible && props.buttonRef.value) {
    const buttonRect = props.buttonRef.value.getBoundingClientRect()
    const buttonCenter = buttonRect.left + buttonRect.width / 2
    const rightPosition = document.documentElement.clientWidth - buttonCenter - 5

    arrowPosition.value = rightPosition
    menuPosition.value = buttonRect.top + 1
  }
}

// Track container width for responsive layout
const updateWidth = () => {
  if (itemsContainerRef.value) {
    containerWidth.value = itemsContainerRef.value.offsetWidth
  }
}

let resizeObserver: ResizeObserver | null = null

watch(() => props.isVisible, async (newVal) => {
  if (newVal) {
    await nextTick()
    updatePositions()
    updateWidth()
    
    if (itemsContainerRef.value) {
      resizeObserver = new ResizeObserver(updateWidth)
      resizeObserver.observe(itemsContainerRef.value)
    }
  } else {
    if (resizeObserver) {
      resizeObserver.disconnect()
      resizeObserver = null
    }
  }
}, { immediate: true })

watch([() => props.isVisible, viewportWidth, viewportHeight], async () => {
  if (props.isVisible) {
    await nextTick()
    updatePositions()
  }
})

// Handle window resize
const handleResize = () => {
  viewportWidth.value = window.innerWidth
  viewportHeight.value = window.innerHeight
  if (props.isVisible) {
    updatePositions()
  }
}

// Handle click outside
const handleClickOutside = (e: MouseEvent) => {
  if (
    modalRef.value &&
    !modalRef.value.contains(e.target as Node) &&
    props.buttonRef.value &&
    !props.buttonRef.value.contains(e.target as Node)
  ) {
    emit('update:isVisible', false)
  }
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  document.addEventListener('mousedown', handleClickOutside)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  document.removeEventListener('mousedown', handleClickOutside)
  if (resizeObserver) {
    resizeObserver.disconnect()
  }
})

// Handle max requests input
const handleMaxRequestsInput = async (e: Event) => {
  const input = e.target as HTMLInputElement
  // Remove any non-numeric characters
  input.value = input.value.replace(/[^0-9]/g, '')
  const value = parseInt(input.value)
  if (!Number.isNaN(value) && value > 0) {
    await updateMaxRequests(value)
  }
}

// Handle max requests keydown
const handleMaxRequestsKeydown = (e: KeyboardEvent) => {
  // Prevent non-numeric keys (except for backspace, delete, arrows)
  if (
    !/^\d$/.test(e.key) &&
    !['Backspace', 'Delete', 'ArrowLeft', 'ArrowRight'].includes(e.key)
  ) {
    e.preventDefault()
  }
}
</script>

<style scoped>
.vscode-button {
  background: transparent;
  border: none;
  color: var(--vscode-foreground);
  cursor: pointer;
}

.vscode-button.appearance-icon {
  padding: 4px;
}

.vscode-text-field {
  background: var(--vscode-input-background);
  color: var(--vscode-input-foreground);
  border: 1px solid var(--vscode-input-border);
  padding: 4px 8px;
  border-radius: 2px;
  font-size: 12px;
}

.vscode-text-field:focus {
  outline: 1px solid var(--vscode-focusBorder);
  outline-offset: -1px;
}
</style>

