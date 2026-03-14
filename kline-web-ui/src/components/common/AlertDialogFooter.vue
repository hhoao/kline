<template>
  <div :class="['flex flex-row justify-end gap-3 mt-6', props.class]">
    <slot />
  </div>
</template>

<script setup lang="ts">
interface Props {
  class?: string
}

const props = defineProps<Props>()
</script>

