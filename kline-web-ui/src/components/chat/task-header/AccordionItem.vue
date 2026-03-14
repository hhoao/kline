<template>
  <div class="flex flex-col">
    <div
      class="flex justify-between items-center gap-3 cursor-pointer hover:bg-[var(--vscode-foreground)]/5 rounded px-1 py-0.5 transition-colors"
      @click="handleClick"
    >
      <div class="flex gap-1 items-center">
        <svg
          v-if="isExpanded"
          xmlns="http://www.w3.org/2000/svg"
          width="12"
          height="12"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="6 9 12 15 18 9" />
        </svg>
        <svg
          v-else
          xmlns="http://www.w3.org/2000/svg"
          width="12"
          height="12"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="9 18 15 12 9 6" />
        </svg>
        <div class="text-sm font-semibold">{{ title }}</div>
      </div>
      <div class="text-sm text-[var(--vscode-descriptionForeground)]">
        <slot name="value">{{ value }}</slot>
      </div>
    </div>
    <div v-if="isExpanded" class="mt-2 mb-1 ml-4 text-xs text-[var(--vscode-descriptionForeground)]">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  title: string
  value?: string | number
  isExpanded: boolean
}

defineProps<Props>()

const emit = defineEmits<{
  toggle: [event?: MouseEvent]
}>()

const handleClick = (event: MouseEvent) => {
  event.preventDefault()
  event.stopPropagation()
  emit('toggle', event)
}
</script>

