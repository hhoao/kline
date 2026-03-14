<template>
  <Teleport to="body">
    <div
      v-if="open"
      class="fixed inset-0 bg-black/50 flex items-center justify-center"
      :style="{ zIndex: zIndex }"
      @click="handleBackdropClick"
    >
      <slot />
    </div>
  </Teleport>
</template>

<script setup lang="ts">
interface Props {
  open: boolean
  zIndex?: number
}

withDefaults(defineProps<Props>(), {
  zIndex: 1050,
})

const emit = defineEmits<{
  'update:open': [open: boolean]
}>()

const handleBackdropClick = (e: MouseEvent) => {
  if (e.target === e.currentTarget) {
    emit('update:open', false)
  }
}
</script>

