
export interface BrowserConnectionInfo {
  isConnected: boolean;
  isRemote: boolean;
  host?: string | undefined;
}

export interface BrowserConnection {
  success: boolean;
  message: string;
  endpoint?: string | undefined;
}

export interface ChromePath {
  path: string;
  isBundled: boolean;
}

export interface Viewport {
  width: number;
  height: number;
}

export interface BrowserSettings {
  viewport: Viewport | undefined;
  remoteBrowserHost?: string | undefined;
  remoteBrowserEnabled?: boolean | undefined;
  chromeExecutablePath?: string | undefined;
  disableToolUse?: boolean | undefined;
  customArgs?: string | undefined;
}

export interface UpdateBrowserSettingsRequest {
  viewport: Viewport | undefined;
  remoteBrowserHost?: string | undefined;
  remoteBrowserEnabled?: boolean | undefined;
  chromeExecutablePath?: string | undefined;
  disableToolUse?: boolean | undefined;
  customArgs?: string | undefined;
}