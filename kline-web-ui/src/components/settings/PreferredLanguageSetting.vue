<template>
  <div>
    <label class="block mb-1 text-sm font-medium" for="preferred-language-dropdown">
      Preferred Language
    </label>
    <select
      id="preferred-language-dropdown"
      :value="preferredLanguage || 'English'"
      class="w-full px-2 py-1 rounded border border-[var(--vscode-select-border)] bg-[var(--vscode-select-background)] text-[var(--vscode-select-foreground)] text-[13px]"
      @change="onChange"
    >
      <option value="English">English</option>
      <option value="Arabic - العربية">Arabic - العربية</option>
      <option value="Portuguese - Português (Brasil)">Portuguese - Português (Brasil)</option>
      <option value="Czech - Čeština">Czech - Čeština</option>
      <option value="French - Français">French - Français</option>
      <option value="German - Deutsch">German - Deutsch</option>
      <option value="Hindi - हिन्दी">Hindi - हिन्दी</option>
      <option value="Hungarian - Magyar">Hungarian - Magyar</option>
      <option value="Italian - Italiano">Italian - Italiano</option>
      <option value="Japanese - 日本語">Japanese - 日本語</option>
      <option value="Korean - 한국어">Korean - 한국어</option>
      <option value="Polish - Polski">Polish - Polski</option>
      <option value="Portuguese - Português (Portugal)">Portuguese - Português (Portugal)</option>
      <option value="Russian - Русский">Russian - Русский</option>
      <option value="Simplified Chinese - 简体中文">Simplified Chinese - 简体中文</option>
      <option value="Spanish - Español">Spanish - Español</option>
      <option value="Traditional Chinese - 繁體中文">Traditional Chinese - 繁體中文</option>
      <option value="Turkish - Türkçe">Turkish - Türkçe</option>
    </select>
    <p class="text-xs text-[var(--vscode-descriptionForeground)] mt-1">
      The language that Cline should use for communication.
    </p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useExtensionStateStore } from '@/stores/extensionState'
import { updateSetting } from './utils/settingsHandlers'

const store = useExtensionStateStore()
const preferredLanguage = computed(() => store.extensionState?.preferredLanguage)

function onChange(e: Event) {
  const value = (e.target as HTMLSelectElement).value
  updateSetting('preferredLanguage', value)
}
</script>
