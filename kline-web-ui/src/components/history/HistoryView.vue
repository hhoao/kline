<template>
  <div class="flex overflow-hidden fixed inset-0 flex-col">
    <div class="flex justify-between items-center px-5 py-2.5">
      <h3 class="m-0" :style="{ color: environmentColor }">History</h3>
      <button
        class="bg-[var(--vscode-button-background)] text-[var(--vscode-button-foreground)] border border-[var(--vscode-button-border)] px-4 py-2 rounded cursor-pointer transition-colors hover:bg-[var(--vscode-button-hoverBackground)]"
        @click="handleDone"
      >
        Done
      </button>
    </div>
    <div class="px-4 py-1.5">
      <div class="flex flex-col gap-1.5">
        <div class="relative w-full">
          <input
            v-model="searchQuery"
            type="text"
            placeholder="Fuzzy search history..."
            class="w-full px-3 py-2 pr-8 bg-[var(--vscode-input-background)] text-[var(--vscode-input-foreground)] border border-[var(--vscode-input-border)] rounded focus:outline-none focus:border-[var(--vscode-focusBorder)]"
            @input="handleSearchInput"
          />
          <span
            class="i-codicon:search absolute left-2.5 top-1/2 -translate-y-1/2 text-[13px] opacity-80 pointer-events-none"
          />
          <button
            v-if="searchQuery"
            aria-label="Clear search"
            class="flex absolute right-2 top-1/2 justify-center items-center h-full -translate-y-1/2 cursor-pointer"
            @click="searchQuery = ''"
          >
            <span class="i-codicon:close" />
          </button>
        </div>
        <div class="flex flex-wrap">
          <label
            v-for="option in sortOptions"
            :key="option.value"
            class="flex items-center mr-2.5 cursor-pointer"
          >
            <input
              v-model="sortOption"
              type="radio"
              :value="option.value"
              :disabled="option.disabled"
              class="mr-1"
              @change="handleSortChange"
            />
            <span :class="{ 'opacity-50': option.disabled }">{{
              option.label
            }}</span>
          </label>
          <CustomFilterRadio
            :checked="showCurrentWorkspaceOnly"
            icon="workspace"
            label="Workspace"
            @change="showCurrentWorkspaceOnly = !showCurrentWorkspaceOnly"
          />
          <CustomFilterRadio
            :checked="showFavoritesOnly"
            icon="star-full"
            label="Favorites"
            @change="showFavoritesOnly = !showFavoritesOnly"
          />
        </div>

        <div class="flex gap-2.5 justify-end">
          <button
            class="bg-[var(--vscode-button-background)] text-[var(--vscode-button-foreground)] border border-[var(--vscode-button-border)] px-4 py-2 rounded cursor-pointer transition-colors hover:bg-[var(--vscode-button-hoverBackground)]"
            @click="handleBatchHistorySelect(true)"
          >
            Select All
          </button>
          <button
            class="bg-[var(--vscode-button-background)] text-[var(--vscode-button-foreground)] border border-[var(--vscode-button-border)] px-4 py-2 rounded cursor-pointer transition-colors hover:bg-[var(--vscode-button-hoverBackground)]"
            @click="handleBatchHistorySelect(false)"
          >
            Select None
          </button>
        </div>
      </div>
    </div>
    <div class="overflow-y-auto flex-grow m-0">
      <div
        v-for="(item, index) in taskHistorySearchResults"
        :key="item.id"
        class="flex cursor-pointer history-item"
        :class="{
          'border-b border-[var(--vscode-panel-border)]':
            index < taskHistory.length - 1,
        }"
      >
        <input
          :checked="selectedItems.includes(item.id)"
          type="checkbox"
          class="pr-1 pl-3 py-auto"
          @click.stop="
            handleHistorySelect(
              item.id,
              ($event.target as HTMLInputElement).checked
            )
          "
        />
        <div
          class="flex relative flex-col flex-grow gap-2 p-3 pl-4"
          @click="handleShowTaskWithId(item.id)"
        >
          <div class="flex justify-between items-center">
            <span
              class="text-[var(--vscode-descriptionForeground)] font-medium text-[0.85em] uppercase"
            >
              {{ formatDate(item.ts) }}
            </span>
            <div class="flex gap-1">
              <button
                v-if="!(pendingFavoriteToggles[item.id] ?? item.isFavorited)"
                aria-label="Delete"
                class="p-0 bg-transparent border-0 opacity-0 cursor-pointer pointer-events-none delete-button history-item:hover:opacity-100 history-item:hover:pointer-events-auto"
                @click.stop="handleDeleteHistoryItem(item.id)"
              >
                <div class="flex items-center gap-1 text-[11px]">
                  <span class="i-codicon:trash"></span>
                  {{ formatSize(item.size) }}
                </div>
              </button>
              <button
                :aria-label="
                  item.isFavorited
                    ? 'Remove from favorites'
                    : 'Add to favorites'
                "
                class="p-0 bg-transparent border-0 cursor-pointer"
                @click.stop="toggleFavorite(item.id, item.isFavorited || false)"
              >
                <span
                  :class="[
                    'codicon',
                    pendingFavoriteToggles[item.id] !== undefined
                      ? pendingFavoriteToggles[item.id]
                        ? 'i-codicon:star-full'
                        : 'i-codicon:star-empty'
                      : item.isFavorited
                      ? 'i-codicon:star-full'
                      : 'i-codicon:star-empty',
                  ]"
                  :style="{
                    color:
                      pendingFavoriteToggles[item.id] ?? item.isFavorited
                        ? 'var(--vscode-button-background)'
                        : 'inherit',
                    opacity:
                      pendingFavoriteToggles[item.id] ?? item.isFavorited
                        ? 1
                        : 0.7,
                    display:
                      pendingFavoriteToggles[item.id] ?? item.isFavorited
                        ? 'block'
                        : undefined,
                  }"
                />
              </button>
            </div>
          </div>

          <div class="relative mb-2">
            <div
              class="text-[var(--vscode-foreground)] line-clamp-3 whitespace-pre-wrap break-words overflow-wrap-anywhere"
              style="font-size: var(--vscode-font-size)"
              v-html="item.task"
            />
          </div>
          <div class="flex flex-col gap-1">
            <div class="flex justify-between items-center">
              <div class="flex flex-wrap gap-1 items-center">
                <span
                  class="font-medium text-[var(--vscode-descriptionForeground)]"
                  >Tokens:</span
                >
                <span
                  class="flex items-center gap-1 text-[var(--vscode-descriptionForeground)]"
                >
                  <i
                    class="-mb-0.5 text-xs font-bold i-codicon:arrow-up"
                  />
                  {{ formatLargeNumber(item.tokensIn || 0) }}
                </span>
                <span
                  class="flex items-center gap-1 text-[var(--vscode-descriptionForeground)]"
                >
                  <i
                    class="-mb-0.5 text-xs font-bold i-codicon:arrow-down"
                  />
                  {{ formatLargeNumber(item.tokensOut || 0) }}
                </span>
              </div>
              <button
                v-if="!item.totalCost"
                aria-label="Export"
                class="p-0 bg-transparent border-0 opacity-0 cursor-pointer pointer-events-none export-button history-item:hover:opacity-100 history-item:hover:pointer-events-auto"
                @click.stop="handleExportTask(item.id)"
              >
                <div class="text-[11px] font-medium opacity-100">EXPORT</div>
              </button>
            </div>

            <div
              v-if="item.cacheWrites || item.cacheReads"
              class="flex flex-wrap gap-1 items-center"
            >
              <span
                class="font-medium text-[var(--vscode-descriptionForeground)]"
                >Cache:</span
              >
              <span
                v-if="item.cacheWrites > 0"
                class="flex items-center gap-1 text-[var(--vscode-descriptionForeground)]"
              >
                <i
                  class="-mb-0.5 text-xs font-bold i-codicon:arrow-right"
                />
                {{ formatLargeNumber(item.cacheWrites) }}
              </span>
              <span
                v-if="item.cacheReads > 0"
                class="flex items-center gap-1 text-[var(--vscode-descriptionForeground)]"
              >
                <i class="mb-0 text-xs font-bold i-codicon:arrow-left" />
                {{ formatLargeNumber(item.cacheReads) }}
              </span>
            </div>
            <div
              v-if="item.totalCost"
              class="flex justify-between items-center -mt-0.5"
            >
              <div class="flex gap-1 items-center">
                <span
                  class="font-medium text-[var(--vscode-descriptionForeground)]"
                  >API Cost:</span
                >
                <span class="text-[var(--vscode-descriptionForeground)]"
                  >${{ item.totalCost?.toFixed(4) }}</span
                >
              </div>
              <button
                aria-label="Export"
                class="p-0 bg-transparent border-0 opacity-0 cursor-pointer pointer-events-none export-button history-item:hover:opacity-100 history-item:hover:pointer-events-auto"
                @click.stop="handleExportTask(item.id)"
              >
                <div class="text-[11px] font-medium opacity-100">EXPORT</div>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="p-2.5 border-t border-[var(--vscode-panel-border)]">
      <DangerButton
        v-if="selectedItems.length > 0"
        aria-label="Delete selected items"
        class="w-full"
        @click="handleDeleteSelectedHistoryItems(selectedItems)"
      >
        Delete
        {{ selectedItems.length > 1 ? selectedItems.length : "" }} Selected
        <template v-if="selectedItemsSize > 0">
          ({{ formatSize(selectedItemsSize) }})</template
        >
      </DangerButton>
      <DangerButton
        v-else
        aria-label="Delete all history"
        :disabled="deleteAllDisabled || taskHistory.length === 0"
        class="w-full"
        @click="handleDeleteAll"
      >
        Delete All History<template v-if="totalTasksSize !== null">
          ({{ formatSize(totalTasksSize) }})</template
        >
      </DangerButton>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  ref,
  computed,
  watch,
  onMounted,
  onBeforeUnmount,
  h,
  defineComponent,
} from "vue";
import { useRouter } from "vue-router";
import Fuse, { type FuseResult } from "fuse.js";
import DangerButton from "@/components/common/DangerButton.vue";
import { formatLargeNumber, formatSize } from "@/utils/format";
import { useExtensionStateStore } from "@/stores/extensionState";
import { taskService } from "@/api/task";

