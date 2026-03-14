<template>
  <div class="p-0.5">
    <div
      class="inline-block relative group"
      @click.stop="handleChange"
    >
      <div
        :class="[
          'flex items-center justify-between pl-1 pr-0.5 rounded cursor-pointer transition-all duration-200',
          'hover:bg-[var(--vscode-textBlockQuote-background)]',
          favorited ? 'opacity-100' : 'opacity-100'
        ]"
      >
        <div class="flex gap-2 items-center">
          <!-- Star icon for favorites -->
          <span
            v-if="onToggleFavorite && !condensed"
            :class="[
              'p-0.5 cursor-pointer text-sm transition-opacity',
              favorited ? 'i-codicon:star-full text-[var(--vscode-terminal-ansiYellow)] opacity-100' : 'i-codicon:star-empty text-[var(--vscode-descriptionForeground)] opacity-60'
            ]"
            @click.stop="handleToggleFavorite"
            :title="favorited ? 'Remove from quick-access menu' : 'Add to quick-access menu'"
          />
          
          <!-- Checkbox -->
          <input
            type="checkbox"
            :checked="checked"
            class="vscode-checkbox"
            @change.stop="handleChange"
          />
          
          <!-- Icon -->
          <span
            v-if="showIcon"
            :class="['codicon', action.icon, 'text-[var(--vscode-foreground)] text-sm']"
          />
          
          <!-- Label -->
          <span class="text-[var(--vscode-foreground)] text-xs font-medium">
            {{ condensed ? action.shortName : action.label }}
          </span>
        </div>
      </div>
      
      <!-- Tooltip -->
      <div
        v-if="action.description"
        class="absolute bottom-full left-1/2 transform -translate-x-1/2 mb-2 px-2 py-1 bg-[var(--vscode-sideBar-background)] text-[var(--vscode-descriptionForeground)] text-xs rounded border border-[var(--vscode-input-border)] whitespace-normal max-w-[250px] opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity duration-500 z-50"
        style="transition-delay: 500ms"
      >
        {{ action.description }}
      </div>
    </div>
    
    <!-- Sub-action (animated) -->
    <div
      v-if="action.subAction && !condensed"
      :class="[
        'relative pl-6 transition-all duration-200 origin-top',
        checked ? 'scale-y-100 opacity-100 h-auto' : 'scale-y-0 opacity-0 h-0'
      ]"
      style="overflow: visible"
    >
      <AutoApproveMenuItem
        :action="action.subAction"
        :is-checked="isChecked"
        :is-favorited="isFavorited"
        :on-toggle="onToggle"
        :on-toggle-favorite="onToggleFavorite"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ActionMetadata } from './types'

interface Props {
  action: ActionMetadata
  isChecked: (action: ActionMetadata) => boolean
  isFavorited?: (action: ActionMetadata) => boolean
  onToggle: (action: ActionMetadata, checked: boolean) => Promise<void>
  onToggleFavorite?: (actionId: string) => Promise<void>
  condensed?: boolean
  showIcon?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  condensed: false,
  showIcon: true,
})

const checked = computed(() => props.isChecked(props.action))
const favorited = computed(() => props.isFavorited?.(props.action) ?? false)

const handleChange = async (e: Event) => {
  e.stopPropagation()
  await props.onToggle(props.action, !checked.value)
}

const handleToggleFavorite = async (e: Event) => {
  e.stopPropagation()
  if (props.action.id === 'enableAll') {
    return
  }
  await props.onToggleFavorite?.(String(props.action.id))
}
</script>

<style scoped>
.vscode-checkbox {
  width: 18px;
  height: 18px;
  cursor: pointer;
  accent-color: var(--vscode-checkbox-background, var(--vscode-button-background));
}
</style>

