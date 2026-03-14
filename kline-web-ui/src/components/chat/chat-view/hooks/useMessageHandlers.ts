import { computed, type Ref } from "vue";
import type { ClineMessage } from "@/shared/ExtensionMessage";
import { taskService } from "@/api/task";
import { slashService } from "@/api/slash";
import { useChatStateStore } from "@/stores/chatState";
import { useExtensionStateStore } from "@/stores/extensionState";
import type { ButtonActionType } from "../shared/buttonConfig";
import type { MessageHandlers, ScrollBehavior } from "../types/chatTypes";

/**
 * Vue composable for managing message handlers
 * Handles sending messages, button clicks, and task management
 */
export function useMessageHandlers(
  messages: Ref<ClineMessage[]> | ClineMessage[],
  scrollBehavior: ScrollBehavior
): MessageHandlers {
  const chatStateStore = useChatStateStore();
  const extensionStateStore = useExtensionStateStore();

  // Normalize messages to ref for consistent access
  const messagesRef = Array.isArray(messages)
    ? computed(() => messages)
    : messages;

  // Get extension state from chat store
  const extensionState = computed(() => extensionStateStore.extensionState);
  const backgroundCommandRunning = computed(
    () => extensionState.value?.backgroundCommandRunning || false
  );

  // Clear input state helper
  const clearInputState = () => {
    chatStateStore.setInputValue("");
    chatStateStore.setActiveQuote(null);
    chatStateStore.setSelectedImages([]);
    chatStateStore.setSelectedFiles([]);
  };

  // Start a new task
  const startNewTask = async () => {
    chatStateStore.setActiveQuote(null);
    const currentTaskId = extensionStateStore.conversationId;
    if (currentTaskId) {
      await taskService.clearTask(currentTaskId);
    }
  };

  // Handle sending a message
  const handleSendMessage = async (
    text: string,
    images: string[],
    files: string[]
  ) => {
    let messageToSend = text.trim();
    const hasContent = messageToSend || images.length > 0 || files.length > 0;

    // Prepend the active quote if it exists
    if (chatStateStore.activeQuote && hasContent) {
      const prefix = "[context] \n> ";
      const formattedQuote = chatStateStore.activeQuote;
      const suffix = "\n[/context] \n\n";
      messageToSend = `${prefix} ${formattedQuote} ${suffix} ${messageToSend}`;
    }

    if (hasContent) {
      console.log(
        "[ChatView] handleSendMessage - Sending message:",
        messageToSend
      );
      const msgs = messagesRef.value;
      const lastMsg = msgs.length > 0 ? msgs[msgs.length - 1] : undefined;
      const clineAsk = lastMsg?.type === "ask" ? lastMsg?.ask : undefined;
      const currentTaskId = extensionStateStore.conversationId!;

      if (msgs.length === 0) {
        const taskId = await taskService.newTask({
          text: messageToSend,
          images: images,
          files: files,
        });
        extensionStateStore.setConversationId(taskId);
      } else if (clineAsk) {
        switch (clineAsk) {
          case "followup":
          case "plan_mode_respond":
          case "tool":
          case "browser_action_launch":
          case "command":
          case "command_output":
          case "use_mcp_server":
          case "completion_result":
          case "resume_task":
          case "resume_completed_task":
          case "mistake_limit_reached":
          case "auto_approval_max_req_reached":
          case "api_req_failed":
          case "new_task":
          case "condense":
          case "report_bug":
            await taskService.askResponse({
              responseType: "messageResponse",
              taskId: currentTaskId,
              text: messageToSend,
              images: images,
              files: files,
            });
            break;
        }
      }
      chatStateStore.setInputValue("");
      chatStateStore.setActiveQuote(null);
      chatStateStore.setSendingDisabled(true);
      chatStateStore.setSelectedImages([]);
      chatStateStore.setSelectedFiles([]);
      chatStateStore.setEnableButtons(false);

      // Reset auto-scroll
      const disableRef = scrollBehavior.disableAutoScrollRef;
      disableRef.value = false;
    }
  };

  // Execute button action based on type
  const executeButtonAction = async (
    actionType: ButtonActionType,
    text?: string,
    images?: string[],
    files?: string[]
  ) => {
    const trimmedInput = text?.trim();
    const hasContent =
      trimmedInput ||
      (images && images.length > 0) ||
      (files && files.length > 0);
    const msgs = messagesRef.value;
    const lastMsg = msgs.length > 0 ? msgs[msgs.length - 1] : undefined;
    const clineAsk = lastMsg?.type === "ask" ? lastMsg?.ask : undefined;
    const lastMessage = lastMsg;
    const currentTaskId = extensionStateStore.conversationId!;

    switch (actionType) {
      case "retry":
        // For API retry (api_req_failed), always send simple approval without content
        await taskService.askResponse({
          responseType: "yesButtonClicked",
          taskId: currentTaskId,
        });
        clearInputState();
        break;

      case "approve":
        if (hasContent) {
          await taskService.askResponse({
            responseType: "yesButtonClicked",
            taskId: currentTaskId,
            text: trimmedInput,
            images: images,
            files: files,
          });
        } else {
          await taskService.askResponse({
            responseType: "yesButtonClicked",
            taskId: currentTaskId,
          });
        }
        clearInputState();
        break;

      case "reject":
        if (hasContent) {
          await taskService.askResponse({
            responseType: "noButtonClicked",
            taskId: currentTaskId,
            text: trimmedInput,
            images: images,
            files: files,
          });
        } else {
          await taskService.askResponse({
            responseType: "noButtonClicked",
            taskId: currentTaskId,
          });
        }
        clearInputState();
        break;

      case "proceed":
        if (hasContent) {
          await taskService.askResponse({
            responseType: "yesButtonClicked",
            taskId: currentTaskId,
            text: trimmedInput,
            images: images,
            files: files,
          });
        } else {
          await taskService.askResponse({
            responseType: "yesButtonClicked",
            taskId: currentTaskId,
          });
          clearInputState();
        }
        break;

      case "new_task":
        if (clineAsk === "new_task") {
          await taskService.newTask({
            text: lastMessage?.text || "",
            images: [],
            files: [],
          });
        } else {
          await startNewTask();
        }
        break;

      case "cancel":
        if (backgroundCommandRunning.value) {
          await taskService.cancelBackgroundCommand(currentTaskId);
        } else {
          await taskService.cancelTask(currentTaskId);
        }
        break;

      case "utility":
        switch (clineAsk) {
          case "condense":
            await slashService
              .condense(lastMessage?.text || "")
              .catch((err: any) => console.error(err));
            break;
          case "report_bug":
            await slashService
              .reportBug(lastMessage?.text || "")
              .catch((err: any) => console.error(err));
            break;
        }
        break;
    }

    // Reset auto-scroll
    const disableRef = scrollBehavior.disableAutoScrollRef;
    disableRef.value = false;
  };

  // Handle task close button click
  const handleTaskCloseButtonClick = () => {
    startNewTask();
  };

  return {
    handleSendMessage,
    executeButtonAction,
    handleTaskCloseButtonClick,
    startNewTask,
  };
}
