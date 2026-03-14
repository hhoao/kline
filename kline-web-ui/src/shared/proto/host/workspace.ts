import { FileDiagnostics } from "../index.cline";

export interface GetWorkspacePathsRequest {
  /**
   * The unique ID for the workspace/project.
   * This is currently optional in vscode. It is required in other environments where cline is running at
   * the application level, and the user can open multiple projects.
   */
  id?: string | undefined;
}

export interface GetWorkspacePathsResponse {
  /** The unique ID for the workspace/project. */
  id?: string | undefined;
  paths: string[];
}

export interface SaveOpenDocumentIfDirtyRequest {
  filePath?: string | undefined;
}

export interface SaveOpenDocumentIfDirtyResponse {
  /** Returns true if the document was saved. */
  wasSaved?: boolean | undefined;
}

export interface GetDiagnosticsRequest {
}

export interface GetDiagnosticsResponse {
  fileDiagnostics: FileDiagnostics[];
}

/** Request for host-side workspace search (files/folders) used by mentions autocomplete */
export interface SearchWorkspaceItemsRequest {
  /** Search query string */
  query: string;
  /** Optional limit for results (default decided by host) */
  limit?: number | undefined;
  selectedType?: SearchWorkspaceItemsRequest_SearchItemType | undefined;
}

/** Optional selected type filter */
export enum SearchWorkspaceItemsRequest_SearchItemType {
  FILE = 0,
  FOLDER = 1,
  UNRECOGNIZED = -1,
}

export function searchWorkspaceItemsRequest_SearchItemTypeFromJSON(
  object: any,
): SearchWorkspaceItemsRequest_SearchItemType {
  switch (object) {
    case 0:
    case "FILE":
      return SearchWorkspaceItemsRequest_SearchItemType.FILE;
    case 1:
    case "FOLDER":
      return SearchWorkspaceItemsRequest_SearchItemType.FOLDER;
    case -1:
    case "UNRECOGNIZED":
    default:
      return SearchWorkspaceItemsRequest_SearchItemType.UNRECOGNIZED;
  }
}

export function searchWorkspaceItemsRequest_SearchItemTypeToJSON(
  object: SearchWorkspaceItemsRequest_SearchItemType,
): string {
  switch (object) {
    case SearchWorkspaceItemsRequest_SearchItemType.FILE:
      return "FILE";
    case SearchWorkspaceItemsRequest_SearchItemType.FOLDER:
      return "FOLDER";
    case SearchWorkspaceItemsRequest_SearchItemType.UNRECOGNIZED:
    default:
      return "UNRECOGNIZED";
  }
}

/** Response for host-side workspace search */
export interface SearchWorkspaceItemsResponse {
  items: SearchWorkspaceItemsResponse_SearchItem[];
}

export interface SearchWorkspaceItemsResponse_SearchItem {
  /** Workspace-relative path using platform separators */
  path: string;
  type: SearchWorkspaceItemsRequest_SearchItemType;
  /** Optional display label (e.g., basename) */
  label?: string | undefined;
}

export interface OpenProblemsPanelRequest {
}

export interface OpenProblemsPanelResponse {
}

export interface OpenInFileExplorerPanelRequest {
  path: string;
}

export interface OpenInFileExplorerPanelResponse {
}

export interface OpenClineSidebarPanelRequest {
}

export interface OpenClineSidebarPanelResponse {
}

export interface OpenTerminalRequest {
}

export interface OpenTerminalResponse {
}

/** Execute a command in the terminal */
export interface ExecuteCommandInTerminalRequest {
  /** The command to execute */
  command: string;
}

export interface ExecuteCommandInTerminalResponse {
  /** Whether the command was successfully sent to the terminal */
  success: boolean;
}