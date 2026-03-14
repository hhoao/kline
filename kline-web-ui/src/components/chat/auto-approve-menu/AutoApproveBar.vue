<template>
  <div
    :class="[
      'px-[10px] mx-[15px] select-none rounded-[10px_10px_0_0] overflow-y-auto',
      isModalVisible ? 'bg-[var(--vscode-editor-background)]' : 'bg-transparent'
    ]"
    :style="{
      borderTop: `0.5px solid color-mix(in srgb, var(--vscode-titleBar-inactiveForeground) 20%, transparent)`,
      ...style,
    }"
  >
    <div
      ref="buttonRef"
      class="cursor-pointer py-[8px] pr-[2px] flex items-center justify-between gap-[8px]"
      @click="toggleModal"
    >
      <div
        class="flex flex-nowrap items-center overflow-x-auto gap-[4px] whitespace-nowrap"
        style="
          -ms-overflow-style: none;
          scrollbar-width: none;
          -webkit-overflow-scrolling: touch;
        "
      >
        <span>Auto-approve:</span>
        <template v-for="(favId, index) in favorites" :key="`fav-${index}`">
          <AutoApproveMenuItem
            v-if="getFavoritedAction(favId)"
            :action="getFavoritedAction(favId)!"
            condensed
            :is-checked="isChecked"
            :is-favorited="isFavorited"
            :on-toggle="updateAction"
            :show-icon="false"
          />
        </template>
        <span
          v-if="quickAccessEnabledActions.length > 0"
          class="text-[color:var(--vscode-foreground-muted)] pl-[10px] opacity-60"
        >
          ✓
        </span>
        <template v-for="(action, index) in quickAccessEnabledActions" :key="`enabled-${index}`">
          <span class="text-[color:var(--vscode-foreground-muted)] opacity-60">
            {{ action.shortName }}{{ index < quickAccessEnabledActions.length - 1 ? ',' : '' }}
          </span>
        </template>
      </div>
      <span
        :class="[
          'codicon',
          isModalVisible ? 'i-codicon:chevron-down' : 'i-codicon:chevron-up'
        ]"
      />
    </div>

    <AutoApproveModal
      :is-visible="isModalVisible"
      :button-ref="{ value: buttonRef }"
      :action-metadata="ACTION_METADATA"
      :notifications-setting="NOTIFICATIONS_SETTING"
      @update:is-visible="(val) => (isModalVisible = val)"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useAutoApproveActions } from '@/hooks/useAutoApproveActions'
import AutoApproveMenuItem from './AutoApproveMenuItem.vue'
import AutoApproveModal from './AutoApproveModal.vue'
import { ACTION_METADATA, NOTIFICATIONS_SETTING } from './constants'

interface Props {
  style?: Record<string, any>
}

defineProps<Props>()

const { autoApprovalSettings, isChecked, isFavorited, updateAction } = useAutoApproveActions()

const isModalVisible = ref(false)
const buttonRef = ref<HTMLDivElement | null>(null)

const favorites = computed(() => autoApprovalSettings.value.favorites || [])

// Get favorited action by ID
const getFavoritedAction = (favId: string) => {
  const actions = [
    ...ACTION_METADATA.flatMap((a) => [a, a.subAction]),
    NOTIFICATIONS_SETTING,
  ]
  return actions.find((a) => a?.id === favId) || null
}

// Get quick access enabled actions (excluding favorites)
const quickAccessEnabledActions = computed(() => {
  const notificationsEnabled = autoApprovalSettings.value.enableNotifications

  // Some older states may not have actions initialized; guard against undefined
  const actions = (autoApprovalSettings.value.actions ||
    {}) as Record<string, boolean>

  const enabledActionsNames = Object.keys(actions).filter((key) => actions[key])

  const enabledActions = enabledActionsNames.map((actionId) => {
    return ACTION_METADATA.flatMap((a) => [a, a.subAction]).find(
      (a) => a?.id === actionId,
    )
  })

  const minusFavorites = enabledActions.filter(
    (action) => !favorites.value.includes(action?.id ?? '') && action?.shortName
  )

  if (notificationsEnabled) {
    minusFavorites.push(NOTIFICATIONS_SETTING)
  }

  return minusFavorites.filter((action): action is NonNullable<typeof action> => action !== undefined)
})

const toggleModal = () => {
  isModalVisible.value = !isModalVisible.value
}
</script>

