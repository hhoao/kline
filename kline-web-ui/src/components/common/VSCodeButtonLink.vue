<template>
  <a
    :href="href"
    class="no-underline text-inherit"
    v-bind="$attrs"
  >
    <button
      :class="[
        'px-4 py-2 rounded border cursor-pointer transition-colors',
        'bg-[var(--vscode-button-background)] text-[var(--vscode-button-foreground)] border-[var(--vscode-button-border)]',
        'hover:bg-[var(--vscode-button-hoverBackground)]',
        className
      ]"
      @click="$emit('click', $event)"
    >
      <slot />
    </button>
  </a>
</template>

<script setup lang="ts">
interface Props {
  href: string
  className?: string
}

const props = defineProps<Props>()

const className = props.className

defineEmits<{
  click: [event: MouseEvent]
}>()
</script>