// CustomFilterRadio component - matches React version
const CustomFilterRadio = defineComponent({
  props: {
    checked: {
      type: Boolean,
      required: true,
    },
    icon: {
      type: String,
      required: true,
    },
    label: {
      type: String,
      required: true,
    },
  },
  emits: ["change"],
  setup(props, { emit }) {
    const handleClick = () => {
      emit("change");
    };

    return () =>
      h(
        "div",
        {
          class:
            "flex items-center cursor-pointer py-[0.3em] px-0 mr-[10px] text-[var(--vscode-font-size)] select-none",
          onClick: handleClick,
        },
        [
          h(
            "div",
            {
              class: `w-[14px] h-[14px] rounded-full border border-[var(--vscode-checkbox-border)] relative flex justify-center items-center mr-[6px] ${
                props.checked
                  ? "bg-[var(--vscode-checkbox-background)]"
                  : "bg-transparent"
              }`,
            },
            [
              props.checked &&
                h("div", {
                  class:
                    "w-[6px] h-[6px] rounded-full bg-[var(--vscode-checkbox-foreground)]",
                }),
            ]
          ),
          h("span", { class: "flex items-center gap-[3px]" }, [
            h("div", {
              class: `text-base i-codicon:${props.icon} text-[var(--vscode-button-background)]`,
            }),
            props.label,
          ]),
        ]
      );
  },
});

