<template>
  <div :class="['ph-no-capture', compact ? 'compact' : '']" :style="markdownStyle" v-html="renderedContent" />
</template>

<script setup lang="ts">
import { computed, watch, ref, nextTick } from 'vue'
import { CODE_BLOCK_BG_COLOR } from './CodeBlock'

interface Props {
  markdown?: string
  compact?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  compact: false,
})

// TODO: Replace with proper markdown parsing library (e.g., markdown-it + plugins)
// For now, we'll do basic markdown parsing
const renderedContent = ref('')

const markdownStyle = computed(() => ({
  fontFamily: "var(--vscode-font-family, system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif)",
  fontSize: 'var(--vscode-font-size, 13px)',
  color: 'var(--vscode-editor-foreground, #fff)',
}))

const parseMarkdown = (markdown: string): string => {
  if (!markdown) return ''
  
  let html = markdown
  
  // Convert URLs to links
  const urlRegex = /https?:\/\/[^\s<>)"]+/g
  html = html.replace(urlRegex, (url) => {
    return `<a href="${url}" target="_blank" rel="noopener noreferrer" style="color: var(--vscode-textLink-foreground); text-decoration: none;">${url}</a>`
  })
  
  // Handle code blocks (including mermaid)
  const codeBlockRegex = /```(\w+)?\n([\s\S]*?)```/g
  html = html.replace(codeBlockRegex, (_match, lang, code) => {
    const language = lang || 'javascript'
    const finalLang = language.includes('.') ? language.split('.').slice(-1)[0] : language
    
    // Handle mermaid diagrams
    if (finalLang === 'mermaid') {
      return `<div class="mermaid-block" data-code="${escapeHtml(code)}"></div>`
    }
    
    const escapedCode = escapeHtml(code)
    return `<pre style="background-color: ${CODE_BLOCK_BG_COLOR}; border-radius: 5px; margin: 0; padding: 10px;"><code class="language-${finalLang}">${escapedCode}</code></pre>`
  })
  
  // Handle inline code
  html = html.replace(/`([^`]+)`/g, '<code style="font-family: var(--vscode-editor-font-family, monospace); color: var(--vscode-textPreformat-foreground, #f78383); background-color: var(--vscode-textCodeBlock-background, #1e1e1e); padding: 0 2px; border-radius: 3px; border: 1px solid var(--vscode-textSeparator-foreground, #424242); white-space: pre-line; word-break: break-word; overflow-wrap: anywhere;">$1</code>')
  
  // Handle bold
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  
  // Handle italic
  html = html.replace(/\*(.+?)\*/g, '<em>$1</em>')
  
  // Handle headers
  html = html.replace(/^### (.+)$/gm, '<h3>$1</h3>')
  html = html.replace(/^## (.+)$/gm, '<h2>$1</h2>')
  html = html.replace(/^# (.+)$/gm, '<h1>$1</h1>')
  
  // Handle lists
  html = html.replace(/^\- (.+)$/gm, '<li>$1</li>')
  html = html.replace(/(<li>.*<\/li>)/s, '<ul>$1</ul>')
  
  // Handle paragraphs (split by double newlines)
  html = html.split('\n\n').map(para => {
    if (para.trim() && !para.match(/^<[hul]/)) {
      return `<p style="white-space: pre-wrap; ${props.compact ? 'margin: 0;' : ''}">${para}</p>`
    }
    return para
  }).join('\n\n')
  
  return html
}

const escapeHtml = (text: string): string => {
  const div = document.createElement('div')
  div.textContent = text
  return div.innerHTML
}

watch(() => props.markdown, (newMarkdown) => {
  renderedContent.value = parseMarkdown(newMarkdown || '')
  
  // Handle mermaid blocks after rendering
  nextTick(() => {
    const mermaidBlocks = document.querySelectorAll('.mermaid-block')
    mermaidBlocks.forEach((block) => {
      const code = block.getAttribute('data-code')
      if (code) {
        // TODO: Render MermaidBlock component here
        // For now, just show the code
        block.innerHTML = `<pre><code>${escapeHtml(code)}</code></pre>`
      }
    })
  })
}, { immediate: true })
</script>

<style scoped>
.compact p {
  margin: 0;
}

p,
li,
ol,
ul {
  line-height: 1.25;
}

ol,
ul {
  padding-left: 2.5em;
  margin-left: 0;
}

a {
  text-decoration: none;
}

a:hover {
  text-decoration: underline;
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
  border-radius: 3px;
  background-color: v-bind(CODE_BLOCK_BG_COLOR);
  font-size: var(--vscode-editor-font-size, var(--vscode-font-size, 12px));
  font-family: var(--vscode-editor-font-family);
}
</style>

