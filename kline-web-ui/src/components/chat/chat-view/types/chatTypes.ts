/**
 * Shared types and interfaces for the chat view components
 */

import { ClineAsk, ClineMessage } from "@/shared/ExtensionMessage";
import type { Ref } from "vue";
import { ButtonActionType } from "../shared/buttonConfig";

/**
 * Main ChatView component props
 */
export interface ChatViewProps {
  isHidden: boolean;
  showAnnouncement: boolean;
  hideAnnouncement: () => void;
  showHistoryView: () => void;
}

/**
 * Chat state interface
 * Compatible with both React and Vue implementations
 */
export interface ChatState {
  // State values
  inputValue: string;
  setInputValue: (value: string) => void;
  activeQuote: string | null;
  setActiveQuote: (value: string | null) => void;
  isTextAreaFocused: boolean;
  setIsTextAreaFocused: (value: boolean) => void;
  selectedImages: string[];
  setSelectedImages: (value: string[]) => void;
  selectedFiles: string[];
  setSelectedFiles: (value: string[]) => void;
  sendingDisabled: boolean;
  setSendingDisabled: (value: boolean) => void;
  enableButtons: boolean;
  setEnableButtons: (value: boolean) => void;
  primaryButtonText: string | undefined;
  setPrimaryButtonText: (value: string | undefined) => void;
  secondaryButtonText: string | undefined;
  setSecondaryButtonText: (value: string | undefined) => void;
  expandedRows: Record<number, boolean>;
  setExpandedRows: (value: Record<number, boolean>) => void;

  // Refs - Vue uses Ref<T | null>, React uses RefObject<T>
  textAreaRef: Ref<HTMLTextAreaElement | null>;

  // Derived values
  lastMessage: ClineMessage | undefined;
  secondLastMessage: ClineMessage | undefined;
  clineAsk: ClineAsk | undefined;
  task: ClineMessage | undefined;

  // Handlers
  handleFocusChange: (isFocused: boolean) => void;
  clearExpandedRows: () => void;
  resetState: () => void;

  // Scroll-related state (will be moved to scroll hook)
  showScrollToBottom?: boolean;
  isAtBottom?: boolean;
  pendingScrollToMessage?: number | null;
}

/**
 * Message handlers interface
 */
export interface MessageHandlers {
  executeButtonAction: (
    action: ButtonActionType,
    text?: string,
    images?: string[],
    files?: string[]
  ) => Promise<void>;
  handleSendMessage: (
    text: string,
    images: string[],
    files: string[]
  ) => Promise<void>;
  handleTaskCloseButtonClick: () => void;
  startNewTask: () => Promise<void>;
}

/**
 * Scroll behavior interface
 * Compatible with both React and Vue implementations
 */
export interface ScrollBehavior {
  virtuosoRef: Ref<any>;
  scrollContainerRef: Ref<HTMLDivElement | null>;
  disableAutoScrollRef: Ref<boolean>;
  scrollToBottomSmooth: () => void;
  scrollToBottomAuto: () => void;
  scrollToMessage: (messageIndex: number) => void;
  toggleRowExpansion: (ts: number) => void;
  handleRowHeightChange: (isTaller: boolean) => void;
  showScrollToBottom: boolean | Ref<boolean>;
  setShowScrollToBottom: (
    value: boolean | ((prev: boolean) => boolean)
  ) => void;
  isAtBottom: boolean | Ref<boolean>;
  setIsAtBottom: (value: boolean | ((prev: boolean) => boolean)) => void;
  pendingScrollToMessage: number | null | Ref<number | null>;
  setPendingScrollToMessage: (
    value: number | null | ((prev: number | null) => number | null)
  ) => void;
}

/**
 * Button state interface
 */
export interface ButtonState {
  enableButtons: boolean;
  primaryButtonText: string | undefined;
  secondaryButtonText: string | undefined;
}

/**
 * Input state interface
 */
export interface InputState {
  inputValue: string;
  selectedImages: string[];
  selectedFiles: string[];
  activeQuote: string | null;
  isTextAreaFocused: boolean;
}

/**
 * Task section props
 */
export interface TaskSectionProps {
  task: ClineMessage;
  messages: ClineMessage[];
  scrollBehavior: ScrollBehavior;
  buttonState: ButtonState;
  messageHandlers: MessageHandlers;
  chatState: ChatState;
  apiMetrics: {
    totalTokensIn: number;
    totalTokensOut: number;
    totalCacheWrites?: number;
    totalCacheReads?: number;
    totalCost: number;
  };
  lastApiReqTotalTokens?: number;
  selectedModelInfo: {
    supportsPromptCache: boolean;
    supportsImages: boolean;
  };
  isStreaming: boolean;
  clineAsk?: ClineAsk;
  modifiedMessages: ClineMessage[];
}

/**
 * Welcome section props
 */
export interface WelcomeSectionProps {
  showAnnouncement: boolean;
  hideAnnouncement: () => void;
  showHistoryView: () => void;
  telemetrySetting: string;
  version: string;
  taskHistory: any[];
  shouldShowQuickWins: boolean;
}

/**
 * Input section props
 */
export interface InputSectionProps {
  chatState: ChatState;
  messageHandlers: MessageHandlers;
  textAreaRef: Ref<HTMLTextAreaElement | null>;
  onFocusChange: (isFocused: boolean) => void;
  onInputChange: (value: string) => void;
  onQuoteChange: (quote: string | null) => void;
  onImagesChange: (images: string[]) => void;
  onFilesChange: (files: string[]) => void;
  placeholderText: string;
  shouldDisableFilesAndImages: boolean;
  selectFilesAndImages: () => Promise<void>;
}
