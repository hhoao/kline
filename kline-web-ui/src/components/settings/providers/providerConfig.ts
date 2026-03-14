import type { ApiProvider } from '@/shared/api'
import type { ModelInfo } from '@/shared/api'
import {
  deepSeekModels,
  deepSeekDefaultModelId,
  mistralModels,
  mistralDefaultModelId,
  nebiusModels,
  nebiusDefaultModelId,
  moonshotModels,
  moonshotDefaultModelId,
  minimaxModels,
  minimaxDefaultModelId,
} from '@/shared/api'

export interface GenericProviderConfig {
  providerId: ApiProvider
  label: string
  entrypointField?: keyof import('@/shared/api').ApiHandlerOptions
  entrypointOptions?: { value: string; label: string }[]
  models: Record<string, ModelInfo>
  defaultModelId: string
}

export const PROVIDER_CONFIGS: Record<string, GenericProviderConfig> = {
  mistral: {
    providerId: 'mistral',
    label: 'Mistral',
    models: mistralModels as Record<string, ModelInfo>,
    defaultModelId: mistralDefaultModelId,
  },
  deepseek: {
    providerId: 'deepseek',
    label: 'DeepSeek',
    models: deepSeekModels as Record<string, ModelInfo>,
    defaultModelId: deepSeekDefaultModelId,
  },
  nebius: {
    providerId: 'nebius',
    label: 'Nebius',
    models: nebiusModels as Record<string, ModelInfo>,
    defaultModelId: nebiusDefaultModelId,
  },
  moonshot: {
    providerId: 'moonshot',
    label: 'Moonshot (Kimi)',
    entrypointField: 'moonshotApiLine',
    entrypointOptions: [
      { value: 'moonshot', label: 'Moonshot' },
      { value: 'kimi', label: 'Kimi' },
    ],
    models: moonshotModels as Record<string, ModelInfo>,
    defaultModelId: moonshotDefaultModelId,
  },
  minimax: {
    providerId: 'minimax',
    label: 'Minimax',
    entrypointField: 'minimaxApiLine',
    entrypointOptions: [
      { value: 'minimax', label: 'Minimax' },
      { value: 'zhipu', label: 'Zhipu' },
    ],
    models: minimaxModels as Record<string, ModelInfo>,
    defaultModelId: minimaxDefaultModelId,
  },
}

export function getGenericProviderConfig(providerId: string): GenericProviderConfig | undefined {
  return PROVIDER_CONFIGS[providerId]
}

export function isGenericProvider(providerId: string): boolean {
  return providerId in PROVIDER_CONFIGS
}

export const GENERIC_PROVIDER_IDS = Object.keys(PROVIDER_CONFIGS) as ApiProvider[]
