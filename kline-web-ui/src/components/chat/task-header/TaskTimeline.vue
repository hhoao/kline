<template>
  <div
    v-if="taskTimelinePropsMessages.length === 0"
    ref="containerRef"
    class="overflow-hidden relative mb-1 w-full"
  >
    <div class="flex items-center w-full h-3">
      <div
        class="w-[11px] h-[11px] rounded-full opacity-50 flex-shrink-0 mr-1"
        :style="{ backgroundColor: COLOR_GRAY }"
      />
    </div>
  </div>
  <div
    v-else
    ref="containerRef"
    class="relative mb-1 w-full h-3"
  >
    <div
      ref="scrollableRef"
      class="flex overflow-x-auto items-center timeline-scrollable"
      style="scrollbar-width: none; -ms-overflow-style: none;"
    >
      <div
        v-for="(message, index) in taskTimelinePropsMessages"
        :key="`timeline-${index}-${message.ts}`"
        class="flex-shrink-0"
      >
        <TaskTimelineTooltip :message="message">
          <div
            class="flex-shrink-0 mr-1 rounded-sm cursor-pointer hover:brightness-120"
            :style="{
              width: BLOCK_WIDTH,
              height: BLOCK_WIDTH,
              backgroundColor: getColor(message),
            }"
            @click="handleBlockClick(messageIndexMap[index])"
          />
        </TaskTimelineTooltip>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { combineApiRequests } from '@/shared/combineApiRequests'
import { combineCommandSequences } from '@/shared/combineCommandSequences'
import type { ClineMessage } from '@/shared/ExtensionMessage'
import { COLOR_GRAY } from '../colors'
import TaskTimelineTooltip from './TaskTimelineTooltip.vue'
import { getColor } from './util'

interface Props {
  messages: ClineMessage[]
  onBlockClick?: (messageIndex: number) => void
}

const props = defineProps<Props>()

const BLOCK_WIDTH = '11px'

const containerRef = ref<HTMLDivElement | null>(null)
const scrollableRef = ref<HTMLDivElement | null>(null)

const timelineData = computed(() => {
  if (props.messages.length <= 1) {
    return { taskTimelinePropsMessages: [], messageIndexMap: [] }
  }

  const processed = combineApiRequests(combineCommandSequences(props.messages.slice(1)))
  const indexMap: number[] = []

  const filtered = processed.filter((msg: ClineMessage) => {
    const originalIndex = props.messages.findIndex((originalMsg, idx) => idx > 0 && originalMsg.ts === msg.ts)

    // Filter out standard "say" events we don't want to show
    if (
      msg.type === 'say' &&
      (msg.say === 'api_req_started' ||
        msg.say === 'api_req_finished' ||
        msg.say === 'api_req_retried' ||
        msg.say === 'deleted_api_reqs' ||
        msg.say === 'checkpoint_created' ||
        msg.say === 'task_progress' ||
        msg.say === 'text' ||
        msg.say === 'reasoning')
    ) {
      return false
    }

    // Filter out "ask" events we don't want to show
    if (
      msg.type === 'ask' &&
      (msg.ask === 'resume_task' || msg.ask === 'resume_completed_task' || msg.ask === 'completion_result')
    ) {
      return false
    }
    if (originalIndex !== -1) {
      indexMap.push(originalIndex)
    }

    return true
  })
  return { taskTimelinePropsMessages: filtered, messageIndexMap: indexMap }
})

const taskTimelinePropsMessages = computed(() => timelineData.value.taskTimelinePropsMessages)
const messageIndexMap = computed(() => timelineData.value.messageIndexMap)

const handleBlockClick = (messageIndex?: number) => {
  if (props.onBlockClick && messageIndex !== undefined) {
    props.onBlockClick(messageIndex)
  }
}

watch(
  () => taskTimelinePropsMessages.value,
  async () => {
    await nextTick()
    if (scrollableRef.value && taskTimelinePropsMessages.value.length > 0) {
      scrollableRef.value.scrollLeft = scrollableRef.value.scrollWidth
    }
  },
  { immediate: true }
)

onMounted(() => {
  if (scrollableRef.value && taskTimelinePropsMessages.value.length > 0) {
    scrollableRef.value.scrollLeft = scrollableRef.value.scrollWidth
  }
})
</script>

<style scoped>
.timeline-scrollable::-webkit-scrollbar {
  display: none;
}
</style>

