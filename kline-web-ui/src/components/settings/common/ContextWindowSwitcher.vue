<template>
  <div v-if="switcherInfo" class="mb-0.5">
    <a
      href="#"
      class="text-[var(--vscode-textLink-foreground)] hover:underline"
      style="font-size: 10.5px"
      @click.prevent="onSwitch"
    >
      {{ switcherInfo.linkText }}
    </a>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  selectedModelId: string
  base200kModelId: string
  base1mModelId: string
}>()

const emit = defineEmits<{ modelChange: [modelId: string] }>()

const switcherInfo = computed(() => {
  const { selectedModelId, base200kModelId, base1mModelId } = props
  if (selectedModelId === base200kModelId) {
    return {
      current: base200kModelId,
      alternate: base1mModelId,
      linkText: 'Switch to 1M context window model',
    }
  }
  if (selectedModelId === base1mModelId) {
    return {
      current: base1mModelId,
      alternate: base200kModelId,
      linkText: 'Switch to 200K context window model',
    }
  }
  return null
})

function onSwitch() {
  if (switcherInfo.value) emit('modelChange', switcherInfo.value.alternate)
}
</script>
