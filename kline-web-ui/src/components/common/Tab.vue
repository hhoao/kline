<template>
  <div :class="['fixed inset-0 flex flex-col', props.class]">
    <slot />
  </div>
</template>

<script setup lang="ts">
interface Props {
  class?: string
}

const props = defineProps<Props>()
</script>

