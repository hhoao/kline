<template>
  <div>
    <SectionHeader>Browser Settings</SectionHeader>
    <Section>
      <div id="browser-settings-section" class="mb-5">
        <div class="mb-3">
          <label class="flex items-center gap-2 cursor-pointer">
            <input
              :checked="browserSettings.disableToolUse ?? false"
              type="checkbox"
              class="rounded border-[var(--vscode-checkbox-border)]"
              @change="onDisableToolUseChange"
            />
            <span>Disable browser tool usage</span>
          </label>
          <p class="text-xs text-[var(--vscode-descriptionForeground)] mt-1">
            Prevent Cline from using browser actions (e.g. launch, click, type).
          </p>
        </div>

        <CollapsibleContent :is-open="!(browserSettings.disableToolUse ?? false)">
          <div class="mb-4">
            <label class="font-medium block mb-1">Viewport size</label>
            <select
              :value="viewportPresetKey"
              class="w-full px-2 py-1 rounded border border-[var(--vscode-select-border)] bg-[var(--vscode-select-background)] text-[var(--vscode-select-foreground)] text-[13px]"
              @change="onViewportChange"
            >
              <option
                v-for="(_, name) in BROWSER_VIEWPORT_PRESETS"
                :key="String(name)"
                :value="String(name)"
              >
                {{ name }}
              </option>
            </select>
            <p class="text-xs text-[var(--vscode-descriptionForeground)] mt-1">
              Set the size of the browser viewport for screenshots and interactions.
            </p>
          </div>

          <div class="mb-4">
            <label class="flex items-center gap-2 cursor-pointer">
              <input
                :checked="browserSettings.remoteBrowserEnabled"
                type="checkbox"
                class="rounded border-[var(--vscode-checkbox-border)]"
                @change="onRemoteBrowserChange"
              />
              <span>Use remote browser connection</span>
            </label>
            <p class="text-xs text-[var(--vscode-descriptionForeground)] mt-1 mb-2">
              Enable Cline to use your Chrome{{ browserSettings.remoteBrowserEnabled ? '. You can specify a custom path below. Using a remote browser connection requires starting Chrome in debug mode manually (<code>--remote-debugging-port=9222</code>) or using the button below. Enter the host address or leave it blank for automatic discovery.' : '.' }}
            </p>
            <div v-if="browserSettings.remoteBrowserEnabled" class="mt-2">
              <DebouncedTextField
                :initial-value="browserSettings.remoteBrowserHost || ''"
                placeholder="http://localhost:9222"
                class="w-full mb-2"
                :on-change="onRemoteHostChange"
              />
            </div>
            <div class="mt-2">
              <label class="font-medium block mb-1" for="chrome-executable-path">
                Chrome Executable Path (Optional)
              </label>
              <DebouncedTextField
                id="chrome-executable-path"
                :initial-value="browserSettings.chromeExecutablePath || ''"
                placeholder="e.g., /usr/bin/google-chrome or C:\Program Files\Google\Chrome\Application\chrome.exe"
                class="w-full"
                :on-change="onChromePathChange"
              />
              <p class="text-xs text-[var(--vscode-descriptionForeground)] mt-1">Leave blank to auto-detect.</p>
            </div>
            <div class="mt-2">
              <label class="font-medium block mb-1" for="custom-browser-args">
                Custom Browser Arguments (Optional)
              </label>
              <DebouncedTextField
                id="custom-browser-args"
                :initial-value="browserSettings.customArgs || ''"
                placeholder="e.g., --no-sandbox --disable-setuid-sandbox --disable-dev-shm-usage --disable-gpu --no-first-run --no-zygote"
                class="w-full"
                :on-change="onCustomArgsChange"
              />
              <p class="text-xs text-[var(--vscode-descriptionForeground)] mt-1">Space-separated arguments to pass to the browser executable.</p>
            </div>
          </div>
        </CollapsibleContent>
      </div>
    </Section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useExtensionStateStore } from '@/stores/extensionState'
import { updateSetting } from '../utils/settingsHandlers'
import { BROWSER_VIEWPORT_PRESETS } from '@/shared/BrowserSettings'
import Section from '../Section.vue'
import SectionHeader from '../SectionHeader.vue'
import CollapsibleContent from '../common/CollapsibleContent.vue'
import DebouncedTextField from '../common/DebouncedTextField.vue'

const store = useExtensionStateStore()
const browserSettings = computed(() => store.extensionState?.browserSettings ?? {
  viewport: { width: 900, height: 600 },
  disableToolUse: false,
  remoteBrowserEnabled: false,
  remoteBrowserHost: '',
  chromeExecutablePath: '',
  customArgs: '',
})

const viewportPresetKey = computed(() => {
  const v = browserSettings.value.viewport
  const entry = Object.entries(BROWSER_VIEWPORT_PRESETS).find(
    ([_, size]) => size.width === v?.width && size.height === v?.height
  )
  return entry?.[0] ?? 'Small Desktop (900x600)'
})

function onDisableToolUseChange(e: Event) {
  const checked = (e.target as HTMLInputElement).checked
  updateSetting('browserSettings', { ...browserSettings.value, disableToolUse: checked })
}

function onViewportChange(e: Event) {
  const key = (e.target as HTMLSelectElement).value as keyof typeof BROWSER_VIEWPORT_PRESETS
  const size = BROWSER_VIEWPORT_PRESETS[key]
  if (size) {
    updateSetting('browserSettings', {
      ...browserSettings.value,
      viewport: { width: size.width, height: size.height },
    })
  }
}

function onRemoteBrowserChange(e: Event) {
  const enabled = (e.target as HTMLInputElement).checked
  updateSetting('browserSettings', {
    ...browserSettings.value,
    remoteBrowserEnabled: enabled,
    ...(enabled ? {} : { remoteBrowserHost: undefined }),
  })
}

function onRemoteHostChange(value: string) {
  updateSetting('browserSettings', { ...browserSettings.value, remoteBrowserHost: value || undefined })
}

function onChromePathChange(value: string) {
  updateSetting('browserSettings', { ...browserSettings.value, chromeExecutablePath: value })
}

function onCustomArgsChange(value: string) {
  updateSetting('browserSettings', { ...browserSettings.value, customArgs: value })
}
</script>
