<template>
  <div
    :style="{
      overflowY: forceWrap ? 'visible' : 'auto',
      maxHeight: forceWrap ? 'none' : '100%',
      backgroundColor: CODE_BLOCK_BG_COLOR,
    }"
  >
    <div
      :class="[
        'ph-no-capture',
        forceWrap ? 'force-wrap' : ''
      ]"
      :style="markdownStyle"
      v-html="renderedContent"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, watch, ref } from 'vue'
import { CODE_BLOCK_BG_COLOR } from './CodeBlock'

interface Props {
  source?: string
  forceWrap?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  forceWrap: false,
})

// TODO: Replace with proper markdown parsing library (e.g., markdown-it)
// For now, we'll do basic code block extraction
const renderedContent = ref('')

const markdownStyle = computed(() => ({
  backgroundColor: CODE_BLOCK_BG_COLOR,
  fontFamily: "var(--vscode-font-family, system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif)",
  fontSize: 'var(--vscode-editor-font-size, var(--vscode-font-size, 12px))',
  color: 'var(--vscode-editor-foreground, #fff)',
}))

const parseMarkdown = (source: string): string => {
  if (!source) return ''
  
  // Basic markdown code block parsing
  const codeBlockRegex = /```(\w+)?\n([\s\S]*?)```/g
  let html = source
  
  html = html.replace(codeBlockRegex, (_match, lang, code) => {
    const language = lang || 'javascript'
    // Extract file extension if language contains a dot
    const finalLang = language.includes('.') ? language.split('.').slice(-1)[0] : language
    
    // Escape HTML
    const escapedCode = code
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;')
    
    return `<pre style="background-color: ${CODE_BLOCK_BG_COLOR}; border-radius: 5px; margin: 0; padding: 10px; min-width: ${props.forceWrap ? 'auto' : 'max-content'}"><code class="language-${finalLang}">${escapedCode}</code></pre>`
  })
  
  // Handle inline code
  html = html.replace(/`([^`]+)`/g, '<code style="font-family: var(--vscode-editor-font-family); color: #f78383; border-radius: 5px; background-color: ' + CODE_BLOCK_BG_COLOR + '">$1</code>')
  
  return html
}

watch(() => props.source, (newSource) => {
  renderedContent.value = parseMarkdown(newSource || '')
}, { immediate: true })
</script>

<style scoped>
.force-wrap pre,
.force-wrap code {
  white-space: pre-wrap;
  word-break: break-all;
  overflow-wrap: anywhere;
}

pre > code .hljs-deletion {
  background-color: var(--vscode-diffEditor-removedTextBackground);
  display: inline-block;
  width: 100%;
}

pre > code .hljs-addition {
  background-color: var(--vscode-diffEditor-insertedTextBackground);
  display: inline-block;
  width: 100%;
}

code span.line:empty {
  display: none;
}

code {
  word-wrap: break-word;
  border-radius: 5px;
  background-color: v-bind(CODE_BLOCK_BG_COLOR);
  font-size: var(--vscode-editor-font-size, var(--vscode-font-size, 12px));
  font-family: var(--vscode-editor-font-family);
}

code:not(pre > code) {
  font-family: var(--vscode-editor-font-family);
  color: #f78383;
}

p,
li,
ol,
ul {
  line-height: 1.5;
}
</style>

