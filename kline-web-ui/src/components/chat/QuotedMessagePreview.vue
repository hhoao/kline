<template>
  <div v-if="activeQuote" class="preview-container">
    <div class="content-row">
      <span class="reply-icon i-codicon:reply"></span>
      <div class="text-container" :title="activeQuote">{{ activeQuote }}</div>
      <button
        class="dismiss-button"
        aria-label="Dismiss quote"
        @click="handleDismiss"
      >
        <span class="i-codicon:close"></span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useChatStateStore } from '@/stores/chatState'

const chatStateStore = useChatStateStore()
const { activeQuote } = storeToRefs(chatStateStore)
const { setActiveQuote } = chatStateStore

const handleDismiss = () => {
  setActiveQuote(null)
}
</script>

<style scoped>
.preview-container {
  background-color: var(--vscode-input-background);
  padding: 4px 4px 4px 4px;
  margin: 0px 15px 0 15px;
  border-radius: 2px 2px 0 0;
  display: flex;
  position: relative;
}

.content-row {
  background-color: color-mix(in srgb, var(--vscode-input-background) 70%, white 30%);
  border-radius: 2px 2px 2px 2px;
  padding: 8px 10px 10px 8px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  width: 100%;
}

.text-container {
  flex-grow: 1;
  margin: 0 2px;
  white-space: pre-wrap;
  word-break: break-word;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
  font-size: var(--vscode-editor-font-size);
  opacity: 0.9;
  line-height: 1.4;
  max-height: calc(1.4 * var(--vscode-editor-font-size) * 3);
}

.dismiss-button {
  flex-shrink: 0;
  min-width: 22px;
  height: 22px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--vscode-foreground);
}

.dismiss-button:hover {
  background-color: var(--vscode-button-hoverBackground);
}

.reply-icon {
  color: var(--vscode-descriptionForeground);
  margin-right: 2px;
  flex-shrink: 0;
  font-size: 13px;
}
</style>

