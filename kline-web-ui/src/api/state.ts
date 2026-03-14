/**
 * State Service API
 * 状态相关服务
 */
import { AutoApprovalSettingsRequest, UpdateSettingsRequest, Settings, Secrets, TelemetrySettingEnum } from '@/shared/proto/index.cline'
import { TelemetrySettingRequest } from '@/shared/proto/index.cline'
import type { GlobalState } from '@/shared/storage/state-keys'
import type { AutoApprovalSettings } from '@/shared/AutoApprovalSettings'
import { defHttp } from '@/utils/http/axios'

export const stateService = {
  async getLatestState(): Promise<string> {
    return await defHttp.get<string>({
      url: '/api/cline/state/latest',
      method: 'GET'
    })
  },

  async getAvailableTerminalProfiles(): Promise<string> {
    return await defHttp.get<string>({
      url: '/api/cline/state/terminal-profiles',
      method: 'GET'
    })
  },

  async getGlobalState(): Promise<GlobalState> {
    return await defHttp.get<GlobalState>({
      url: '/api/cline/state/global-state',
      method: 'GET'
    })
  },

  async updateGlobalState(globalState: Partial<GlobalState>): Promise<void> {
    return await defHttp.post<void>({
      url: '/api/cline/state/global-state',
      method: 'POST',
      data: globalState
    })
  },

  async getSecrets(): Promise<Secrets> {
    return await defHttp.get<Secrets>({
      url: '/api/cline/state/secrets',
      method: 'GET'
    })
  },

  async updateSecrets(secrets: Partial<Secrets>): Promise<void> {
    return await defHttp.post<void>({
      url: '/api/cline/state/secrets',
      method: 'POST',
      data: secrets
    })
  },

  async getSettings(): Promise<Settings> {
    return await defHttp.get<Settings>({
      url: '/api/cline/state/settings',
      method: 'GET'
    })
  },

  async updateSettings(settings: Partial<Settings>): Promise<void> {
    return await defHttp.post<void>({
      url: '/api/cline/state/settings',
      method: 'POST',
      data: settings
    })
  },


  async updateAutoApprovalSettings(request: AutoApprovalSettings | AutoApprovalSettingsRequest): Promise<void> {
    return await this.updateSettings({
      autoApprovalSettings: request as any
    })
  },

  async updateSettingsLegacy(request: UpdateSettingsRequest): Promise<void> {
    return await defHttp.post<void>({
      url: '/api/cline/state/update-settings',
      method: 'POST',
      data: request
    })
  },

  async updateTelemetrySetting(request: TelemetrySettingRequest): Promise<void> {
    const telemetrySettingValue = request.setting === TelemetrySettingEnum.ENABLED ? 'enabled' : 
                                   request.setting === TelemetrySettingEnum.DISABLED ? 'disabled' : 'unset'
    return await this.updateSettings({
      telemetrySetting: telemetrySettingValue
    })
  },

  async setWelcomeViewCompleted(request: boolean): Promise<void> {
    return await this.updateGlobalState({
      welcomeViewCompleted: request
    })
  },

  async updateInfoBannerVersion(request: number): Promise<void> {
    return await this.updateGlobalState({
      lastDismissedInfoBannerVersion: request
    })
  },

  async updateModelBannerVersion(request: number): Promise<void> {
    return await this.updateGlobalState({
      lastDismissedModelBannerVersion: request
    })
  }
}
