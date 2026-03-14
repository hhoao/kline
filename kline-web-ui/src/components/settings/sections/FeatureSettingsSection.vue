<template>
  <div>
    <SectionHeader>Feature Settings</SectionHeader>
    <Section>
      <div class="mb-5">
        <div class="mb-4">
          <label class="flex items-center gap-2 cursor-pointer">
            <input
              :checked="enableCheckpointsSetting"
              type="checkbox"
              class="rounded border-[var(--vscode-checkbox-border)]"
              @change="(e) => updateSetting('enableCheckpointsSetting', (e.target as HTMLInputElement).checked)"
            />
            <span>Enable Checkpoints</span>
          </label>
          <p class="text-xs text-[var(--vscode-descriptionForeground)]">
            Enables extension to save checkpoints of workspace throughout the task. Uses git under the hood which
            may not work well with large workspaces.
          </p>
        </div>

        <div class="mb-4">
          <div v-if="remoteConfigSettings?.mcpMarketplaceEnabled !== undefined" class="flex items-center gap-2" :title="remoteConfigTooltip">
            <label class="flex items-center gap-2 cursor-pointer">
              <input :checked="mcpMarketplaceEnabled" type="checkbox" disabled class="rounded border-[var(--vscode-checkbox-border)]" />
              <span>Enable MCP Marketplace</span>
            </label>
            <i class="codicon codicon-lock text-[var(--vscode-descriptionForeground)] text-sm" />
          </div>
          <label v-else class="flex items-center gap-2 cursor-pointer">
            <input
              :checked="mcpMarketplaceEnabled"
              type="checkbox"
              class="rounded border-[var(--vscode-checkbox-border)]"
              @change="(e) => updateSetting('mcpMarketplaceEnabled', (e.target as HTMLInputElement).checked)"
            />
            <span>Enable MCP Marketplace</span>
          </label>
          <p class="text-xs text-[var(--vscode-descriptionForeground)]">
            Enables the MCP Marketplace tab for discovering and installing MCP servers.
          </p>
        </div>

        <div class="mb-4">
          <label class="block text-sm font-medium mb-1" for="mcp-display-mode-dropdown">
            MCP Display Mode
          </label>
          <McpDisplayModeDropdown
            :model-value="mcpDisplayMode"
            class="w-full"
            id="mcp-display-mode-dropdown"
            @update:model-value="(v) => updateSetting('mcpDisplayMode', v)"
          />
          <p class="text-xs mt-1 text-[var(--vscode-descriptionForeground)]">
            Controls how MCP responses are displayed: plain text, rich formatting with links/images, or markdown rendering.
          </p>
        </div>

        <div class="mb-4">
          <label class="flex items-center gap-2 cursor-pointer">
            <input
              :checked="mcpResponsesCollapsed"
              type="checkbox"
              class="rounded border-[var(--vscode-checkbox-border)]"
              @change="(e) => updateSetting('mcpResponsesCollapsed', (e.target as HTMLInputElement).checked)"
            />
            <span>Collapse MCP Responses</span>
          </label>
          <p class="text-xs text-[var(--vscode-descriptionForeground)] mt-1">
            Sets the default display mode for MCP response panels
          </p>
        </div>

        <div class="mb-4">
          <label class="block text-sm font-medium mb-1" for="openai-reasoning-effort-dropdown">
            OpenAI Reasoning Effort
          </label>
          <select
            id="openai-reasoning-effort-dropdown"
            :value="openaiReasoningEffort || 'medium'"
            class="w-full px-2 py-1 rounded border border-[var(--vscode-select-border)] bg-[var(--vscode-select-background)] text-[var(--vscode-select-foreground)] text-[13px]"
            @change="(e) => updateSetting('openaiReasoningEffort', (e.target as HTMLSelectElement).value)"
          >
            <option value="minimal">Minimal</option>
            <option value="low">Low</option>
            <option value="medium">Medium</option>
            <option value="high">High</option>
          </select>
          <p class="text-xs mt-1 text-[var(--vscode-descriptionForeground)]">
            Reasoning effort for the OpenAI family of models (applies to all OpenAI model providers).
          </p>
        </div>

        <div class="mb-4">
          <label class="flex items-center gap-2 cursor-pointer">
            <input
              :checked="strictPlanModeEnabled"
              type="checkbox"
              class="rounded border-[var(--vscode-checkbox-border)]"
              @change="(e) => updateSetting('strictPlanModeEnabled', (e.target as HTMLInputElement).checked)"
            />
            <span>Enable strict plan mode</span>
          </label>
          <p class="text-xs text-[var(--vscode-descriptionForeground)]">
            Enforces strict tool use while in plan mode, preventing file edits.
          </p>
        </div>

        <div class="mb-4">
          <label class="flex items-center gap-2 cursor-pointer">
            <input
              :checked="focusChainSettings?.enabled ?? false"
              type="checkbox"
              class="rounded border-[var(--vscode-checkbox-border)]"
              @change="(e) => updateSetting('focusChainSettings', { ...focusChainSettings, enabled: (e.target as HTMLInputElement).checked })"
            />
            <span>Enable Focus Chain</span>
          </label>
          <p class="text-xs text-[var(--vscode-descriptionForeground)]">
            Enables enhanced task progress tracking and automatic focus chain list management throughout tasks.
          </p>
          <div v-if="focusChainSettings?.enabled" class="mt-3 ml-5">
            <label class="block text-sm font-medium mb-1" for="focus-chain-remind-interval">
              Focus Chain Reminder Interval
            </label>
            <input
              id="focus-chain-remind-interval"
              type="number"
              min="1"
              max="100"
              :value="focusChainSettings?.remindClineInterval ?? 6"
              class="w-20 px-2 py-1 rounded border border-[var(--vscode-input-border)] bg-[var(--vscode-input-background)] text-[var(--vscode-input-foreground)] text-[13px]"
              @input="onFocusChainIntervalInput"
            />
            <p class="text-xs mt-1 text-[var(--vscode-descriptionForeground)]">
              Interval (in messages) to remind Cline about its focus chain checklist (1-100). Lower values provide more frequent reminders.
            </p>
          </div>
        </div>

        <div v-if="dictationSettings?.featureEnabled" class="mb-4">
          <label class="flex items-center gap-2 cursor-pointer">
            <input
              :checked="dictationSettings?.dictationEnabled"
              type="checkbox"
              class="rounded border-[var(--vscode-checkbox-border)]"
              @change="(e) => updateSetting('dictationSettings', { ...dictationSettings, dictationEnabled: (e.target as HTMLInputElement).checked })"
            />
            <span>Enable Dictation</span>
          </label>
          <p class="text-xs text-[var(--vscode-descriptionForeground)] mt-1">
            Enables speech-to-text transcription using your Cline account. Uses the Whisper model, at $0.006 credits per minute of audio processed. 5 minutes max per message.
          </p>
          <div v-if="dictationSettings?.dictationEnabled" class="mt-3 ml-5">
            <label class="block text-sm font-medium mb-1" for="dictation-language-dropdown">
              Dictation Language
            </label>
            <select
              id="dictation-language-dropdown"
              :value="dictationSettings?.dictationLanguage || 'en'"
              class="w-full px-2 py-1 rounded border border-[var(--vscode-select-border)] bg-[var(--vscode-select-background)] text-[var(--vscode-select-foreground)] text-[13px]"
              @change="onDictationLanguageChange"
            >
              <option v-for="lang in SUPPORTED_DICTATION_LANGUAGES" :key="lang.code" :value="lang.code">
                {{ lang.name }}
              </option>
            </select>
            <p class="text-xs mt-1 text-[var(--vscode-descriptionForeground)]">
              The language you want to speak to the Dictation service in. This is separate from your preferred UI language.
            </p>
          </div>
        </div>

        <div class="mb-4">
          <label class="flex items-center gap-2 cursor-pointer">
            <input
              :checked="useAutoCondense"
              type="checkbox"
              class="rounded border-[var(--vscode-checkbox-border)]"
              @change="(e) => updateSetting('useAutoCondense', (e.target as HTMLInputElement).checked)"
            />
            <span>Enable Auto Compact</span>
          </label>
          <p class="text-xs text-[var(--vscode-descriptionForeground)]">
            Enables advanced context management system which uses LLM based condensing for next-gen models.
            <a
              href="https://docs.cline.bot/features/auto-compact"
              target="_blank"
              rel="noopener noreferrer"
              class="text-[var(--vscode-textLink-foreground)] hover:underline"
            >Learn more</a>
          </p>
        </div>

        <div v-if="multiRootSetting?.featureFlag" class="mb-4">
          <label class="flex items-center gap-2 cursor-pointer">
            <input
              :checked="multiRootSetting?.user"
              type="checkbox"
              class="rounded border-[var(--vscode-checkbox-border)]"
              @change="(e) => updateSetting('multiRootEnabled', (e.target as HTMLInputElement).checked)"
            />
            <span>Enable Multi-Root Workspace</span>
          </label>
          <p class="text-xs text-[var(--vscode-descriptionForeground)]">
            <span class="text-[var(--vscode-errorForeground)]">Experimental: </span>
            Allows Cline to work across multiple workspaces.
          </p>
        </div>

        <div v-if="hooksEnabled?.featureFlag" class="mb-4">
          <label class="flex items-center gap-2 cursor-pointer">
            <input
              :checked="hooksEnabled?.user"
              type="checkbox"
              class="rounded border-[var(--vscode-checkbox-border)]"
              @change="(e) => updateSetting('hooksEnabled', (e.target as HTMLInputElement).checked)"
            />
            <span>Enable Hooks</span>
          </label>
          <p class="text-xs text-[var(--vscode-descriptionForeground)]">
            <span class="text-[var(--vscode-errorForeground)]">Experimental: </span>
            Allows execution of hooks from .clinerules/hooks/ directory.
          </p>
        </div>

        <div class="mb-4">
          <div v-if="remoteConfigSettings?.yoloModeToggled !== undefined" class="flex items-center gap-2" :title="remoteConfigTooltip">
            <label class="flex items-center gap-2 cursor-pointer">
              <input :checked="yoloModeToggled" type="checkbox" disabled class="rounded border-[var(--vscode-checkbox-border)]" />
              <span>Enable YOLO Mode</span>
            </label>
            <i class="codicon codicon-lock text-[var(--vscode-descriptionForeground)] text-sm" />
          </div>
          <label v-else class="flex items-center gap-2 cursor-pointer">
            <input
              :checked="yoloModeToggled"
              type="checkbox"
              class="rounded border-[var(--vscode-checkbox-border)]"
              @change="(e) => updateSetting('yoloModeToggled', (e.target as HTMLInputElement).checked)"
            />
            <span>Enable YOLO Mode</span>
          </label>
          <p class="text-xs text-[var(--vscode-errorForeground)]">
            EXPERIMENTAL & DANGEROUS: This mode disables safety checks and user confirmations. Cline will automatically approve all actions without asking. Use with extreme caution.
          </p>
        </div>

        <div v-if="subagentsEnabled !== undefined" class="mb-4 p-3 rounded-md border border-[var(--vscode-widget-border)] bg-[var(--vscode-list-hoverBackground)]">
          <label class="flex items-center gap-2 cursor-pointer">
            <input
              :checked="subagentsEnabled"
              type="checkbox"
              class="rounded border-[var(--vscode-checkbox-border)]"
              @change="(e) => updateSetting('subagentsEnabled', (e.target as HTMLInputElement).checked)"
            />
            <span class="font-semibold">{{ subagentsEnabled ? 'Subagents Enabled' : 'Enable Subagents' }}</span>
          </label>
          <p class="text-xs mt-1 text-[var(--vscode-descriptionForeground)]">
            <span class="text-[var(--vscode-errorForeground)]">Experimental: </span>
            Allows Cline to spawn subprocesses to handle focused tasks.
          </p>
          <div v-if="subagentsEnabled" class="mt-3">
            <SubagentOutputLineLimitSlider />
          </div>
        </div>
      </div>
    </Section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useExtensionStateStore } from '@/stores/extensionState'
