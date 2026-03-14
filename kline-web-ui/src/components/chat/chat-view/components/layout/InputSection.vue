<template>
  <div>
    <div class="mt-2.5 -mb-3">
      <QuotedMessagePreview />
    </div>

    <ChatTextArea
      :should-disable-files-and-images="shouldDisableFilesAndImages"
      :placeholder-text="placeholderText"
      :on-send-message="handleSendMessage"
      :on-select-files-and-images="selectFilesAndImages"
    />
  </div>
</template>

<script setup lang="ts">
import ChatTextArea from "@/components/chat/ChatTextArea.vue";
import QuotedMessagePreview from "@/components/chat/QuotedMessagePreview.vue";
import type { MessageHandlers, ScrollBehavior } from "../../types/chatTypes";
import { useChatStateStore } from "@/stores/chatState";

interface Props {
  messageHandlers: MessageHandlers;
  scrollBehavior: ScrollBehavior;
  placeholderText: string;
  shouldDisableFilesAndImages: boolean;
  selectFilesAndImages: () => Promise<void>;
}

const props = defineProps<Props>();

// Use store directly
const chatStateStore = useChatStateStore();

const handleSendMessage = () => {
  const hasContent = 
    (chatStateStore.inputValue.trim().length > 0) ||
    (chatStateStore.selectedImages.length > 0) ||
    (chatStateStore.selectedFiles.length > 0);
  
  if (hasContent) {
    props.messageHandlers.handleSendMessage(
      chatStateStore.inputValue as string,
      chatStateStore.selectedImages as string[],
      chatStateStore.selectedFiles as string[]
    );
  }
};
</script>
