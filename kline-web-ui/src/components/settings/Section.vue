<template>
  <div :class="['flex flex-col gap-3 p-5 py-2', $attrs.class]">
    <slot />
  </div>
</template>

<script setup lang="ts">
defineOptions({ inheritAttrs: false })
</script>
