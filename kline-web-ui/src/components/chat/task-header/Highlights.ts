import { mentionRegexGlobal } from '@/shared/context-mentions'
import { validateSlashCommand } from '@/utils/slash-commands'
import { h, type VNode } from 'vue'
import { fileService } from '@/api/file'

// Optimized highlighting functions
const highlightSlashCommands = (text: string, withShadow = true): string | VNode[] => {
  const match = text.match(/^\s*\/([a-zA-Z0-9_-]+)(\s*|$)/)
  if (!match || validateSlashCommand(match[1]) !== 'full') {
    return text
  }

  const commandName = match[1]
  const commandEndIndex = match[0].length
  const beforeCommand = text.substring(0, text.indexOf('/'))
  const afterCommand = match[2] + text.substring(commandEndIndex)

  return [
    beforeCommand,
    h('span', {
      class: withShadow ? 'mention-context-highlight-with-shadow' : 'mention-context-highlight',
      key: 'slashCommand',
    }, `/${commandName}`),
    afterCommand,
  ]
}

export const highlightMentions = (text: string, withShadow = true): string | VNode[] => {
  if (!mentionRegexGlobal.test(text)) {
    return text
  }

  const parts = text.split(mentionRegexGlobal)
  const result: (string | VNode)[] = []

  for (let i = 0; i < parts.length; i++) {
    if (i % 2 === 0) {
      if (parts[i]) {
        result.push(parts[i])
      }
    } else {
      result.push(
        h('span', {
          class: `${withShadow ? 'mention-context-highlight-with-shadow' : 'mention-context-highlight'} cursor-pointer`,
          key: `mention-${Math.floor(i / 2)}`,
          onClick: () => fileService.openMention(parts[i]),
        }, `@${parts[i]}`)
      )
    }
  }

  return result.length === 1 ? result[0] : result
}

export const highlightText = (text?: string, withShadow = true): string | VNode[] => {
  if (!text) {
    return text || ''
  }

  const slashResult = highlightSlashCommands(text, withShadow)

  if (slashResult === text) {
    return highlightMentions(text, withShadow)
  }

  if (Array.isArray(slashResult) && slashResult.length === 3) {
    const [beforeCommand, commandElement, afterCommand] = slashResult as [string, VNode, string]
    const mentionResult = highlightMentions(afterCommand, withShadow)

    return Array.isArray(mentionResult)
      ? [beforeCommand, commandElement, ...mentionResult]
      : [beforeCommand, commandElement, mentionResult]
  }

  return slashResult
}

