<template>
  <div class="p-2 border-none rounded-md mb-2 bg-[var(--vscode-textBlockQuote-background)]">
    <div class="mb-3 font-azeret-mono">
      <div class="mb-2 text-error">{{ message }}</div>
      <div class="mb-3">
        <div v-if="currentBalance" class="text-foreground">
          Current Balance: <span class="font-bold">{{ currentBalance.toFixed(2) }}</span>
        </div>
        <div v-if="totalSpent" class="text-foreground">Total Spent: {{ totalSpent.toFixed(2) }}</div>
        <div v-if="totalPromotions" class="text-foreground">
          Total Promotions: {{ totalPromotions.toFixed(2) }}
        </div>
      </div>
    </div>

    <VSCodeButtonLink class="mb-2 w-full" :href="fullBuyCreditsUrl">
      <span class="i-codicon:credit-card mr-[6px] text-sm" />
      Buy Credits
    </VSCodeButtonLink>

    <button
      class="w-full bg-[var(--vscode-button-secondaryBackground)] text-[var(--vscode-button-secondaryForeground)] border-0 rounded-[2px] px-3 py-1.5 cursor-pointer font-inherit flex items-center justify-center gap-1.5"
      @click="handleRetry"
    >
      <span class="mr-1.5 i-codicon:refresh"></span>
      Retry Request
    </button>
  </div>
</template>

<script setup lang="ts">
import { accountService } from '@/api/account'
import { taskService } from '@/api/task'
import VSCodeButtonLink from '@/components/common/VSCodeButtonLink.vue'
import { useExtensionStateStore } from '@/stores/extensionState'
import { computed, onMounted, ref, watch } from 'vue'

interface Props {
  currentBalance: number
  totalSpent?: number
  totalPromotions?: number
  message: string
  buyCreditsUrl?: string
}

const props = withDefaults(defineProps<Props>(), {
  message: 'You have run out of credits.',
})

const DEFAULT_BUY_CREDITS_URL = {
  USER: 'https://app.cline.bot/dashboard/account?tab=credits&redirect=true',
  ORG: 'https://app.cline.bot/dashboard/organization?tab=credits&redirect=true',
}

const extensionStateStore = useExtensionStateStore()
const extensionState = computed(() => extensionStateStore.extensionState)
const activeOrganization = computed(() => {
  const state = extensionState.value as { activeOrganization?: { organizationId?: string } }
  return state?.activeOrganization
})

const fullBuyCreditsUrl = ref<string>('')

const dashboardUrl = computed(() => {
  return (
    props.buyCreditsUrl ??
    (activeOrganization.value?.organizationId ? DEFAULT_BUY_CREDITS_URL.ORG : DEFAULT_BUY_CREDITS_URL.USER)
  )
})

const fetchCallbackUrl = async () => {
  try {
    const callbackUrl = (await accountService.getRedirectUrl())
    const url = new URL(dashboardUrl.value)
    url.searchParams.set('callback_url', callbackUrl)
    fullBuyCreditsUrl.value = url.toString()
  } catch (error) {
    console.error('Error fetching callback URL:', error)
    // Fallback to URL without callback if the API call fails
    fullBuyCreditsUrl.value = dashboardUrl.value
  }
}

watch(dashboardUrl, fetchCallbackUrl, { immediate: true })
onMounted(() => {
  fetchCallbackUrl()
})

const handleRetry = async () => {
  try {
    await taskService.askResponse(
      {
        responseType: 'yesButtonClicked',
        taskId: extensionStateStore.conversationId || "",
      }
    )
  } catch (error) {
    console.error('Error invoking action:', error)
  }
}
</script>

