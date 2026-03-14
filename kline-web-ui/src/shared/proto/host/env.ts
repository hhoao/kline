
export enum Setting {
  /** UNSUPPORTED - This host does not support this setting. */
  UNSUPPORTED = 0,
  ENABLED = 1,
  DISABLED = 2,
  UNRECOGNIZED = -1,
}

export function settingFromJSON(object: any): Setting {
  switch (object) {
    case 0:
    case "UNSUPPORTED":
      return Setting.UNSUPPORTED;
    case 1:
    case "ENABLED":
      return Setting.ENABLED;
    case 2:
    case "DISABLED":
      return Setting.DISABLED;
    case -1:
    case "UNRECOGNIZED":
    default:
      return Setting.UNRECOGNIZED;
  }
}

export function settingToJSON(object: Setting): string {
  switch (object) {
    case Setting.UNSUPPORTED:
      return "UNSUPPORTED";
    case Setting.ENABLED:
      return "ENABLED";
    case Setting.DISABLED:
      return "DISABLED";
    case Setting.UNRECOGNIZED:
    default:
      return "UNRECOGNIZED";
  }
}

export interface GetHostVersionResponse {
  /** The name of the host platform, e.g VSCode, IntelliJ Ultimate Edition, etc. */
  platform?:
    | string
    | undefined;
  /** The version of the host platform, e.g. 1.103.0 for VSCode, or 2025.1.1.1 for JetBrains IDEs. */
  version?:
    | string
    | undefined;
  /**
   * The type of the cline host environment, e.g. 'VSCode Extension', 'Cline for JetBrains', 'CLI'
   * This is different from the platform because there are many JetBrains IDEs, but they all use the same
   * plugin.
   */
  clineType?:
    | string
    | undefined;
  /** The version of the cline host environment, e.g. 33.2.10 for extension, or 1.0.6 for JetBrains. */
  clineVersion?: string | undefined;
}

export interface GetTelemetrySettingsResponse {
  isEnabled: Setting;
}

export interface TelemetrySettingsEvent {
  isEnabled: Setting;
}