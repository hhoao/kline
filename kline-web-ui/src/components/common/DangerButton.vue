<template>
  <button
    :class="[
      'bg-[#c42b2b] border-[#c42b2b] text-white',
      'hover:bg-[#a82424] hover:border-[#a82424]',
      'active:bg-[#8f1f1f] active:border-[#8f1f1f]',
      'px-4 py-2 rounded border cursor-pointer transition-colors',
      props.class
    ]"
    v-bind="$attrs"
    @click="$emit('click', $event)"
  >
    <slot />
  </button>
</template>

<script setup lang="ts">
interface Props {
  class?: string
}

const props = defineProps<Props>()

defineEmits<{
  click: [event: MouseEvent]
}>()
</script>

