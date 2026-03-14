import { stateService } from '@/api/state'
import type { Settings, UpdateSettingsRequest } from '@/shared/proto/index.cline'

type SettingsKey = keyof Settings | keyof UpdateSettingsRequest

function convertValue(field: string, value: unknown): unknown {
  if (field === 'openaiReasoningEffort' && typeof value === 'string') {
    const map = { minimal: 'minimal', low: 'low', medium: 'medium', high: 'high' }
    return map[value as keyof typeof map] ?? value
  }
  if (field === 'mcpDisplayMode' && typeof value === 'string') {
    const map = { rich: 'rich', plain: 'plain', markdown: 'markdown' }
    return map[value as keyof typeof map] ?? value
  }
  return value
}

export function updateSetting<K extends SettingsKey>(field: K, value: unknown): void {
  const payload = { [field]: convertValue(field as string, value) } as Partial<Record<K, unknown>>
  stateService.updateSettings(payload as any).catch((e) => {
    console.error(`Failed to update setting ${String(field)}:`, e)
  })
}