import { SUPPORTED_DICTATION_LANGUAGES } from '@/shared/DictationSettings'
import { updateSetting } from '../utils/settingsHandlers'
import Section from '../Section.vue'
import SectionHeader from '../SectionHeader.vue'
import McpDisplayModeDropdown from '@/components/mcp/chat-display/McpDisplayModeDropdown.vue'
import SubagentOutputLineLimitSlider from '../SubagentOutputLineLimitSlider.vue'

const remoteConfigTooltip = 'This setting is managed by your organization\'s remote configuration'

const store = useExtensionStateStore()
const enableCheckpointsSetting = computed(() => store.extensionState?.enableCheckpointsSetting ?? false)
const mcpMarketplaceEnabled = computed(() => store.extensionState?.mcpMarketplaceEnabled ?? false)
const mcpDisplayMode = computed(() => store.extensionState?.mcpDisplayMode ?? 'plain')
const mcpResponsesCollapsed = computed(() => store.extensionState?.mcpResponsesCollapsed ?? false)
const openaiReasoningEffort = computed(() => store.extensionState?.openaiReasoningEffort)
const strictPlanModeEnabled = computed(() => store.extensionState?.strictPlanModeEnabled ?? false)
const focusChainSettings = computed(() => store.extensionState?.focusChainSettings ?? { enabled: false, remindClineInterval: 6 })
const dictationSettings = computed(() => store.extensionState?.dictationSettings)
const useAutoCondense = computed(() => store.extensionState?.useAutoCondense ?? false)
const yoloModeToggled = computed(() => store.extensionState?.yoloModeToggled ?? false)
const remoteConfigSettings = computed(() => store.extensionState?.remoteConfigSettings)
const multiRootSetting = computed(() => store.extensionState?.multiRootSetting)
const hooksEnabled = computed(() => store.extensionState?.hooksEnabled)
const subagentsEnabled = computed(() => store.extensionState?.subagentsEnabled)

function onFocusChainIntervalInput(e: Event) {
  const value = parseInt((e.target as HTMLInputElement).value, 10)
  if (!Number.isNaN(value) && value >= 1 && value <= 100) {
    updateSetting('focusChainSettings', { ...focusChainSettings.value, remindClineInterval: value })
  }
}

function onDictationLanguageChange(e: Event) {
  const newValue = (e.target as HTMLSelectElement).value
  const current = dictationSettings.value ?? { featureEnabled: true, dictationEnabled: true, dictationLanguage: 'en' }
  updateSetting('dictationSettings', { ...current, dictationLanguage: newValue })
}
</script>
