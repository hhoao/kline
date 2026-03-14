<template>
  <button
    :class="[
      'bg-[#176f2c] border-[#176f2c] text-white',
      'hover:bg-[#197f31] hover:border-[#197f31]',
      'active:bg-[#156528] active:border-[#156528]',
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

