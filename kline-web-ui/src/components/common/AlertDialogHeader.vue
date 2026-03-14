<template>
  <div :class="['flex flex-col gap-1 text-left', props.class]">
    <slot />
  </div>
</template>

<script setup lang="ts">
interface Props {
  class?: string
}

const props = defineProps<Props>()
</script>

