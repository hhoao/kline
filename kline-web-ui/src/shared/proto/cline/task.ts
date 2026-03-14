import { Settings } from "./state";

/** Request message for creating a new task */
export interface NewTaskRequest {
  text: string;
  images?: string[];
  files?: string[];
  taskSettings?: Settings | undefined;
}

/** Request message for toggling task favorite status */
export interface TaskFavoriteRequest {
  taskId: string;
  isFavorited: boolean;
}

/** Response for task details */
export interface TaskResponse {
  id: string;
  task: string;
  ts: number;
  isFavorited: boolean;
  size: number;
  totalCost: number;
  tokensIn: number;
  tokensOut: number;
  cacheWrites: number;
  cacheReads: number;
}

/** Request for getting task history with filtering */
export type GetTaskHistoryRequest = {
  favoritesOnly?: boolean;
  searchQuery?: string;
  sortBy?: string;
  currentWorkspaceOnly?: boolean;
}

/** Response for task history */
export interface TaskHistoryArray {
  tasks: TaskItem[];
  totalCount: number;
}

/** Task item details for history list */
export interface TaskItem {
  id: string;
  task: string;
  ts: number;
  isFavorited: boolean;
  size: number;
  totalCost: number;
  tokensIn: number;
  tokensOut: number;
  cacheWrites: number;
  cacheReads: number;
}

/** Request for ask response operation */
export interface AskResponseRequest {
  responseType: string;
  taskId: string;
  text?: string;
  images?: string[];
  files?: string[];
}

/** Request for executing a quick win task */
export interface ExecuteQuickWinRequest {
  command: string;
  title: string;
}

/** Results returned when deleting all task history */
export interface DeleteAllTaskHistoryCount {
  tasksDeleted: number;
}