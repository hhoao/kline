import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './style.css'
import '@unocss/reset/normalize.css';
import '@unocss/reset/sanitize/assets.css';
import '@unocss/reset/sanitize/sanitize.css';
import '@unocss/reset/tailwind-compat.css';
import '@unocss/reset/tailwind.css';
import 'virtual:uno.css';
import { getTokenFromStorage, setCookie, saveTokenToStorage } from './utils/token'
import { initVSCodeTheme, detectSystemTheme } from './utils/vscTheme'

// 默认 satoken
const DEFAULT_SATOKEN = 'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJsb2dpblR5cGUiOiJsb2dpbiIsImxvZ2luSWQiOjEsImRldmljZVR5cGUiOiJERUYiLCJlZmYiOjE3NjI4Mjg5MDUwMTMsInJuU3RyIjoiSGpOaVFPOTU5REVaQkJGTDFTMU55b1lQMXF6eEZoRzAiLCJ1c2VybmFtZSI6InN5c2FkbWluIn0.U0V0kpvdJOSyXrbrH9tWs9ASFpmGhhNXWnweAL-Wzno'

// 应用启动时同步 token 到 cookie
function initTokenCookie(): void {
  let token = getTokenFromStorage('satoken')
  // 如果 localStorage 中没有 token，使用默认值并保存
  if (!token) {
    token = DEFAULT_SATOKEN
    // 保存默认 token 到 localStorage
    saveTokenToStorage(DEFAULT_SATOKEN, 'satoken')
  }

  // 同步到 cookie
  setCookie('satoken', token, 7)
}

// 初始化 token cookie
initTokenCookie()

// 初始化 VSCode 主题变量（根据系统主题自动检测）
const systemTheme = detectSystemTheme()
initVSCodeTheme(systemTheme)

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.mount('#app')

