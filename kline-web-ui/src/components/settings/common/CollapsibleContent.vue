<template>
  <div
    :class="[
      'overflow-hidden transition-all duration-300 ease-in-out',
      isOpen ? 'max-h-[1000px] opacity-100 mt-[15px] visible' : 'max-h-0 opacity-0 mt-0 invisible'
    ]"
  >
    <slot />
  </div>
</template>

<script setup lang="ts">
defineProps<{
  isOpen: boolean
}>()
</script>
