<template>
  <template v-if="!text"></template>
  <template v-else>
    <template v-for="(node, index) in parsedNodes" :key="`node-${index}`">
      <strong v-if="node.type === 'strong'">
        <LightMarkdown :text="node.content" :compact="true" />
      </strong>
      <em v-else-if="node.type === 'em'">
        <LightMarkdown :text="node.content" :compact="true" />
      </em>
      <span v-else-if="!compact && node.type === 'block'" style="display: block">
        <LightMarkdown :text="node.content" :compact="true" />
      </span>
      <template v-else>{{ node.content }}</template>
    </template>
  </template>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  text: string
  compact?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  compact: false,
})

interface ParsedNode {
  type: 'text' | 'strong' | 'em' | 'block'
  content: string
}

function parseInlineEmphasis(text: string): ParsedNode[] {
  const out: ParsedNode[] = []
  const len = text.length

  const firstStar = text.indexOf('*')
  if (firstStar === -1) {
    out.push({ type: 'text', content: text })
    return out
  }

  let i = 0
  let segmentStart = 0

  while (i < len) {
    const starIdx = text.indexOf('*', i)
    if (starIdx === -1) {
      if (segmentStart < len) {
        out.push({ type: 'text', content: text.slice(segmentStart, len) })
      }
      break
    }

    // Check for bold start (**)
    if (starIdx + 1 < len && text[starIdx + 1] === '*') {
      const contentStart = starIdx + 2
      const endIdx = text.indexOf('**', contentStart)
      if (endIdx !== -1 && endIdx > contentStart) {
        if (segmentStart < starIdx) {
          out.push({ type: 'text', content: text.slice(segmentStart, starIdx) })
        }
        const inner = text.slice(contentStart, endIdx)
        out.push({ type: 'strong', content: inner })
        i = endIdx + 2
        segmentStart = i
        continue
      } else {
        i = starIdx + 1
        continue
      }
    }

    // Italic start (*)
    const contentStart = starIdx + 1
    const endIdx = text.indexOf('*', contentStart)
    if (endIdx !== -1 && endIdx > contentStart) {
      if (segmentStart < starIdx) {
        out.push({ type: 'text', content: text.slice(segmentStart, starIdx) })
      }
      const inner = text.slice(contentStart, endIdx)
      out.push({ type: 'em', content: inner })
      i = endIdx + 1
      segmentStart = i
    } else {
      i = starIdx + 1
    }
  }

  return out
}

function parseTextToNodes(text: string, compact: boolean): ParsedNode[] {
  if (text.indexOf('*') === -1) {
    if (compact) {
      return [{ type: 'text', content: text }]
    }
    const lines = text.split(/\r?\n/)
    return lines.map((line) => ({ type: 'block', content: line }))
  }

  const lines = text.split(/\r?\n/)

  if (compact) {
    const flat: ParsedNode[] = []
    for (const line of lines) {
      const inlineNodes = parseInlineEmphasis(line)
      flat.push(...inlineNodes)
    }
    return flat
  } else {
    return lines.map((line) => {
      // For block mode, we need to preserve the line structure
      // So we'll return a single block node with the full line content
      return { type: 'block', content: line }
    })
  }
}

const parsedNodes = computed(() => {
  if (!props.text) {
    return []
  }
  return parseTextToNodes(props.text, props.compact)
})
</script>

