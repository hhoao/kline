<template>
  <div
    ref="listRef"
    :class="['flex', props.class]"
    role="tablist"
  >
    <slot />
  </div>
</template>

<script setup lang="ts">
import { ref, provide } from 'vue'

interface Props {
  value: string
  class?: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:value': [value: string]
}>()

const listRef = ref<HTMLDivElement | null>(null)

const handleTabSelect = (tabValue: string) => {
  console.log('Tab selected:', tabValue)
  emit('update:value', tabValue)
}

provide('tabContext', {
  selectedValue: () => props.value,
  selectTab: handleTabSelect,
})
</script>

