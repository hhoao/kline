<template>
  <div v-if="config" class="flex flex-col gap-3">
    <div v-if="config.entrypointField && config.entrypointOptions?.length" class="flex flex-col gap-1">
      <label :for="entrypointId" class="text-sm text-[var(--vscode-foreground)]">Entrypoint</label>
      <select
        :id="entrypointId"
        :value="entrypointValue"
        class="w-full px-2 py-1.5 rounded border border-[var(--vscode-input-border)] bg-[var(--vscode-input-background)] text-[var(--vscode-input-foreground)] text-[13px]"
        @change="onEntrypointChange"
      >
        <option v-for="opt in config.entrypointOptions" :key="opt.value" :value="opt.value">
          {{ opt.label }}
        </option>
      </select>
    </div>
    <ApiKeyField
      :initial-value="apiKeyValue"
      :provider-name="config.label"
      @change="onApiKeyChange"
    />
    <template v-if="showModelOptions">
      <ModelSelector
        :models="config.models"
        :selected-model-id="modelIdValue"
        :input-id="modelId"
        @change="onModelIdChange"
      />
      <ModelInfoView
        v-if="selectedModelInfo && modelIdValue"
        :selected-model-id="modelIdValue"
        :model-info="selectedModelInfo"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useExtensionStateStore } from '@/stores/extensionState'
import { stateService } from '@/api/state'
import { getGenericProviderConfig } from './providerConfig'
import ApiKeyField from '../common/ApiKeyField.vue'
import ModelInfoView from '../common/ModelInfoView.vue'
import ModelSelector from '../common/ModelSelector.vue'

const props = withDefaults(
  defineProps<{
    providerId: string
    currentMode: 'plan' | 'act'
    showModelOptions?: boolean
  }>(),
  { showModelOptions: true }
)

const store = useExtensionStateStore()
const config = computed(() => getGenericProviderConfig(props.providerId))
const apiConfiguration = computed(() => store.extensionState?.apiConfiguration)

const entrypointId = computed(() => `generic-provider-entrypoint-${props.providerId}-${props.currentMode}`)
const modelId = computed(() => `generic-provider-model-${props.providerId}-${props.currentMode}`)

const apiKeyValue = computed(() => {
  if (!config.value || !apiConfiguration.value) return ''
  return apiConfiguration.value.apiKeys?.[props.providerId] ?? ''
})

const entrypointValue = computed(() => {
  if (!config.value?.entrypointField || !apiConfiguration.value) return config.value?.entrypointOptions?.[0]?.value ?? ''
  const v = (apiConfiguration.value as Record<string, string>)[config.value.entrypointField]
  return v ?? config.value?.entrypointOptions?.[0]?.value ?? ''
})

const modelIdValue = computed(() => {
  if (!apiConfiguration.value) return config.value?.defaultModelId ?? ''
  const key = props.currentMode === 'plan' ? 'planModeApiModelId' : 'actModeApiModelId'
  const v = (apiConfiguration.value as Record<string, string>)[key]
  if (v && config.value && v in config.value.models) return v
  return config.value?.defaultModelId ?? ''
})

const selectedModelInfo = computed(() => {
  if (!config.value || !modelIdValue.value) return null
  return config.value.models[modelIdValue.value] ?? null
})

function patchSettings(patch: Record<string, string | undefined>) {
  stateService.updateSettings(patch as any).catch(console.error)
}

function onApiKeyChange(value: string) {
  if (!config.value) return
  const next = { ...(apiConfiguration.value?.apiKeys ?? {}), [props.providerId]: value }
  stateService.updateSecrets({ apiKeys: next }).catch(console.error)
}

function onEntrypointChange(e: Event) {
  if (!config.value?.entrypointField) return
  const value = (e.target as HTMLSelectElement).value
  patchSettings({ [config.value.entrypointField]: value })
}

function onModelIdChange(value: string) {
  const key = props.currentMode === 'plan' ? 'planModeApiModelId' : 'actModeApiModelId'
  patchSettings({ [key]: value })
}
</script>
