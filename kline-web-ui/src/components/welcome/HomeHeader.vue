<template>
  <div class="flex flex-col items-center mb-5">
    <!-- <div class="my-5">
      <img
        v-if="environment"
        :src="logoUrl"
        alt="Logo"
        class="size-16"
      />
      <div v-else class="size-16 bg-[var(--vscode-icon-foreground)] opacity-20 rounded"></div>
    </div> -->
    <div class="flex justify-center items-center text-center">
      <h2 class="m-0 text-lg">What can I do for you?</h2>
      <HeroTooltip
        class="max-w-[300px]"
        content="I can develop software step-by-step by editing files, exploring projects, running commands, and using browsers. I can even extend my capabilities with MCP tools to assist beyond basic code completion."
        placement="bottom"
      >
        <span class="ml-2 text-sm cursor-pointer i-codicon:info text-link" />
      </HeroTooltip>
    </div>
  </div>
</template>

<script setup lang="ts">
import HeroTooltip from '@/components/common/HeroTooltip.vue'
import { useExtensionStateStore } from "@/stores/extensionState"
import { computed } from 'vue'

interface Props {
  shouldShowQuickWins?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  shouldShowQuickWins: false,
})

const extensionState = computed(() => useExtensionStateStore().extensionState)
const environment = computed(() => extensionState.value?.platform || 'web')

// Logo URL - 可以根据环境动态设置
const logoUrl = computed(() => {
  // 这里可以根据 environment 返回不同的 logo URL
  // 如果项目中有 logo 资源，可以导入或使用路径
  return '/assets/logo.svg' // 占位符，需要根据实际资源路径调整
})

</script>

