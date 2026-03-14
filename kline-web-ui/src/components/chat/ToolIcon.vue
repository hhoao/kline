<template>
  <span
    :class="`i-codicon:${name} ph-no-capture`"
    :style="iconStyle"
    :title="title"
  ></span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  name: string
  color?: 'red' | 'yellow' | 'green'
  rotation?: number
  title?: string
}

const props = defineProps<Props>()

const colorMap = {
  red: 'var(--vscode-errorForeground)',
  yellow: 'var(--vscode-editorWarning-foreground)',
  green: 'var(--vscode-charts-green)',
}

const iconStyle = computed(() => ({
  color: props.color ? colorMap[props.color] || props.color : 'var(--vscode-foreground)',
  marginBottom: '-1.5px',
  transform: props.rotation ? `rotate(${props.rotation}deg)` : undefined,
}))
</script>


