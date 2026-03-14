<template>
  <button
    ref="buttonRef"
    :aria-selected="isSelected"
    :class="['focus:outline-none', props.class]"
    :data-value="value"
    :tabindex="isSelected ? 0 : -1"
    role="tab"
    @click="handleSelect"
  >
    <slot />
  </button>
</template>

<script setup lang="ts">
import { computed, inject, ref } from 'vue'

interface Props {
  value: string
  class?: string
}

const props = defineProps<Props>()

const buttonRef = ref<HTMLButtonElement | null>(null)

// Get tab context from parent TabList
const tabContext = inject<{
  selectedValue: () => string
  selectTab: (value: string) => void
}>('tabContext', {
  selectedValue: () => '',
  selectTab: () => {},
})

const isSelected = computed(() => {
  return tabContext.selectedValue() === props.value
})

const handleSelect = () => {
  tabContext.selectTab(props.value)
}
</script>

