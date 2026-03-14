<template>
  <div>
    <SectionHeader>Terminal Settings</SectionHeader>
    <Section>
      <div class="mb-5" id="terminal-settings-section">
        <div class="mb-4">
          <label class="font-medium block mb-1" for="default-terminal-profile">
            Default Terminal Profile
          </label>
          <select
            id="default-terminal-profile"
            :value="defaultTerminalProfile || 'default'"
            class="w-full px-2 py-1 rounded border border-[var(--vscode-select-border)] bg-[var(--vscode-select-background)] text-[var(--vscode-select-foreground)] text-[13px]"
            @change="onDefaultProfileChange"
          >
            <option
              v-for="profile in availableTerminalProfiles"
              :key="profile.id"
              :value="profile.id"
              :title="profile.description"
            >
              {{ profile.name }}
            </option>
          </select>
          <p class="text-xs text-[var(--vscode-descriptionForeground)] mt-1">
            Select the default terminal Cline will use. 'Default' uses your VSCode global setting.
          </p>
        </div>

        <div class="mb-4">
          <label class="font-medium block mb-1">Shell integration timeout (seconds)</label>
          <div class="flex items-center">
            <input
              v-model="timeoutInputValue"
              type="text"
              class="w-full px-2 py-1 rounded border border-[var(--vscode-input-border)] bg-[var(--vscode-input-background)] text-[var(--vscode-input-foreground)]"
              placeholder="Enter timeout in seconds"
              @blur="onTimeoutBlur"
              @input="onTimeoutInput"
            />
          </div>
          <div v-if="timeoutError" class="text-[var(--vscode-errorForeground)] text-xs mt-1">
            {{ timeoutError }}
          </div>
          <p class="text-xs text-[var(--vscode-descriptionForeground)]">
            Set how long Cline waits for shell integration to activate before executing commands. Increase this
            value if you experience terminal connection timeouts.
          </p>
        </div>

        <div class="mb-4">
          <label class="flex items-center gap-2 cursor-pointer mb-2">
            <input
              :checked="terminalReuseEnabled ?? true"
              type="checkbox"
              class="rounded border-[var(--vscode-checkbox-border)]"
              @change="onTerminalReuseChange"
            />
            <span>Enable aggressive terminal reuse</span>
          </label>
          <p class="text-xs text-[var(--vscode-descriptionForeground)]">
            When enabled, Cline will reuse existing terminal windows that aren't in the current working directory.
            Disable this if you experience issues with task lockout after a terminal command.
          </p>
        </div>

        <div class="mb-4">
          <label class="font-medium block mb-1" for="terminal-execution-mode">
            Terminal Execution Mode
          </label>
          <select
            id="terminal-execution-mode"
            :value="vscodeTerminalExecutionMode ?? 'vscodeTerminal'"
            class="w-full px-2 py-1 rounded border border-[var(--vscode-select-border)] bg-[var(--vscode-select-background)] text-[var(--vscode-select-foreground)] text-[13px]"
            @change="onExecutionModeChange"
          >
            <option value="vscodeTerminal">VS Code Terminal</option>
            <option value="backgroundExec">Background Exec</option>
          </select>
          <p class="text-xs text-[var(--vscode-descriptionForeground)] mt-1">
            Choose whether Cline runs commands in the VS Code terminal or a background process.
          </p>
        </div>

        <TerminalOutputLineLimitSlider />

        <div class="mt-5 p-3 rounded border bg-[var(--vscode-textBlockQuote-background)] border-[var(--vscode-textBlockQuote-border)]">
          <p class="text-[13px] m-0">
            <strong>Having terminal issues?</strong> Check our
            <a
              href="https://docs.cline.bot/troubleshooting/terminal-quick-fixes"
              target="_blank"
              rel="noopener noreferrer"
              class="text-[var(--vscode-textLink-foreground)] underline"
            >Terminal Quick Fixes</a>
            or the
            <a
              href="https://docs.cline.bot/troubleshooting/terminal-integration-guide"
              target="_blank"
              rel="noopener noreferrer"
              class="text-[var(--vscode-textLink-foreground)] underline"
            >Complete Troubleshooting Guide</a>.
          </p>
        </div>
      </div>
    </Section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useExtensionStateStore } from '@/stores/extensionState'
import { stateService } from '@/api/state'
import { updateSetting } from '../utils/settingsHandlers'
import Section from '../Section.vue'
import SectionHeader from '../SectionHeader.vue'
import TerminalOutputLineLimitSlider from '../TerminalOutputLineLimitSlider.vue'

const store = useExtensionStateStore()
const shellIntegrationTimeout = computed(() => store.extensionState?.shellIntegrationTimeout ?? 30000)
const terminalReuseEnabled = computed(() => store.extensionState?.terminalReuseEnabled)
const defaultTerminalProfile = computed(() => store.extensionState?.defaultTerminalProfile)
const availableTerminalProfiles = computed(() => store.availableTerminalProfiles ?? [])
const vscodeTerminalExecutionMode = computed(() => store.extensionState?.vscodeTerminalExecutionMode)

const timeoutInputValue = ref(String((shellIntegrationTimeout.value / 1000)))
const timeoutError = ref<string | null>(null)

watch(shellIntegrationTimeout, (v) => {
  timeoutInputValue.value = String(v / 1000)
}, { immediate: true })

function onDefaultProfileChange(e: Event) {
  const value = (e.target as HTMLSelectElement).value
  updateSetting('defaultTerminalProfile', value || 'default')
}

function onTimeoutInput(e: Event) {
  const value = (e.target as HTMLInputElement).value
  timeoutInputValue.value = value
  const seconds = parseFloat(value)
  if (Number.isNaN(seconds) || seconds <= 0) {
    timeoutError.value = 'Please enter a positive number'
    return
  }
  timeoutError.value = null
  const timeoutMs = Math.round(seconds * 1000)
  stateService.updateSettings({ shellIntegrationTimeout: timeoutMs }).catch(console.error)
}

function onTimeoutBlur() {
  if (timeoutError.value) {
    timeoutInputValue.value = String(shellIntegrationTimeout.value / 1000)
    timeoutError.value = null
  }
}

function onTerminalReuseChange(e: Event) {
  const checked = (e.target as HTMLInputElement).checked
  updateSetting('terminalReuseEnabled', checked)
}

function onExecutionModeChange(e: Event) {
  const value = (e.target as HTMLSelectElement).value
  updateSetting('vscodeTerminalExecutionMode', value === 'backgroundExec' ? 'backgroundExec' : 'vscodeTerminal')
}
</script>
