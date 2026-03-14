import { h, type VNode } from 'vue'
import ProgressIndicator from '../ProgressIndicator.vue'
import { ClineError, ClineErrorType } from '@/api/error'

interface ErrorBlockTitleProps {
  cost?: number
  apiReqCancelReason?: string
  apiRequestFailedMessage?: string
  retryStatus?: {
    attempt: number
    maxAttempts: number
    delaySec?: number
    errorSnippet?: string
  }
}

export const getErrorBlockTitle = ({
  cost,
  apiReqCancelReason,
  apiRequestFailedMessage,
  retryStatus,
}: ErrorBlockTitleProps): [VNode | null, VNode | null] => {
  const getIconSpan = (iconName: string, colorClass: string) => {
    return h('div', { class: 'w-4 h-4 flex items-center justify-center' }, [
      h('span', {
        class: `i-codicon:${iconName} text-base -mb-0.5 ${colorClass}`,
      }),
    ])
  }

  const icon =
    apiReqCancelReason != null
      ? apiReqCancelReason === 'user_cancelled'
        ? getIconSpan('error', 'text-[var(--vscode-descriptionForeground)]')
        : getIconSpan('error', 'text-[var(--vscode-errorForeground)]')
      : cost != null
        ? getIconSpan('check', 'text-[var(--vscode-charts-green)]')
        : apiRequestFailedMessage
          ? getIconSpan('error', 'text-[var(--vscode-errorForeground)]')
          : h(ProgressIndicator)

  const title = (() => {
    // Default loading state
    const details: { title: string; classNames: string[] } = {
      title: 'API Request...',
      classNames: ['font-bold'],
    }
    // Handle cancellation states first
    if (apiReqCancelReason === 'user_cancelled') {
      details.title = 'API Request Cancelled'
      details.classNames.push('text-[var(--vscode-foreground)]')
    } else if (apiReqCancelReason != null) {
      details.title = 'API Request Failed'
      details.classNames.push('text-[var(--vscode-errorForeground)]')
    } else if (cost != null) {
      // Handle completed request
      details.title = 'API Request'
      details.classNames.push('text-[var(--vscode-foreground)]')
    } else if (apiRequestFailedMessage) {
      // Handle failed request
      const clineError = ClineError.parse(apiRequestFailedMessage)
      const titleText = clineError?.isErrorType(ClineErrorType.Balance)
        ? 'Credit Limit Reached'
        : 'API Request Failed'
      details.title = titleText
      details.classNames.push('font-bold text-[var(--vscode-errorForeground)]')
    } else if (retryStatus) {
      // Handle retry state
      details.title = 'API Request'
      details.classNames.push('text-[var(--vscode-foreground)]')
    }

    return h('span', { class: details.classNames.join(' ') }, details.title)
  })()

  return [icon, title]
}


