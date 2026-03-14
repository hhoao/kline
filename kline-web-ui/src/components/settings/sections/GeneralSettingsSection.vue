<template>
  <div>
    <SectionHeader>General Settings</SectionHeader>
    <Section>
      <PreferredLanguageSetting />
      <div class="mb-[5px]">
        <div v-if="isDisabledByRemoteConfig" class="flex items-center gap-2 mb-[5px]" title="This setting is managed by your organization's remote configuration">
          <label class="flex items-center gap-2 cursor-pointer">
            <input
              :checked="telemetryChecked"
              type="checkbox"
              disabled
              class="rounded border-[var(--vscode-checkbox-border)]"
            />
            <span>Allow error and usage reporting</span>
          </label>
          <i class="codicon codicon-lock text-[var(--vscode-descriptionForeground)] text-sm" />
        </div>
        <label v-else class="flex items-center gap-2 cursor-pointer mb-[5px]">
          <input
            :checked="telemetrySetting === 'enabled'"
            type="checkbox"
            class="rounded border-[var(--vscode-checkbox-border)]"
            @change="onTelemetryChange"
          />
          <span>Allow error and usage reporting</span>
        </label>
        <p class="text-xs mt-[5px] text-[var(--vscode-descriptionForeground)]">
          Help improve Cline by sending usage data and error reports. No code, prompts, or personal information are
          ever sent. See our
          <a
            href="https://docs.cline.bot/more-info/telemetry"
            class="text-[var(--vscode-textLink-foreground)] underline"
            target="_blank"
            rel="noopener noreferrer"
          >telemetry overview</a>
          and
          <a
            href="https://cline.bot/privacy"
            class="text-[var(--vscode-textLink-foreground)] underline"
            target="_blank"
            rel="noopener noreferrer"
          >privacy policy</a>
          for more details.
        </p>
      </div>
    </Section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useExtensionStateStore } from '@/stores/extensionState'
import { updateSetting } from '../utils/settingsHandlers'
import Section from '../Section.vue'
import SectionHeader from '../SectionHeader.vue'
import PreferredLanguageSetting from '../PreferredLanguageSetting.vue'

const store = useExtensionStateStore()
const telemetrySetting = computed(() => store.extensionState?.telemetrySetting ?? 'disabled')
const remoteConfigSettings = computed(() => store.extensionState?.remoteConfigSettings)
const isDisabledByRemoteConfig = computed(
  () => remoteConfigSettings.value?.telemetrySetting !== undefined
)
const telemetryChecked = computed(
  () => remoteConfigSettings.value?.telemetrySetting === 'enabled'
)

function onTelemetryChange(e: Event) {
  const checked = (e.target as HTMLInputElement).checked
  updateSetting('telemetrySetting', checked ? 'enabled' : 'disabled')
}
</script>
