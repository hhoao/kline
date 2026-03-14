<template>
  <!-- flex-1 + min-h-0 让消息区域占满剩余空间并允许内部滚动 -->
  <div class="flex overflow-hidden flex-col flex-1 min-h-0">
    <!-- 外层容器只负责承接滚轮事件，不直接滚动 -->
    <div ref="scrollContainerRef" class="flex flex-grow min-h-0">
      <div
        ref="scrollAreaRef"
        class="overflow-y-auto flex-grow min-h-0 scrollbar-thin"
        @scroll="handleScroll"
      >
        <div
          v-for="(messageOrGroup, index) in groupedMessages"
          :key="getMessageKey(messageOrGroup, index)"
        >
          <MessageRenderer
            :index="index"
            :message-or-group="messageOrGroup"
            :grouped-messages="groupedMessages"
            :modified-messages="modifiedMessages"
            :expanded-rows="stateStore.expandedRows"
            :input-value="stateStore.inputValue"
            :message-handlers="messageHandlers"
            :mode="mode"
            @toggle-expand="toggleRowExpansion"
            @height-change="handleRowHeightChange"
            @set-quote="stateStore.setActiveQuote"
          />
        </div>
        <div style="height: 5px" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ClineMessage } from '@/shared/ExtensionMessage'
import { useChatStateStore } from '@/stores/chatState'
import { nextTick, onMounted, ref, watch, type Ref } from 'vue'
import type { MessageHandlers, ScrollBehavior } from '../../types/chatTypes'
import MessageRenderer from '../messages/MessageRenderer.vue'

interface Props {
  task: ClineMessage
  groupedMessages: (ClineMessage | ClineMessage[])[]
  modifiedMessages: ClineMessage[]
  scrollBehavior: ScrollBehavior
  messageHandlers: MessageHandlers
  mode: string
  textAreaRef?: Ref<HTMLTextAreaElement | null>
}

const props = defineProps<Props>()

const {
  virtuosoRef,
  scrollContainerRef: scrollContainerRefFromBehavior,
  toggleRowExpansion,
  handleRowHeightChange,
  setIsAtBottom,
  setShowScrollToBottom,
  disableAutoScrollRef,
} = props.scrollBehavior

// 使用 Pinia store 中的展开状态和输入框内容
const stateStore = useChatStateStore()

const scrollContainerRef = ref<HTMLDivElement | null>(null)
const scrollAreaRef = ref<HTMLDivElement | null>(null)

// 同步外层容器到 scrollBehavior.scrollContainerRef（用于滚轮检测等）
watch(
  () => scrollContainerRef.value,
  (val) => {
    if (!scrollContainerRefFromBehavior) return

    // ScrollBehavior.scrollContainerRef 在 Vue 里就是 Ref<HTMLDivElement | null>
      scrollContainerRefFromBehavior.value = val
  },
  { immediate: true },
)

// 让 scrollBehavior.virtuosoRef 指向真正可滚动的 DOM 元素，
// 这样 useScrollBehavior 和 ActionButtons 里的 scrollTo* 才能工作
watch(
  () => scrollAreaRef.value,
  (val) => {
    if (!virtuosoRef) return

    // Vue 版本中 ScrollBehavior.virtuosoRef 定义为 Ref<any>
    virtuosoRef.value = val

    // 为了兼容 useScrollBehavior 中对 virtuosoRef.scrollToIndex 的调用，
    // 在真实 DOM 元素上挂一个简化版的 scrollToIndex，统一滚到底部即可。
    if (val) {
      const area = val as any
      if (typeof area.scrollToIndex !== 'function') {
        area.scrollToIndex = ({ index, align, behavior }: { index: number; align?: string; behavior?: ScrollBehavior }) => {
          area.scrollTo({
            top: area.scrollHeight,
            behavior: (behavior as any) || 'auto',
          })
  }
      }
    }
  },
  { immediate: true },
)

const getMessageKey = (messageOrGroup: ClineMessage | ClineMessage[], index: number): string => {
  if (Array.isArray(messageOrGroup)) {
    return `group-${messageOrGroup[0]?.ts || index}`
  }
  return `message-${(messageOrGroup as ClineMessage).ts || index}`
}

const handleScroll = () => {
  if (!scrollAreaRef.value) return

  const { scrollTop, scrollHeight, clientHeight } = scrollAreaRef.value
  const isAtBottomValue = scrollHeight - scrollTop - clientHeight < 10

  // 等价于 React Virtuoso 的 atBottomStateChange
  setIsAtBottom(isAtBottomValue)

  const disableRef = disableAutoScrollRef

  if (isAtBottomValue) {
      disableRef.value = false
  }

  const disableValue = disableRef.value
  setShowScrollToBottom(disableValue && !isAtBottomValue)
}

// 首次挂载时滚动到底部（对应 TSX 中 Virtuoso 的 initialTopMostItemIndex）
onMounted(() => {
  nextTick(() => {
    const area = scrollAreaRef.value
    if (!area) return
    area.scrollTop = area.scrollHeight
  })
})

// 新消息到来且仍开启自动滚动时滚动到底部
watch(
  () => props.groupedMessages.length,
  () => {
  nextTick(() => {
    const disableRef = disableAutoScrollRef
      const disableValue = disableRef.value
      const area = scrollAreaRef.value
      if (area && !disableValue) {
        area.scrollTop = area.scrollHeight
    }
  })
  },
)
</script>


