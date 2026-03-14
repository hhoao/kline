import type { ModelInfo } from '@/shared/api'

export function formatPrice(price: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(price)
}

export function formatTokenPrice(price: number): string {
  return `${formatPrice(price)}/million tokens`
}

export function hasThinkingBudget(modelInfo: ModelInfo): boolean {
  return !!modelInfo.thinkingConfig
}

export function supportsImages(modelInfo: ModelInfo): boolean {
  return !!modelInfo.supportsImages
}

export function supportsBrowserUse(modelInfo: ModelInfo): boolean {
  return !!modelInfo.supportsImages
}

export function supportsPromptCache(modelInfo: ModelInfo): boolean {
  return !!modelInfo.supportsPromptCache
}

export function formatTokenLimit(limit: number): string {
  return limit.toLocaleString()
}
