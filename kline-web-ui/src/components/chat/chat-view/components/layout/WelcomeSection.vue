<template>
  <div class="flex flex-col flex-1 p-0 m-0 w-full h-full">
    <div class="flex overflow-y-auto flex-col pb-2.5">
      <Announcement
        v-if="showAnnouncement"
        :version="version"
        @hide-announcement="hideAnnouncement"
      />
      <HomeHeader :should-show-quick-wins="shouldShowQuickWins" />
      <HistoryPreview
        v-if="!shouldShowQuickWins && taskHistory.length > 0"
        :show-history-view="showHistoryView"
      />
    </div>
    <SuggestedTasks :should-show-quick-wins="shouldShowQuickWins" />
  </div>
</template>

<script setup lang="ts">
import Announcement from '@/components/chat/Announcement.vue'
import HistoryPreview from '@/components/history/HistoryPreview.vue'
import HomeHeader from '@/components/welcome/HomeHeader.vue'
import SuggestedTasks from '@/components/welcome/SuggestedTasks.vue'
import type { WelcomeSectionProps } from '../../types/chatTypes'

const props = defineProps<WelcomeSectionProps>()

</script>

