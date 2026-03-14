<template>
  <div
    :class="['flex-1 overflow-auto', props.class]"
    @wheel="handleWheel"
  >
    <slot />
  </div>
</template>

<script setup lang="ts">
interface Props {
  class?: string
}

const props = defineProps<Props>()

const handleWheel = (e: WheelEvent) => {
  const target = e.target as HTMLElement

  // Prevent scrolling if the target is a listbox or option
  // (e.g. selects, dropdowns, etc).
  if (target.role === 'listbox' || target.role === 'option') {
    return
  }

  const currentTarget = e.currentTarget as HTMLDivElement
  currentTarget.scrollTop += e.deltaY
}
</script>

