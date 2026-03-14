<template>
  <!-- diff_error and clineignore_error don't show header separately -->
  <template v-if="errorType === 'diff_error' || errorType === 'clineignore_error'">
    <component :is="renderErrorContent()" />
  </template>

  <!-- Other error types -->
  <template v-else>
    <component :is="renderErrorContent()" />
  </template>
</template>

<script setup lang="ts">
import { computed, h, type VNode } from 'vue'
import type { ClineMessage } from '@/shared/ExtensionMessage'
import { useExtensionStateStore } from "@/stores/extensionState"
import CreditLimitError from './CreditLimitError.vue'
import { accountService } from '@/api/account'
import { ClineError, ClineErrorType } from '@/api/error'

interface Props {
  message: ClineMessage
  errorType: 'error' | 'mistake_limit_reached' | 'auto_approval_max_req_reached' | 'diff_error' | 'clineignore_error'
  apiRequestFailedMessage?: string
  apiReqStreamingFailedMessage?: string
}

const props = defineProps<Props>()

const extensionState = computed(() => useExtensionStateStore().extensionState)
const clineUser = computed(() => {
  const state = extensionState.value as { clineUser?: unknown }
  return state?.clineUser
})

const renderErrorContent = (): VNode | null => {
  switch (props.errorType) {
    case 'error':
    case 'mistake_limit_reached':
    case 'auto_approval_max_req_reached':
      // Handle API request errors with special error parsing
      if (props.apiRequestFailedMessage || props.apiReqStreamingFailedMessage) {
        const clineError = ClineError.parse(props.apiRequestFailedMessage || props.apiReqStreamingFailedMessage)
        const clineErrorMessage = clineError?.message
        const requestId = clineError?._error?.request_id
        const isClineProvider = clineError?.providerId === 'cline'

        if (clineError) {
          if (clineError.isErrorType(ClineErrorType.Balance)) {
            const errorDetails = clineError._error?.details
            return h(CreditLimitError, {
              buyCreditsUrl: errorDetails?.buy_credits_url,
              currentBalance: errorDetails?.current_balance,
              message: errorDetails?.message,
              totalPromotions: errorDetails?.total_promotions,
              totalSpent: errorDetails?.total_spent,
            })
          }
        }

        if (clineError?.isErrorType(ClineErrorType.RateLimit)) {
          return h('p', { class: 'm-0 whitespace-pre-wrap text-[var(--vscode-errorForeground)] wrap-anywhere' }, [
            clineErrorMessage,
            requestId && h('div', `Request ID: ${requestId}`),
          ])
        }

        // For non-cline providers, we display the raw error message
        const errorMessageToDisplay = isClineProvider
          ? clineErrorMessage
          : props.apiReqStreamingFailedMessage || props.apiRequestFailedMessage

        // Default error display
        return h('p', { class: 'm-0 whitespace-pre-wrap text-[var(--vscode-errorForeground)] wrap-anywhere' }, [
          errorMessageToDisplay,
          requestId && h('div', `Request ID: ${requestId}`),
          clineErrorMessage?.toLowerCase()?.includes('powershell') &&
            h('div', [
              h('br'),
              h('br'),
              "It seems like you're having Windows PowerShell issues, please see this ",
              h('a', {
                class: 'underline text-inherit',
                href: 'https://github.com/cline/cline/wiki/TroubleShooting-%E2%80%90-%22PowerShell-is-not-recognized-as-an-internal-or-external-command%22',
              }, 'troubleshooting guide'),
              '.',
            ]),
          clineError?.isErrorType(ClineErrorType.Auth) &&
            h('div', [
              h('br'),
              h('br'),
              clineUser.value && !isClineProvider
                ? h('span', { class: 'mb-4 text-[var(--vscode-descriptionForeground)]' }, '(Click "Retry" below)')
                : h(
                    'button',
                    {
                      class: 'w-full mb-4 bg-[var(--vscode-button-background)] text-[var(--vscode-button-foreground)] border-0 rounded-[2px] px-3 py-1.5 cursor-pointer font-inherit',
                      onClick: handleSignIn,
                    },
                    'Sign in to Cline'
                  ),
            ]),
        ])
      }

      // Regular error message
      return h('p', { class: 'm-0 whitespace-pre-wrap text-[var(--vscode-errorForeground)] wrap-anywhere' }, [
        props.message.text,
      ])

    case 'diff_error':
      return h('div', {
        class: 'flex flex-col p-2 rounded text-xs opacity-80 bg-[var(--vscode-textBlockQuote-background)] text-[var(--vscode-foreground)]',
      }, [
        h('div', 'The model used search patterns that don\'t match anything in the file. Retrying...'),
      ])

    case 'clineignore_error':
      return h('div', {
        class: 'flex flex-col p-2 rounded text-xs bg-[var(--vscode-textBlockQuote-background)] text-[var(--vscode-foreground)] opacity-80',
      }, [
        h('div', [
          'Cline tried to access ',
          h('code', props.message.text),
          ' which is blocked by the ',
          h('code', '.clineignore'),
          ' file.',
        ]),
      ])

    default:
      return null
  }
}

const handleSignIn = () => {
  const state = extensionState.value as { handleSignIn?: () => void }
  const signInHandler = state?.handleSignIn
  if (signInHandler) {
    signInHandler()
  } else {
    // Fallback: try to call AccountServiceClient directly
      accountService.accountLoginClicked().catch((err: unknown) =>
        console.error('Failed to get login URL:', err)
      )
  }
}
</script>

