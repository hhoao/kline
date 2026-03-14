<template>
  <div class="fixed inset-0 flex flex-col bg-[var(--vscode-editor-background)]">
    <SettingsView :target-section="targetSection" @done="onDone" />
  </div>
</template>

<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import SettingsView from './SettingsView.vue'

const router = useRouter()
const route = useRoute()
const targetSection = (route.query.section as string) || undefined

function onDone() {
  router.push({ name: 'Chat' })
}
</script>
