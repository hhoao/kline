/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly DEV: string
  readonly PROD: string
  readonly BASE_URL: string
  readonly MODE: string
  // 应用配置
  readonly VITE_GLOB_APP_TITLE: string
  readonly VITE_GLOB_API_URL: string
  readonly VITE_GLOB_API_URL_PREFIX?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<Record<string, any>, Record<string, any>, any>
  export default component
}
