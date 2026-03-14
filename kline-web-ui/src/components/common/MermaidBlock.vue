<template>
  <div class="relative my-2">
    <div
      v-if="isLoading"
      class="py-2 text-[var(--vscode-descriptionForeground)] italic text-[0.9em]"
    >
      Generating mermaid diagram...
    </div>
    <div
      class="absolute top-2 right-2 z-[1] opacity-60 transition-opacity duration-200 hover:opacity-100"
    >
      <button
        class="p-1 h-6 w-6 min-w-0 bg-[var(--vscode-button-secondaryBackground)] text-[var(--vscode-button-secondaryForeground)] border border-[var(--vscode-button-border)] rounded transition-all duration-200 flex items-center justify-center hover:bg-[var(--vscode-button-secondaryHoverBackground)] hover:-translate-y-[1px] hover:shadow-[0_2px_4px_rgba(0,0,0,0.1)] active:translate-y-0 active:shadow-none"
        aria-label="Copy Code"
        title="Copy Code"
        @click="handleCopyCode"
      >
        <span class="text-sm i-codicon:copy" />
      </button>
    </div>
    <div
      ref="containerRef"
      :class="[
        'min-h-[20px] transition-opacity duration-200 cursor-pointer flex justify-center',
        isLoading ? 'opacity-30' : 'opacity-100'
      ]"
      @click="handleClick"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import mermaid from 'mermaid'
import { fileService } from '@/api/file'

interface Props {
  code: string
}

const props = defineProps<Props>()

const MERMAID_THEME = {
  background: '#1e1e1e',
  textColor: '#ffffff',
  mainBkg: '#2d2d2d',
  nodeBorder: '#888888',
  lineColor: '#cccccc',
  primaryColor: '#3c3c3c',
  primaryTextColor: '#ffffff',
  primaryBorderColor: '#888888',
  secondaryColor: '#2d2d2d',
  tertiaryColor: '#454545',
  classText: '#ffffff',
  labelColor: '#ffffff',
  actorLineColor: '#cccccc',
  actorBkg: '#2d2d2d',
  actorBorder: '#888888',
  actorTextColor: '#ffffff',
  fillType0: '#2d2d2d',
  fillType1: '#3c3c3c',
  fillType2: '#454545',
}

onMounted(() => {
  mermaid.initialize({
    startOnLoad: false,
    securityLevel: 'loose',
    theme: 'dark',
    themeVariables: {
      ...MERMAID_THEME,
      fontSize: '16px',
      fontFamily: "var(--vscode-font-family, 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif)",
      noteTextColor: '#ffffff',
      noteBkgColor: '#454545',
      noteBorderColor: '#888888',
      critBorderColor: '#ff9580',
      critBkgColor: '#803d36',
      taskTextColor: '#ffffff',
      taskTextOutsideColor: '#ffffff',
      taskTextLightColor: '#ffffff',
      sectionBkgColor: '#2d2d2d',
      sectionBkgColor2: '#3c3c3c',
      altBackground: '#2d2d2d',
      linkColor: '#6cb6ff',
      compositeBackground: '#2d2d2d',
      compositeBorder: '#888888',
      titleColor: '#ffffff',
    },
  })
})

const containerRef = ref<HTMLDivElement | null>(null)
const isLoading = ref(false)
let debounceTimer: ReturnType<typeof setTimeout> | null = null

const renderMermaid = async () => {
  if (!containerRef.value) {
    return
  }

  isLoading.value = true
  containerRef.value.innerHTML = ''

  try {
    const isValid = await mermaid.parse(props.code, { suppressErrors: true })
    if (!isValid) {
      throw new Error('Invalid or incomplete Mermaid code')
    }

    const id = `mermaid-${Math.random().toString(36).substring(2)}`
    const { svg } = await mermaid.render(id, props.code)

    if (containerRef.value) {
      containerRef.value.innerHTML = svg
    }
  } catch (err) {
    console.warn('Mermaid parse/render failed:', err)
    if (containerRef.value) {
      containerRef.value.innerHTML = props.code.replace(/</g, '&lt;').replace(/>/g, '&gt;')
    }
  } finally {
    isLoading.value = false
  }
}

watch(() => props.code, () => {
  isLoading.value = true
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }
  debounceTimer = setTimeout(() => {
    renderMermaid()
  }, 500)
}, { immediate: true })

const handleClick = async () => {
  if (!containerRef.value) {
    return
  }
  const svgEl = containerRef.value.querySelector('svg')
  if (!svgEl) {
    return
  }

  try {
    const pngDataUrl = await svgToPng(svgEl)
    fileService.openImage(pngDataUrl).catch((err: any) =>
      console.error('Failed to open image:', err)
    )
  } catch (err) {
    console.error('Error converting SVG to PNG:', err)
  }
}

const handleCopyCode = async () => {
  try {
    await navigator.clipboard.writeText(props.code)
  } catch (err) {
    console.error('Copy failed', err)
  }
}

async function svgToPng(svgEl: SVGElement): Promise<string> {
  console.log('svgToPng function called')
  const svgClone = svgEl.cloneNode(true) as SVGElement

  const viewBox = svgClone.getAttribute('viewBox')?.split(' ').map(Number) || []
  const originalWidth = viewBox[2] || svgClone.clientWidth
  const originalHeight = viewBox[3] || svgClone.clientHeight

  const editorWidth = 3600
  const scale = editorWidth / originalWidth
  const scaledHeight = originalHeight * scale

  svgClone.setAttribute('width', `${editorWidth}`)
  svgClone.setAttribute('height', `${scaledHeight}`)

  const serializer = new XMLSerializer()
  const svgString = serializer.serializeToString(svgClone)
  const encoder = new TextEncoder()
  const bytes = encoder.encode(svgString)
  const base64 = btoa(Array.from(bytes, (byte) => String.fromCharCode(byte)).join(''))
  const svgDataUrl = `data:image/svg+xml;base64,${base64}`

  return new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => {
      const canvas = document.createElement('canvas')
      canvas.width = editorWidth
      canvas.height = scaledHeight

      const ctx = canvas.getContext('2d')
      if (!ctx) {
        return reject('Canvas context not available')
      }

      ctx.fillStyle = MERMAID_THEME.background
      ctx.fillRect(0, 0, canvas.width, canvas.height)

      ctx.imageSmoothingEnabled = true
      ctx.imageSmoothingQuality = 'high'

      ctx.drawImage(img, 0, 0, editorWidth, scaledHeight)
      resolve(canvas.toDataURL('image/png', 1.0))
    }
    img.onerror = reject
    img.src = svgDataUrl
  })
}
</script>

