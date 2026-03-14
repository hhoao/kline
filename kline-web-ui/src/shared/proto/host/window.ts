export enum ShowMessageType {
  ERROR = 0,
  INFORMATION = 1,
  WARNING = 2,
  UNRECOGNIZED = -1,
}

export function showMessageTypeFromJSON(object: any): ShowMessageType {
  switch (object) {
    case 0:
    case "ERROR":
      return ShowMessageType.ERROR;
    case 1:
    case "INFORMATION":
      return ShowMessageType.INFORMATION;
    case 2:
    case "WARNING":
      return ShowMessageType.WARNING;
    case -1:
    case "UNRECOGNIZED":
    default:
      return ShowMessageType.UNRECOGNIZED;
  }
}

export function showMessageTypeToJSON(object: ShowMessageType): string {
  switch (object) {
    case ShowMessageType.ERROR:
      return "ERROR";
    case ShowMessageType.INFORMATION:
      return "INFORMATION";
    case ShowMessageType.WARNING:
      return "WARNING";
    case ShowMessageType.UNRECOGNIZED:
    default:
      return "UNRECOGNIZED";
  }
}

export interface ShowTextDocumentRequest {
  path: string;
  options?: ShowTextDocumentOptions | undefined;
}

/** See https://code.visualstudio.com/api/references/vscode-api#TextDocumentShowOptions */
export interface ShowTextDocumentOptions {
  preview?: boolean | undefined;
  preserveFocus?: boolean | undefined;
  viewColumn?: number | undefined;
}

export interface TextEditorInfo {
  documentPath: string;
  viewColumn?: number | undefined;
  isActive: boolean;
}

export interface ShowOpenDialogueRequest {
  canSelectMany?: boolean | undefined;
  openLabel?: string | undefined;
  filters?: ShowOpenDialogueFilterOption | undefined;
}

export interface ShowOpenDialogueFilterOption {
  files: string[];
}

export interface SelectedResources {
  paths: string[];
}

export interface ShowMessageRequest {
  type: ShowMessageType;
  message: string;
  options?: ShowMessageRequestOptions | undefined;
}

export interface ShowMessageRequestOptions {
  items: string[];
  modal?: boolean | undefined;
  detail?: string | undefined;
}

export interface SelectedResponse {
  selectedOption?: string | undefined;
}

export interface ShowSaveDialogRequest {
  options?: ShowSaveDialogOptions | undefined;
}

export interface ShowSaveDialogOptions {
  defaultPath?:
    | string
    | undefined;
  /**
   * A map of file types to extensions, e.g
   * "Text Files": { "extensions": ["txt", "md"] }
   */
  filters: { [key: string]: FileExtensionList };
}

export interface ShowSaveDialogOptions_FiltersEntry {
  key: string;
  value: FileExtensionList | undefined;
}

export interface FileExtensionList {
  /** A list of file extension (without the dot). */
  extensions: string[];
}

export interface ShowSaveDialogResponse {
  /** If the user cancelled the dialog, this will be empty. */
  selectedPath?: string | undefined;
}

export interface ShowInputBoxRequest {
  title: string;
  prompt?: string | undefined;
  value?: string | undefined;
}

export interface ShowInputBoxResponse {
  response?: string | undefined;
}

export interface OpenFileRequest {
  filePath: string;
}

export interface OpenFileResponse {
}

export interface OpenSettingsRequest {
  /**
   * Optional query to focus a particular settings section/key.
   * This value is host-specific. In VS Code, it is passed directly as the
   * Settings search query to the "workbench.action.openSettings" command.
   * Examples (VS Code, see - https://code.visualstudio.com/docs/getstarted/settings#settings-editor-filters.):
   * - "telemetry.telemetryLevel" → focuses the Telemetry Level setting
   * - "@id:telemetry.telemetryLevel" → navigates by exact setting id
   * - "@modified", "@ext:publisher.extension"
   * - Plain keywords/categories
   * If not provided the host opens the settings UI without specific focus.
   */
  query?: string | undefined;
}

export interface OpenSettingsResponse {
}

export interface GetOpenTabsRequest {
}

export interface GetOpenTabsResponse {
  paths: string[];
}

export interface GetVisibleTabsRequest {
}

export interface GetVisibleTabsResponse {
  paths: string[];
}

export interface GetActiveEditorRequest {
}

export interface GetActiveEditorResponse {
  filePath?: string | undefined;
}