interface Props {
  onDone?: () => void;
}

const props = withDefaults(defineProps<Props>(), {
  onDone: undefined,
});

const router = useRouter();

type SortOption =
  | "newest"
  | "oldest"
  | "mostExpensive"
  | "mostTokens"
  | "mostRelevant";

const extensionStateStore = useExtensionStateStore();
const taskHistory = computed(
  () => extensionStateStore.extensionState?.taskHistory || []
);

// Handle done button click - use router if onDone prop is not provided
const handleDone = () => {
  if (props.onDone) {
    props.onDone();
  } else {
    // Use router to navigate back to chat
    router.push({ name: "Chat" });
  }
};

// Simple environment color mapping
const environmentColor = computed(() => {
  return "var(--vscode-textLink-foreground)";
});

const searchQuery = ref("");
const sortOption = ref<SortOption>("newest");
const lastNonRelevantSort = ref<SortOption | null>("newest");
const deleteAllDisabled = ref(false);
const selectedItems = ref<string[]>([]);
const showFavoritesOnly = ref(false);
const showCurrentWorkspaceOnly = ref(false);
const pendingFavoriteToggles = ref<Record<string, boolean>>({});
const tasks = ref<any[]>([]);

const sortOptions = computed(() => [
  { value: "newest", label: "Newest", disabled: false },
  { value: "oldest", label: "Oldest", disabled: false },
  { value: "mostExpensive", label: "Most Expensive", disabled: false },
  { value: "mostTokens", label: "Most Tokens", disabled: false },
  {
    value: "mostRelevant",
    label: "Most Relevant",
    disabled: !searchQuery.value,
  },
]);

const loadTaskHistory = async () => {
  try {
    const response = await taskService.getTaskHistory({
      favoritesOnly: showFavoritesOnly.value,
      searchQuery: searchQuery.value || undefined,
      sortBy: sortOption.value,
      currentWorkspaceOnly: showCurrentWorkspaceOnly.value,
    });
    tasks.value = response.tasks || [];
  } catch (error) {
    console.error("Error loading task history:", error);
  }
};

