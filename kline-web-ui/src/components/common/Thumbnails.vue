<template>
  <div
    ref="containerRef"
    :class="['flex flex-wrap', props.className]"
    :style="{
      gap: '5px',
      rowGap: '3px',
      ...props.style,
    }"
  >
    <div
      v-for="(image, index) in images"
      :key="`image-${index}`"
      class="relative"
      @mouseenter="hoveredIndex = `image-${index}`"
      @mouseleave="hoveredIndex = null"
    >
      <img
        :alt="`Thumbnail image-${index + 1}`"
        :src="image"
        class="w-[34px] h-[34px] object-cover rounded cursor-pointer"
        @click="handleImageClick(image)"
      />
      <div
        v-if="isDeletableImages && hoveredIndex === `image-${index}`"
        class="absolute -top-1 -right-1 w-[13px] h-[13px] rounded-full bg-[var(--vscode-badge-background)] flex justify-center items-center cursor-pointer"
        @click.stop="handleDeleteImages(index)"
      >
        <span
          class="i-codicon:close"
          style="color: var(--vscode-foreground); font-size: 10px; font-weight: bold"
        />
      </div>
    </div>

    <div
      v-for="(filePath, index) in files"
      :key="`file-${index}`"
      class="relative"
      @mouseenter="hoveredIndex = `file-${index}`"
      @mouseleave="hoveredIndex = null"
    >
      <div
        class="w-[34px] h-[34px] rounded cursor-pointer bg-[var(--vscode-editor-background)] border border-[var(--vscode-input-border)] flex flex-col items-center justify-center"
        @click="handleFileClick(filePath)"
      >
        <span
          class="i-codicon:file"
          style="font-size: 16px; color: var(--vscode-foreground)"
        />
        <span
          class="text-[7px] mt-[1px] overflow-hidden text-ellipsis max-w-[90%] whitespace-nowrap text-center"
          :title="getFileName(filePath)"
        >
          {{ getFileName(filePath) }}
        </span>
      </div>
      <div
        v-if="isDeletableFiles && hoveredIndex === `file-${index}`"
        class="absolute -top-1 -right-1 w-[13px] h-[13px] rounded-full bg-[var(--vscode-badge-background)] flex justify-center items-center cursor-pointer"
        @click.stop="handleDeleteFiles(index)"
      >
        <span
          class="i-codicon:close"
          style="color: var(--vscode-foreground); font-size: 10px; font-weight: bold"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { fileService } from '@/api/file'

interface Props {
  images: string[]
  files: string[]
  style?: Record<string, any>
  setImages?: (value: string[]) => void
  setFiles?: (value: string[]) => void
  onHeightChange?: (height: number) => void
  className?: string
}

const props = defineProps<Props>()

const hoveredIndex = ref<string | null>(null)
const containerRef = ref<HTMLDivElement | null>(null)
const viewportWidth = ref(window.innerWidth)

const isDeletableImages = computed(() => props.setImages !== undefined)
const isDeletableFiles = computed(() => props.setFiles !== undefined)

const getFileName = (filePath: string) => {
  return filePath.split(/[\\/]/).pop() || filePath
}

const handleDeleteImages = (index: number) => {
  if (props.setImages) {
    props.setImages(props.images.filter((_, i) => i !== index))
  }
}

const handleDeleteFiles = (index: number) => {
  if (props.setFiles) {
    props.setFiles(props.files.filter((_, i) => i !== index))
  }
}

const handleImageClick = (image: string) => {
  fileService.openImage(image).catch((err: any) =>
    console.error('Failed to open image:', err)
  )
}

const handleFileClick = (filePath: string) => {
  fileService.openFile(filePath).catch((err: any) =>
    console.error('Failed to open file:', err)
  )
}

const handleResize = () => {
  viewportWidth.value = window.innerWidth
}

const updateHeight = () => {
  if (containerRef.value && props.onHeightChange) {
    let height = containerRef.value.clientHeight
    if (!height) {
      height = containerRef.value.getBoundingClientRect().height
    }
    props.onHeightChange(height)
  }
  hoveredIndex.value = null
}

watch([() => props.images, () => props.files, viewportWidth], () => {
  updateHeight()
}, { immediate: true })

onMounted(() => {
  window.addEventListener('resize', handleResize)
  updateHeight()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

