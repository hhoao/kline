import { computed } from 'vue'
import { useExtensionStateStore } from "@/stores/extensionState"
import { AutoApprovalSettings } from '@/shared/AutoApprovalSettings'
import { ActionMetadata } from '@/components/chat/auto-approve-menu/types'
import { updateAutoApproveSettings } from '@/components/chat/auto-approve-menu/AutoApproveSettingsAPI'

/**
 * Vue composable for managing auto-approve actions
 */
export function useAutoApproveActions() {
  const extensionStateStore = useExtensionStateStore()

  const autoApprovalSettings = computed(() => {
    return extensionStateStore.extensionState?.autoApprovalSettings || {
      version: 1,
      enabled: false,
      actions: {
        readFiles: false,
        editFiles: false,
        useBrowser: false,
        useMcp: false,
      },
      maxRequests: 10,
      enableNotifications: false,
      favorites: [],
    } as AutoApprovalSettings
  })

  /**
   * Check if an action is enabled
   */
  const isChecked = (action: ActionMetadata): boolean => {
    const settings = autoApprovalSettings.value
    const actionId = action.id

    switch (actionId) {
      case 'enableAll':
        return Object.values(settings.actions).every(Boolean)
      case 'enableNotifications':
        return settings.enableNotifications
      case 'enableAutoApprove':
        return settings.enabled
      default:
        return settings.actions[actionId as keyof typeof settings.actions] ?? false
    }
  }

  /**
   * Check if an action is favorited
   */
  const isFavorited = (action: ActionMetadata): boolean => {
    const favorites = autoApprovalSettings.value.favorites || []
    return favorites.includes(action.id)
  }

  /**
   * Toggle favorite status of an action
   */
  const toggleFavorite = async (actionId: string): Promise<void> => {
    const settings = autoApprovalSettings.value
    const currentFavorites = settings.favorites || []
    let newFavorites: string[]

    if (currentFavorites.includes(actionId)) {
      newFavorites = currentFavorites.filter((id) => id !== actionId)
    } else {
      newFavorites = [...currentFavorites, actionId]
    }

    await updateAutoApproveSettings({
      ...settings,
      version: (settings.version ?? 1) + 1,
      favorites: newFavorites,
    })
  }

  /**
   * Update an action's enabled state
   */
  const updateAction = async (action: ActionMetadata, value: boolean): Promise<void> => {
    const settings = autoApprovalSettings.value
    const actionId = action.id
    const subActionId = action.subAction?.id

    if (actionId === 'enableAutoApprove') {
      await updateAutoApproveEnabled(value)
      return
    }

    if (actionId === 'enableAll' || subActionId === 'enableAll') {
      await toggleAll(action, value)
      return
    }

    if (actionId === 'enableNotifications' || subActionId === 'enableNotifications') {
      await updateNotifications(action, value)
      return
    }

    const newActions = {
      ...settings.actions,
      [actionId]: value,
    }

    if (value === false && subActionId) {
      // @ts-expect-error: TODO: See how we can fix this
      newActions[subActionId] = false
    }

    if (value === true && action.parentActionId) {
      newActions[action.parentActionId as keyof AutoApprovalSettings['actions']] = true
    }

    // Check if this will result in any enabled actions
    const willHaveEnabledActions = Object.values(newActions).some(Boolean)

    await updateAutoApproveSettings({
      ...settings,
      version: (settings.version ?? 1) + 1,
      actions: newActions,
      enabled: willHaveEnabledActions,
    })
  }

  /**
   * Update max requests setting
   */
  const updateMaxRequests = async (maxRequests: number): Promise<void> => {
    const settings = autoApprovalSettings.value
    await updateAutoApproveSettings({
      ...settings,
      version: (settings.version ?? 1) + 1,
      maxRequests,
    })
  }

  /**
   * Update auto-approve enabled state
   */
  const updateAutoApproveEnabled = async (checked: boolean): Promise<void> => {
    const settings = autoApprovalSettings.value
    await updateAutoApproveSettings({
      ...settings,
      version: (settings.version ?? 1) + 1,
      enabled: checked,
    })
  }

  /**
   * Toggle all actions
   */
  const toggleAll = async (_action: ActionMetadata, checked: boolean): Promise<void> => {
    const settings = autoApprovalSettings.value
    const actions = { ...settings.actions }

    for (const action of Object.keys(actions)) {
      actions[action as keyof AutoApprovalSettings['actions']] = checked
    }

    await updateAutoApproveSettings({
      ...settings,
      version: (settings.version ?? 1) + 1,
      actions,
      enabled: checked,
    })
  }

  /**
   * Update notifications setting
   */
  const updateNotifications = async (action: ActionMetadata, checked: boolean): Promise<void> => {
    if (action.id === 'enableNotifications') {
      const settings = autoApprovalSettings.value
      await updateAutoApproveSettings({
        ...settings,
        version: (settings.version ?? 1) + 1,
        enableNotifications: checked,
      })
    }
  }

  return {
    autoApprovalSettings,
    isChecked,
    isFavorited,
    toggleFavorite,
    updateAction,
    updateMaxRequests,
    updateAutoApproveEnabled,
    toggleAll,
    updateNotifications,
  }
}
