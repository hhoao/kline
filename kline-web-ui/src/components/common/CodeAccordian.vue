<template>
  <div
    :style="{
      borderRadius: 3,
      backgroundColor: CODE_BLOCK_BG_COLOR,
      overflow: 'hidden',
      border: '1px solid var(--vscode-editorGroup-border)',
    }"
  >
    <div
      v-if="path || isFeedback || isConsoleLogs"
      :style="{
        color: 'var(--vscode-descriptionForeground)',
        display: 'flex',
        alignItems: 'center',
        padding: '9px 10px',
        cursor: isLoading ? 'wait' : 'pointer',
        opacity: isLoading ? 0.7 : 1,
        userSelect: 'none',
        WebkitUserSelect: 'none',
        MozUserSelect: 'none',
        msUserSelect: 'none',
      }"
      @click="onToggleExpand"
    >
      <template v-if="isFeedback || isConsoleLogs">
        <div style="display: flex; alignitems: center">
          <span
            :class="[
              'codicon',
              `i-codicon:${isFeedback ? 'feedback' : 'output'}`,
            ]"
            style="margin-right: 6px"
          />
          <span
            style="
              whitespace: nowrap;
              overflow: hidden;
              textoverflow: ellipsis;
              marginright: 8px;
            "
          >
            {{ isFeedback ? "User Edits" : "Console Logs" }}
          </span>
        </div>
      </template>
      <template v-else>
        <span v-if="path?.startsWith('.')">.</span>
        <span v-if="path && !path.startsWith('.')">/</span>
        <span
          style="
            whitespace: nowrap;
            overflow: hidden;
            textoverflow: ellipsis;
            marginright: 8px;
            direction: rtl;
            textalign: left;
          "
        >
          {{ cleanPathPrefix(path ?? "") }}\u200E
        </span>
      </template>
      <div style="flexgrow: 1" />
      <div
        v-if="numberOfEdits !== undefined"
        style="
          display: flex;
          alignitems: center;
          marginright: 8px;
          color: var(--vscode-descriptionForeground);
        "
      >
        <span class="i-codicon:diff-single" style="marginright: 4px" />
        <span>{{ numberOfEdits }}</span>
      </div>
      <span
        :class="['codicon', `i-codicon:chevron-${isExpanded ? 'up' : 'down'}`]"
      />
    </div>
    <div
      v-if="!(path || isFeedback || isConsoleLogs) || isExpanded"
      style="overflowx: auto; overflowy: hidden; maxwidth: 100%"
    >
      <CodeBlock
        :source="`\`\`\`${diff !== undefined ? 'diff' : inferredLanguage}\n${(
          code ??
          diff ??
          ''
        ).trim()}\n\`\`\``"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { CODE_BLOCK_BG_COLOR } from "./CodeBlock";
import { getLanguageFromPath } from "@/utils/getLanguageFromPath";
import CodeBlock from "./CodeBlock.vue";

interface Props {
  code?: string;
  diff?: string;
  language?: string | undefined;
  path?: string;
  isFeedback?: boolean;
  isConsoleLogs?: boolean;
  isExpanded: boolean;
  onToggleExpand: () => void;
  isLoading?: boolean;
}

const props = defineProps<Props>();

const cleanPathPrefix = (path: string): string =>
  path.replace(/^[^\u4e00-\u9fa5a-zA-Z0-9]+/, "");

const inferredLanguage = computed(() => {
  return (
    props.code &&
    (props.language ??
      (props.path ? getLanguageFromPath(props.path) : undefined))
  );
});

const numberOfEdits = computed(() => {
  if (props.code) {
    return (props.code.match(/[-]{3,} SEARCH/g) || []).length || undefined;
  }
  return undefined;
});
</script>