const fuse = computed(() => {
  return new Fuse(tasks.value, {
    keys: ["task"],
    threshold: 0.6,
    shouldSort: true,
    isCaseSensitive: false,
    ignoreLocation: false,
    includeMatches: true,
    minMatchCharLength: 1,
  });
});

const taskHistorySearchResults = computed(() => {
  const results = searchQuery.value
    ? highlight(fuse.value.search(searchQuery.value))
    : tasks.value;

  results.sort((a, b) => {
    switch (sortOption.value) {
      case "oldest":
        return a.ts - b.ts;
      case "mostExpensive":
        return (b.totalCost || 0) - (a.totalCost || 0);
      case "mostTokens":
        return (
          (b.tokensIn || 0) +
          (b.tokensOut || 0) +
          (b.cacheWrites || 0) +
          (b.cacheReads || 0) -
          ((a.tokensIn || 0) +
            (a.tokensOut || 0) +
            (a.cacheWrites || 0) +
            (a.cacheReads || 0))
        );
      case "mostRelevant":
        return searchQuery.value ? 0 : b.ts - a.ts;
      case "newest":
      default:
        return b.ts - a.ts;
    }
  });

  return results;
});

const selectedItemsSize = computed(() => {
  if (selectedItems.value.length === 0) {
    return 0;
  }
  return taskHistory.value
    .filter((item) => selectedItems.value.includes(item.id))
    .reduce((total, item) => total + (item.size || 0), 0);
});

const totalTasksSize = computed(
  () => extensionStateStore.extensionState?.totalTasksSize ?? null
);

const toggleFavorite = async (taskId: string, currentValue: boolean) => {
  // Optimistic UI update
  pendingFavoriteToggles.value = {
    ...pendingFavoriteToggles.value,
    [taskId]: !currentValue,
  };

  try {
    await taskService.toggleTaskFavorite({
      taskId,
      isFavorited: !currentValue,
    });

    // Refresh if either filter is active to ensure proper combined filtering
    if (showFavoritesOnly.value || showCurrentWorkspaceOnly.value) {
      await loadTaskHistory();
    }
  } catch (err) {
    console.error(`[FAVORITE_TOGGLE_UI] Error for task ${taskId}:`, err);
    // Revert optimistic update
    const updated = { ...pendingFavoriteToggles.value };
    delete updated[taskId];
    pendingFavoriteToggles.value = updated;
  } finally {
    // Clean up pending state after 1 second
    setTimeout(() => {
      const updated = { ...pendingFavoriteToggles.value };
      delete updated[taskId];
      pendingFavoriteToggles.value = updated;
    }, 1000);
  }
};

const handleShowTaskWithId = (id: string) => {
  extensionStateStore
    .selectConversation(id)
    .then(() => {
      router.push({ name: "Chat" });
    })
    .catch((error: any) => console.error("Error showing task:", error));
};

const handleHistorySelect = (itemId: string, checked: boolean) => {
  if (checked) {
    selectedItems.value = [...selectedItems.value, itemId];
  } else {
    selectedItems.value = selectedItems.value.filter((id) => id !== itemId);
  }
};

const handleDeleteHistoryItem = (id: string) => {
  taskService
    .deleteTasksWithIds([id])
    .then(() => fetchTotalTasksSize())
    .catch((error: any) => console.error("Error deleting task:", error));
};

const handleDeleteSelectedHistoryItems = (ids: string[]) => {
  if (ids.length > 0) {
    taskService
      .deleteTasksWithIds(ids)
      .then(() => fetchTotalTasksSize())
      .catch((error: any) => console.error("Error deleting tasks:", error));
    selectedItems.value = [];
  }
};

const handleDeleteAll = () => {
  deleteAllDisabled.value = true;
  taskService
    .deleteAllTaskHistory()
    .then(() => fetchTotalTasksSize())
    .catch((error: any) => console.error("Error deleting task history:", error))
    .finally(() => {
      deleteAllDisabled.value = false;
    });
};

const handleExportTask = (itemId: string) => {
  taskService
    .exportTaskWithId(itemId)
    .catch((err: any) => console.error("Failed to export task:", err));
};

const handleBatchHistorySelect = (selectAll: boolean) => {
  if (selectAll) {
    selectedItems.value = taskHistorySearchResults.value.map((item) => item.id);
  } else {
    selectedItems.value = [];
  }
};

const handleSearchInput = () => {
  if (
    searchQuery.value &&
    sortOption.value !== "mostRelevant" &&
    !lastNonRelevantSort.value
  ) {
    lastNonRelevantSort.value = sortOption.value;
    sortOption.value = "mostRelevant";
  } else if (
    !searchQuery.value &&
    sortOption.value === "mostRelevant" &&
    lastNonRelevantSort.value
  ) {
    sortOption.value = lastNonRelevantSort.value;
    lastNonRelevantSort.value = null;
  }
};

