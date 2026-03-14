import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import ChatView from '@/components/chat/ChatView.vue'
import HistoryView from '@/components/history/HistoryView.vue'
import SettingsPage from '@/components/settings/SettingsPage.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Chat',
    component: ChatView,
    meta: {
      title: 'AI Chat'
    }
  },
  {
    path: '/history',
    name: 'History',
    component: HistoryView,
    meta: {
      title: 'History'
    }
  },
  // {
  //   path: '/chat/new',
  //   name: 'NewChat',
  //   component: Chat,
  //   meta: {
  //     title: 'New Chat'
  //   }
  // },
  // {
  //   path: '/chat/:conversationId',
  //   name: 'ChatWithId',
  //   component: Chat,
  //   meta: {
  //     title: 'AI Chat'
  //   }
  // },
  {
    path: '/settings',
    name: 'Settings',
    component: SettingsPage,
    meta: {
      title: 'Settings'
    }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.VITE_PUBLIC_PATH),
  routes
})

router.beforeEach((to, _from, next) => {
  document.title = (to.meta.title as string) || 'AI Chat'
  next()
})

export default router

