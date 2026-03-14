<template>
  <div>
    <label class="font-medium block mb-1">{{ providerName }} API Key</label>
    <DebouncedTextField
      :initial-value="initialValue"
      type="password"
      :placeholder="placeholder"
      :on-change="(v) => emit('change', v)"
    />
    <p class="text-xs mt-1 text-[var(--vscode-descriptionForeground)]">
      {{ helpText || 'This key is stored locally and only used to make API requests from this extension.' }}
      <a
        v-if="!initialValue && signupUrl"
        :href="signupUrl"
        target="_blank"
        rel="noopener noreferrer"
        class="text-[var(--vscode-textLink-foreground)] underline"
      >
        {{ signupLinkText }}
      </a>
    </p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import DebouncedTextField from './DebouncedTextField.vue'

const props = withDefaults(
  defineProps<{
    initialValue: string
    providerName: string
    signupUrl?: string
    placeholder?: string
    helpText?: string
  }>(),
  { placeholder: 'Enter API Key...' }
)

const emit = defineEmits<{ change: [value: string] }>()

const signupLinkText = computed(() => {
  const name = props.providerName
  const article = /^[aeiou]/i.test(name) ? 'n' : ''
  return `You can get a${article} ${name} API key by signing up here.`
})
</script>
