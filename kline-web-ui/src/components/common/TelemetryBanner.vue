<template>
  <div class="flex relative flex-col gap-1 px-3 py-2 m-4 mb-1 text-sm bg-banner-background text-banner-foreground shrink-0">
    <h3 class="m-0">Help Improve Cline</h3>
    <i>(and access experimental features)</i>
    <p class="m-0">
      Cline collects error and usage data to help us fix bugs and improve the extension. No code, prompts, or personal
      information is ever sent.
    </p>
    <p class="m-0">
      <span>You can turn this setting off in </span>
      <span class="cursor-pointer text-link" @click="handleOpenSettings">
        settings
      </span>
      .
    </p>

    <!-- Close button -->
    <button
      aria-label="Close banner and enable telemetry"
      class="absolute top-3 right-3 p-0 bg-transparent border-0 opacity-70 cursor-pointer hover:opacity-100 text-inherit"
      @click="handleClose"
      type="button"
    >
      ✕
    </button>
  </div>
</template>

<script setup lang="ts">
import { stateService } from '@/api/state'
import { TelemetrySettingEnum } from '@/shared/proto/index.cline'

const telemetryRequest = {
  setting: TelemetrySettingEnum.ENABLED,
}

const handleClose = () => {
  stateService.updateTelemetrySetting(telemetryRequest).catch(console.error)
}

const handleOpenSettings = () => {
  handleClose()
  // Navigate to settings - you may need to implement this based on your routing
  // navigateToSettings()
}
</script>

