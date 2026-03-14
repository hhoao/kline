<template>
  <!-- Single workspace result -->
  <CodeAccordian
    v-if="!parsedData.isMultiWorkspace"
    :code="content"
    :is-expanded="isExpanded"
    language="plaintext"
    :on-toggle-expand="onToggleExpand"
    :path="path + (filePattern ? `/(${filePattern})` : '')"
  />

  <!-- Multi-workspace result -->
  <div
    v-else
    class="rounded-[3px] bg-[var(--vscode-textCodeBlock-background)] overflow-hidden border border-[var(--vscode-editorGroup-border)]"
  >
    <div
      class="text-[var(--vscode-descriptionForeground)] flex items-center px-2.5 py-2.25 cursor-pointer select-none"
      @click="onToggleExpand"
    >
      <span>/</span>
      <span
        class="overflow-hidden mr-2 whitespace-nowrap text-ellipsis"
      >
        {{ path + (filePattern ? `/(${filePattern})` : '') }}
      </span>
      <div class="flex-grow"></div>
      <span
        :class="`i-codicon:chevron-${isExpanded ? 'up' : 'down'}`"
        class="text-[13.5px] my-[1px]"
      ></span>
    </div>

    <div
      v-if="isExpanded"
      class="p-2.5 border-t border-[var(--vscode-editorGroup-border)]"
    >
      <!-- Summary line -->
      <div
        class="mb-3 font-bold text-[var(--vscode-foreground)]"
      >
        {{ parsedData.summaryLine }}
      </div>

      <!-- Workspace sections -->
      <div
        v-for="(section, index) in parsedData.sections || []"
        :key="`workspace-${section.workspace}`"
        :class="{ 'mb-4': index < (parsedData.sections?.length || 0) - 1 }"
      >
        <div
          class="flex items-center gap-1.5 mb-2 px-2 py-1 bg-[var(--vscode-editor-background)] rounded-[3px] border border-[var(--vscode-editorWidget-border)]"
        >
          <span
            class="i-codicon:folder text-sm text-[var(--vscode-symbolIcon-folderForeground)]"
          ></span>
          <span
            class="font-medium text-[var(--vscode-foreground)]"
          >
            Workspace: {{ section.workspace }}
          </span>
        </div>

        <!-- Results for this workspace -->
        <div
          class="bg-[var(--vscode-textCodeBlock-background)] p-2 rounded-[3px] text-[var(--vscode-editor-font-size)] font-[var(--vscode-editor-font-family)] leading-[1.5] whitespace-pre-wrap break-words overflow-wrap-anywhere"
        >
          <pre class="m-0 font-inherit">{{ section.content.trim() }}</pre>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import CodeAccordian from '@/components/common/CodeAccordian.vue'

interface Props {
  content?: string
  isExpanded: boolean
  onToggleExpand: () => void
  path: string
  filePattern?: string
}

const props = defineProps<Props>()

// 提供默认值，避免 undefined
const content = computed(() => props.content || '')

const parsedData = computed(() => {
  // Check if this is a multi-workspace result
  const multiWorkspaceMatch = content.value.match(/^Found \d+ results? across \d+ workspaces?\./m)

  if (!multiWorkspaceMatch) {
    // Single workspace result - return as is
    return { isMultiWorkspace: false }
  }

  // Parse multi-workspace results
  const lines = content.value.split('\n')
  const sections: Array<{ workspace: string; content: string }> = []
  let currentWorkspace: string | null = null
  let currentContent: string[] = []

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]

    // Check for workspace header
    if (line.startsWith('## Workspace: ')) {
      // Save previous workspace section if exists
      if (currentWorkspace && currentContent.length > 0) {
        sections.push({
          workspace: currentWorkspace,
          content: currentContent.join('\n'),
        })
      }

      // Start new workspace section
      currentWorkspace = line.replace('## Workspace: ', '').trim()
      currentContent = []
    } else if (currentWorkspace) {
      // Add line to current workspace content
      currentContent.push(line)
    }
  }

  // Save last workspace section
  if (currentWorkspace && currentContent.length > 0) {
    sections.push({
      workspace: currentWorkspace,
      content: currentContent.join('\n'),
    })
  }

  return { isMultiWorkspace: true, sections, summaryLine: lines[0] }
})
</script>
