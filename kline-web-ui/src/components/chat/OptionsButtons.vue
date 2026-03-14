<template>
  <div
    v-if="options && options.length > 0"
    class="flex flex-col gap-2 pt-[15px]"
  >
    <button
      v-for="(option, index) in options"
      :key="index"
      :id="`options-button-${index}`"
      :class="[
        'options-button px-3 py-2 text-left text-xs border rounded-[2px] transition-colors',
        {
          'bg-[var(--vscode-focusBorder)] text-white': option === selected,
          'bg-[var(--vscode-editor-background)] text-[var(--vscode-input-foreground)]':
            option !== selected,
          'cursor-default': hasSelected || !isActive,
          'cursor-pointer hover:bg-[var(--vscode-focusBorder)] hover:text-white':
            !hasSelected && isActive,
        },
      ]"
      :disabled="hasSelected || !isActive"
      :style="{
        borderColor: 'var(--vscode-editorGroup-border)',
      }"
      @click="handleOptionClick(option)"
    >
      <span class="ph-no-capture">{{ option }}</span>
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { taskService } from "@/api/task";
import { useExtensionStateStore } from "@/stores/extensionState";

interface Props {
  options?: string[];
  selected?: string;
  isActive?: boolean;
  inputValue?: string;
}

const props = withDefaults(defineProps<Props>(), {
  isActive: false,
});

const extensionStateStore = useExtensionStateStore();

const hasSelected = computed(() => {
  return (
    props.selected !== undefined && props.options?.includes(props.selected)
  );
});

const handleOptionClick = async (option: string) => {
  if (hasSelected.value || !props.isActive) {
    return;
  }

  try {
    await taskService.askResponse(
      {
        responseType: "messageResponse",
        taskId: extensionStateStore.conversationId || "",
        text:
          option + (props.inputValue ? `: ${props.inputValue?.trim()}` : ""),
        images: [],
      }
    );
  } catch (error) {
    console.error("Error sending option response:", error);
  }
};
</script>
