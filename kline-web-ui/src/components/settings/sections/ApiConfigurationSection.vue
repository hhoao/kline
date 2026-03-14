<template>
  <div>
    <SectionHeader>API Configuration</SectionHeader>
    <Section>
      <div v-if="planActSeparateModelsSetting" class="rounded-md mb-5">
        <div class="flex gap-px mb-3 -mt-2 border-0 border-b border-solid border-[var(--vscode-panel-border)]">
          <SettingsTabButton
            :is-active="currentTab === 'plan'"
            @click="currentTab = 'plan'"
          >
            Plan Mode
          </SettingsTabButton>
          <SettingsTabButton
            :is-active="currentTab === 'act'"
            @click="currentTab = 'act'"
          >
            Act Mode
          </SettingsTabButton>
        </div>
      </div>

      <p class="text-sm text-[var(--vscode-descriptionForeground)] mb-4">
        API provider and model selection are available in the chat header. Configure your API keys and model per mode here or in the chat input area.
      </p>

      <div class="mb-4">
        <label class="flex items-center gap-2 cursor-pointer">
          <input
            :checked="planActSeparateModelsSetting"
            type="checkbox"
            class="rounded border-[var(--vscode-checkbox-border)]"
            @change="onSeparateModelsChange"
          />
          <span>Use different models for Plan and Act modes</span>
        </label>
        <p class="text-xs mt-1 text-[var(--vscode-descriptionForeground)]">
          Switching between Plan and Act mode will persist the API and model used in the previous mode.
        </p>
      </div>

      <div v-if="currentTab" class="mt-4 flex flex-col gap-3">
        <div class="flex flex-col gap-1">
          <label for="api-provider-select" class="text-sm text-[var(--vscode-foreground)]">Provider ({{ currentTab === 'plan' ? 'Plan' : 'Act' }})</label>
          <select
            id="api-provider-select"
            :value="currentProviderIdForSelect"
            class="w-full max-w-xs px-2 py-1.5 rounded border border-[var(--vscode-input-border)] bg-[var(--vscode-input-background)] text-[var(--vscode-input-foreground)] text-[13px]"
            @change="onProviderChange"
          >
            <option value="">— Select —</option>
            <option v-for="id in GENERIC_PROVIDER_IDS" :key="id" :value="id">
              {{ getGenericProviderConfig(id)?.label ?? id }}
            </option>
          </select>
        </div>
        <GenericProvider
          v-if="currentProviderId && isGenericProvider(currentProviderId)"
          :provider-id="currentProviderId"
          :current-mode="currentTab"
          :show-model-options="true"
        />
        <ThinkingBudgetSlider v-if="currentTab" :current-mode="currentTab" class="mt-2" />
        <UseCustomPromptCheckbox class="mt-2" />
      </div>
    </Section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useExtensionStateStore } from '@/stores/extensionState'
import { stateService } from '@/api/state'
import Section from '../Section.vue'
import SectionHeader from '../SectionHeader.vue'
import SettingsTabButton from '../SettingsTabButton.vue'
import GenericProvider from '../providers/GenericProvider.vue'
import ThinkingBudgetSlider from '../ThinkingBudgetSlider.vue'
import UseCustomPromptCheckbox from '../UseCustomPromptCheckbox.vue'
import {
  GENERIC_PROVIDER_IDS,
  getGenericProviderConfig,
  isGenericProvider,
} from '../providers/providerConfig'

const store = useExtensionStateStore()
const mode = computed(() => store.extensionState?.mode ?? 'plan')
const currentTab = ref<'plan' | 'act'>(mode.value === 'act' ? 'act' : 'plan')
const planActSeparateModelsSetting = computed(() => store.extensionState?.planActSeparateModelsSetting ?? false)
const apiConfiguration = computed(() => store.extensionState?.apiConfiguration)

const currentProviderId = computed(() => {
  const ac = apiConfiguration.value
  if (!ac) return ''
  const key = currentTab.value === 'plan' ? 'planModeApiProvider' : 'actModeApiProvider'
  return (ac as Record<string, string>)[key] ?? ''
})

const currentProviderIdForSelect = computed(() =>
  currentProviderId.value && isGenericProvider(currentProviderId.value) ? currentProviderId.value : ''
)

watch(mode, (m) => { currentTab.value = m === 'act' ? 'act' : 'plan' }, { immediate: true })

function onSeparateModelsChange(e: Event) {
  const checked = (e.target as HTMLInputElement).checked
  stateService.updateSettings({ planActSeparateModelsSetting: checked }).catch(console.error)
}

function onProviderChange(e: Event) {
  const value = (e.target as HTMLSelectElement).value
  const providerKey = currentTab.value === 'plan' ? 'planModeApiProvider' : 'actModeApiProvider'
  const modelKey = currentTab.value === 'plan' ? 'planModeApiModelId' : 'actModeApiModelId'
  const config = value ? getGenericProviderConfig(value) : null
  const patch: Record<string, string | undefined> = { [providerKey]: value || undefined }
  if (config && value) {
    patch[modelKey] = config.defaultModelId
  }
  stateService.updateSettings(patch as any).catch(console.error)
}
</script>
