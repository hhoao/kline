<template>
  <div
    :class="[
      'overflow-hidden p-0 m-0 w-full h-full min-h-screen relative',
      isHidden ? 'hidden' : 'grid'
    ]"
    style="grid-template-rows: 1fr auto"
  >
    <!-- min-h-0 确保内部 flex 子元素可以收缩，从而让内部滚动生效 -->
    <div class="flex flex-col overflow-hidden min-h-0" style="grid-row: 1">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  isHidden?: boolean
}

withDefaults(defineProps<Props>(), {
  isHidden: false,
})
</script>