const handleSortChange = () => {
  // Sort change handled by computed property
};

const formatDate = (timestamp: number) => {
  const date = new Date(timestamp);
  return date
    ?.toLocaleString("en-US", {
      month: "long",
      day: "numeric",
      hour: "numeric",
      minute: "2-digit",
      hour12: true,
    })
    .replace(", ", " ")
    .replace(" at", ",")
    .toUpperCase();
};

const fetchTotalTasksSize = async () => {
  try {
    const response = await taskService.getTotalTasksSize();
    if (response && typeof response === "number") {
      extensionStateStore.setTotalTasksSize(response || 0);
    }
  } catch (error) {
    console.error("Error getting total tasks size:", error);
  }
};

// https://gist.github.com/evenfrost/1ba123656ded32fb7a0cd4651efd4db0
const highlight = (
  fuseSearchResult: FuseResult<any>[],
  highlightClassName: string = "history-item-highlight"
) => {
  const set = (obj: Record<string, any>, path: string, value: any) => {
    const pathValue = path.split(".");
    let i: number;

    for (i = 0; i < pathValue.length - 1; i++) {
      obj = obj[pathValue[i]] as Record<string, any>;
    }

    obj[pathValue[i]] = value;
  };

  const mergeRegions = (regions: [number, number][]): [number, number][] => {
    if (regions.length === 0) {
      return regions;
    }

    regions.sort((a, b) => a[0] - b[0]);

    const merged: [number, number][] = [regions[0]];

    for (let i = 1; i < regions.length; i++) {
      const last = merged[merged.length - 1];
      const current = regions[i];

      if (current[0] <= last[1] + 1) {
        last[1] = Math.max(last[1], current[1]);
      } else {
        merged.push(current);
      }
    }

    return merged;
  };

  const generateHighlightedText = (
    inputText: string,
    regions: [number, number][] = []
  ) => {
    if (regions.length === 0) {
      return inputText;
    }

    const mergedRegions = mergeRegions(regions);

    let content = "";
    let nextUnhighlightedRegionStartingIndex = 0;

    mergedRegions.forEach((region) => {
      const start = region[0];
      const end = region[1];
      const lastRegionNextIndex = end + 1;

      content += [
        inputText.substring(nextUnhighlightedRegionStartingIndex, start),
        `<span class="${highlightClassName}">`,
        inputText.substring(start, lastRegionNextIndex),
        "</span>",
      ].join("");

      nextUnhighlightedRegionStartingIndex = lastRegionNextIndex;
    });

    content += inputText.substring(nextUnhighlightedRegionStartingIndex);

    return content;
  };

  return fuseSearchResult
    .filter(({ matches }) => matches && matches.length)
    .map(({ item, matches }) => {
      const highlightedItem = { ...item };

      matches?.forEach((match: any) => {
        if (match.key && typeof match.value === "string" && match.indices) {
          const mergedIndices = mergeRegions([...match.indices]);
          set(
            highlightedItem,
            match.key,
            generateHighlightedText(match.value, mergedIndices)
          );
        }
      });

      return highlightedItem;
    });
};

watch([showFavoritesOnly, showCurrentWorkspaceOnly], () => {
  if (showFavoritesOnly.value && showCurrentWorkspaceOnly.value) {
    tasks.value = [];
  }
  loadTaskHistory();
});

watch([searchQuery, sortOption], () => {
  loadTaskHistory();
});

// Use the onRelinquishControl hook instead of message event
let relinquishControlCleanup: (() => void) | null = null;

onMounted(() => {
  loadTaskHistory();
  fetchTotalTasksSize();
  // Register relinquish control callback
  relinquishControlCleanup = extensionStateStore.onRelinquishControl(() => {
    deleteAllDisabled.value = false;
  });
});

onBeforeUnmount(() => {
  // Cleanup relinquish control callback
  if (relinquishControlCleanup) {
    relinquishControlCleanup();
    relinquishControlCleanup = null;
  }
});
</script>

<style scoped>
.history-item:hover {
  background-color: var(--vscode-list-hoverBackground);
}

.delete-button,
.export-button {
  opacity: 0;
  pointer-events: none;
}

.history-item:hover .delete-button,
.history-item:hover .export-button {
  opacity: 1;
  pointer-events: auto;
}

.history-item-highlight {
  background-color: var(--vscode-editor-findMatchHighlightBackground);
  color: inherit;
}
</style>
