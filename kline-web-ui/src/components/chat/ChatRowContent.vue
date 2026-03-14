<template>
  <!-- Tool messages -->
  <template v-if="tool">
    <!-- editedExistingFile -->
    <template v-if="tool.tool === 'editedExistingFile'">
      <div class="flex gap-2.5 items-center mb-3">
        <ToolIcon name="edit" />
        <ToolIcon
          v-if="tool.operationIsLocatedInWorkspace === false"
          name="sign-out"
          color="yellow"
          :rotation="-90"
          title="This file is outside of your workspace"
        />
        <span class="font-bold">Cline wants to edit this file:</span>
      </div>
      <CodeAccordian
        :code="tool.content"
        :is-expanded="isExpanded"
        :on-toggle-expand="handleToggle"
        :path="tool.path!"
      />
    </template>

    <!-- newFileCreated -->
    <template v-else-if="tool.tool === 'newFileCreated'">
      <div class="flex gap-2.5 items-center mb-3">
        <ToolIcon name="new-file" />
        <ToolIcon
          v-if="tool.operationIsLocatedInWorkspace === false"
          name="sign-out"
          color="yellow"
          :rotation="-90"
          title="This file is outside of your workspace"
        />
        <span class="font-bold">Cline wants to create a new file:</span>
      </div>
      <CodeAccordian
        :code="tool.content!"
        :is-expanded="isExpanded"
        :is-loading="message.partial"
        :on-toggle-expand="handleToggle"
        :path="tool.path!"
      />
    </template>

    <!-- readFile -->
    <template v-else-if="tool.tool === 'readFile'">
      <div class="flex gap-2.5 items-center mb-3">
        <ToolIcon :name="isImageFile(tool.path || '') ? 'file-media' : 'file-code'" />
        <ToolIcon
          v-if="tool.operationIsLocatedInWorkspace === false"
          name="sign-out"
          color="yellow"
          :rotation="-90"
          title="This file is outside of your workspace"
        />
        <span class="font-bold">Cline wants to read this file:</span>
      </div>
      <div class="rounded-[3px] bg-[var(--vscode-editor-background)] overflow-hidden border border-[var(--vscode-editorGroup-border)]">
        <div
          :class="[
            'text-[var(--vscode-descriptionForeground)] flex items-center px-2.5 py-2.25',
            isImageFile(tool.path || '') ? 'cursor-default' : 'cursor-pointer',
          ]"
          :style="{
            userSelect: isImageFile(tool.path || '') ? 'text' : 'none',
            WebkitUserSelect: isImageFile(tool.path || '') ? 'text' : 'none',
            MozUserSelect: isImageFile(tool.path || '') ? 'text' : 'none',
            msUserSelect: isImageFile(tool.path || '') ? 'text' : 'none',
          }"
          @click="!isImageFile(tool.path || '') && tool.content && handleReadFileClick(tool.content)"
        >
          <span v-if="tool.path?.startsWith('.')">.</span>
          <span v-if="tool.path && !tool.path.startsWith('.')">/</span>
          <span
            class="overflow-hidden flex-1 mr-2 text-left whitespace-nowrap ph-no-capture text-ellipsis rtl"
          >
            {{ cleanPathPrefix(tool.path || '') }}\u200E
          </span>
          <div class="flex-grow"></div>
          <span
            v-if="!isImageFile(tool.path || '')"
            class="i-codicon:link-external text-[13.5px] my-[1px]"
          ></span>
        </div>
      </div>
    </template>

    <!-- listFilesTopLevel -->
    <template v-else-if="tool.tool === 'listFilesTopLevel'">
      <div class="flex gap-2.5 items-center mb-3">
        <ToolIcon name="folder-opened" />
        <ToolIcon
          v-if="tool.operationIsLocatedInWorkspace === false"
          name="sign-out"
          color="yellow"
          :rotation="-90"
          title="This is outside of your workspace"
        />
        <span class="font-bold">
          {{ message.type === 'ask' ? 'Cline wants to view the top level files in this directory:' : 'Cline viewed the top level files in this directory:' }}
        </span>
      </div>
      <CodeAccordian
        :code="tool.content!"
        :is-expanded="isExpanded"
        language="shell-session"
        :on-toggle-expand="handleToggle"
        :path="tool.path!"
      />
    </template>

    <!-- listFilesRecursive -->
    <template v-else-if="tool.tool === 'listFilesRecursive'">
      <div class="flex gap-2.5 items-center mb-3">
        <ToolIcon name="folder-opened" />
        <ToolIcon
          v-if="tool.operationIsLocatedInWorkspace === false"
          name="sign-out"
          color="yellow"
          :rotation="-90"
          title="This is outside of your workspace"
        />
        <span class="font-bold">
          {{ message.type === 'ask' ? 'Cline wants to recursively view all files in this directory:' : 'Cline recursively viewed all files in this directory:' }}
        </span>
      </div>
      <CodeAccordian
        :code="tool.content!"
        :is-expanded="isExpanded"
        language="shell-session"
        :on-toggle-expand="handleToggle"
        :path="tool.path!"
      />
    </template>

    <!-- listCodeDefinitionNames -->
    <template v-else-if="tool.tool === 'listCodeDefinitionNames'">
      <div class="flex gap-2.5 items-center mb-3">
        <ToolIcon name="file-code" />
        <ToolIcon
          v-if="tool.operationIsLocatedInWorkspace === false"
          name="sign-out"
          color="yellow"
          :rotation="-90"
          title="This file is outside of your workspace"
        />
        <span class="font-bold">
          {{ message.type === 'ask' ? 'Cline wants to view source code definition names used in this directory:' : 'Cline viewed source code definition names used in this directory:' }}
        </span>
      </div>
      <CodeAccordian
        :code="tool.content!"
        :is-expanded="isExpanded"
        :on-toggle-expand="handleToggle"
        :path="tool.path!"
      />
    </template>

    <!-- searchFiles -->
    <template v-else-if="tool.tool === 'searchFiles'">
      <div class="flex gap-2.5 items-center mb-3">
        <ToolIcon name="search" />
        <ToolIcon
          v-if="tool.operationIsLocatedInWorkspace === false"
          name="sign-out"
          color="yellow"
          :rotation="-90"
          title="This is outside of your workspace"
        />
        <span class="font-bold">
          Cline wants to search this directory for
          <code class="break-all">{{ tool.regex }}</code>:
        </span>
      </div>
      <SearchResultsDisplay
        :content="tool.content!"
        :file-pattern="tool.filePattern"
        :is-expanded="isExpanded"
        :on-toggle-expand="handleToggle"
        :path="tool.path!"
      />
    </template>

    <!-- summarizeTask -->
    <template v-else-if="tool.tool === 'summarizeTask'">
      <div class="flex gap-2.5 items-center mb-3">
        <ToolIcon name="book" />
        <span class="font-bold">Cline is condensing the conversation:</span>
      </div>
      <div class="rounded-[3px] bg-[var(--vscode-editor-background)] overflow-hidden border border-[var(--vscode-editorGroup-border)]">
        <div
          class="text-[var(--vscode-descriptionForeground)] px-2.5 py-2.25 cursor-pointer select-none"
          @click="handleToggle"
        >
          <template v-if="isExpanded">
            <div>
              <div class="flex items-center mb-2">
                <span class="mr-1 font-bold">Summary:</span>
                <div class="flex-grow"></div>
                <span class="i-codicon:chevron-up text-[13.5px] my-[1px]"></span>
              </div>
              <span class="whitespace-pre-wrap break-words ph-no-capture overflow-wrap-anywhere">
                {{ tool.content }}
              </span>
            </div>
          </template>
          <template v-else>
            <div class="flex items-center">
              <span
                class="overflow-hidden flex-1 mr-2 text-left whitespace-nowrap ph-no-capture text-ellipsis rtl"
              >
                {{ tool.content }}\u200E
              </span>
              <span class="i-codicon:chevron-down text-[13.5px] my-[1px] flex-shrink-0"></span>
            </div>
          </template>
        </div>
      </div>
    </template>

    <!-- webFetch -->
    <template v-else-if="tool.tool === 'webFetch'">
      <div class="flex gap-2.5 items-center mb-3">
        <span class="i-codicon:link text-[var(--vscode-foreground)] -mb-[1.5px]"></span>
        <ToolIcon
          v-if="tool.operationIsLocatedInWorkspace === false"
          name="sign-out"
          color="yellow"
          :rotation="-90"
          title="This URL is external"
        />
        <span class="font-bold">
          {{ message.type === 'ask' ? 'Cline wants to fetch content from this URL:' : 'Cline fetched content from this URL:' }}
        </span>
      </div>
      <div
        class="rounded-[3px] bg-[var(--vscode-editor-background)] overflow-hidden border border-[var(--vscode-editorGroup-border)] px-2.5 py-2.25 cursor-pointer select-none"
        @click="tool.path && handleOpenUrl(tool.path)"
      >
        <span
          class="ph-no-capture whitespace-nowrap overflow-hidden text-ellipsis mr-2 rtl text-left text-[var(--vscode-textLink-foreground)] underline"
        >
          {{ tool.path }}\u200E
        </span>
      </div>
    </template>
  </template>

  <!-- Command messages -->
  <template v-else-if="isCommandMessage">
    <div class="flex gap-2.5 items-center mb-3">
      <component :is="displayIcon" />
      <component :is="displayTitle" />
    </div>
    <div
      class="rounded-md border border-[var(--vscode-editorGroup-border)] overflow-visible bg-[var(--vscode-editor-background)] transition-all duration-300 ease-in-out"
    >
      <div
        v-if="command"
        class="flex items-center justify-between px-2.5 py-2 bg-[var(--vscode-editor-background)] border-b border-[var(--vscode-editorGroup-border)] rounded-t-md"
      >
        <div class="flex flex-1 gap-2 items-center min-w-0">
          <div
            class="flex-shrink-0 w-2 h-2 rounded-full"
            :class="{
              'bg-[var(--vscode-charts-green)] animate-pulse': isCommandExecuting,
              'bg-[var(--vscode-editorWarning-foreground)]': isCommandPending,
              'bg-[var(--vscode-descriptionForeground)]': !isCommandExecuting && !isCommandPending,
            }"
            :style="{
              animation: isCommandExecuting ? 'pulse 2s ease-in-out infinite' : 'none',
            }"
          ></div>
          <span
            class="text-[13px] flex-shrink-0"
            :class="{
              'text-[var(--vscode-charts-green)]': isCommandExecuting,
              'text-[var(--vscode-editorWarning-foreground)]': isCommandPending,
              'text-[var(--vscode-descriptionForeground)]': !isCommandExecuting && !isCommandPending,
            }"
          >
            {{
              isCommandExecuting
                ? 'Running'
                : isCommandPending
                  ? 'Pending'
                  : isCommandCompleted
                    ? 'Completed'
                    : 'Not Executed'
            }}
          </span>
        </div>
        <div class="flex flex-shrink-0 gap-2 items-center">
          <button
            v-if="showCancelButton"
            class="bg-[var(--vscode-button-secondaryBackground)] text-[var(--vscode-button-secondaryForeground)] border-0 rounded-[2px] px-2.5 py-1 text-xs cursor-pointer font-inherit"
            @click.stop="handleCancelCommand"
            @mouseenter="(e: MouseEvent) => {
              (e.currentTarget as HTMLElement).style.background = 'var(--vscode-button-secondaryHoverBackground)'
            }"
            @mouseleave="(e: MouseEvent) => {
              (e.currentTarget as HTMLElement).style.background = 'var(--vscode-button-secondaryBackground)'
            }"
          >
            {{ vscodeTerminalExecutionMode === 'backgroundExec' ? 'cancel' : 'stop' }}
          </button>
        </div>
      </div>
      <div
        v-if="subagentPrompt"
        class="p-2.5 border-b border-[var(--vscode-editorGroup-border)]"
      >
        <div class="mb-0">
          <strong>Prompt:</strong>
          <span class="ph-no-capture" style="font-family: var(--vscode-editor-font-family)">
            {{ subagentPrompt }}
          </span>
        </div>
      </div>
      <div class="opacity-60 bg-[var(--vscode-editor-background)]">
        <div class="bg-[var(--vscode-editor-background)]">
          <CodeBlock :force-wrap="true" :source="`\`\`\`shell\n${command}\n\`\`\``" />
        </div>
      </div>
      <CommandOutput
        v-if="output.length > 0"
        :is-container-expanded="true"
        :is-output-fully-expanded="isOutputFullyExpanded"
        :output="output"
        @toggle="isOutputFullyExpanded = !isOutputFullyExpanded"
      />
    </div>
    <div
      v-if="requestsApproval"
      class="flex items-center gap-2.5 p-2 text-xs text-[var(--vscode-editorWarning-foreground)]"
    >
      <i class="i-codicon:warning"></i>
      <span>The model has determined this command requires explicit approval.</span>
    </div>
  </template>

  <!-- use_mcp_server messages -->
  <template v-else-if="message.ask === 'use_mcp_server' || message.say === 'use_mcp_server'">
    <div class="flex gap-2.5 items-center mb-3">
      <component :is="icon" />
      <component :is="title" />
    </div>
    <div
      class="bg-[var(--vscode-textCodeBlock-background)] rounded-[3px] px-2.5 py-2 mt-2"
    >
      <McpResourceRow
        v-if="useMcpServer && useMcpServer.type === 'access_mcp_resource'"
        :item="{
          ...(findMatchingResourceOrTemplate(
            useMcpServer?.uri || '',
            server?.resources,
            server?.resourceTemplates
          ) || {
            name: '',
            mimeType: '',
            description: '',
          }),
          uri: useMcpServer?.uri || '',
        }"
      />
      <template v-if="useMcpServer && useMcpServer.type === 'use_mcp_tool'">
        <div @click.stop>
          <McpToolRow
            :server-name="useMcpServer.serverName || ''"
            :tool="{
              name: useMcpServer.toolName || '',
              description:
                server?.tools?.find((tool: any) => tool.name === useMcpServer?.toolName)?.description || '',
              autoApprove:
                server?.tools?.find((tool: any) => tool.name === useMcpServer?.toolName)?.autoApprove ||
                false,
            }"
          />
        </div>
        <div v-if="useMcpServer.arguments && useMcpServer.arguments !== '{}'" class="mt-2">
          <div class="mb-1 text-xs uppercase opacity-80">Arguments</div>
          <CodeAccordian
            :code="useMcpServer.arguments"
            :is-expanded="true"
            language="json"
            :on-toggle-expand="handleToggle"
          />
        </div>
      </template>
    </div>
  </template>

  <!-- say message types -->
  <template v-else-if="message.type === 'say'">
    <!-- api_req_started -->
    <template v-if="message.say === 'api_req_started'">
      <div
        class="flex justify-between items-center mb-3 cursor-pointer select-none"
        :class="{
          'mb-2.5':
            (cost == null && apiRequestFailedMessage) || apiReqStreamingFailedMessage,
        }"
        @click="handleToggle"
      >
        <div class="flex gap-2.5 items-center">
          <component :is="icon" />
          <component :is="title" />
          <span
            class="text-xs px-1 py-0.5 rounded bg-[var(--vscode-badge-background)] text-[var(--vscode-badge-foreground)]"
            :style="{ opacity: cost != null && cost > 0 ? 1 : 0 }"
          >
            ${{ Number(cost || 0)?.toFixed(4) }}
          </span>
        </div>
        <span :class="`i-codicon:chevron-${isExpanded ? 'up' : 'down'}`"></span>
      </div>
      <ErrorRow
        v-if="(cost == null && apiRequestFailedMessage) || apiReqStreamingFailedMessage"
        :api-req-streaming-failed-message="apiReqStreamingFailedMessage"
        :api-request-failed-message="apiRequestFailedMessage"
        error-type="error"
        :message="message"
      />
      <div v-if="isExpanded" class="mt-2.5">
        <CodeAccordian
          :code="JSON.parse(message.text || '{}').request"
          :is-expanded="true"
          language="markdown"
          :on-toggle-expand="handleToggle"
        />
      </div>
    </template>

    <!-- api_req_finished -->
    <template v-else-if="message.say === 'api_req_finished'">
      <!-- Should never see this message type -->
    </template>

    <!-- mcp_server_response -->
    <template v-else-if="message.say === 'mcp_server_response'">
      <McpResponseDisplay :response-text="message.text || ''" />
    </template>

    <!-- mcp_notification -->
    <template v-else-if="message.say === 'mcp_notification'">
      <div
        class="flex items-start gap-2 px-3 py-2 bg-[var(--vscode-textBlockQuote-background)] rounded text-[13px] text-[var(--vscode-foreground)] opacity-90 mb-2"
      >
        <i
          class="i-codicon:bell mt-0.5 text-sm text-[var(--vscode-notificationsInfoIcon-foreground)] flex-shrink-0"
        ></i>
        <div class="flex-1 break-words">
          <span class="font-medium">MCP Notification: </span>
          <span class="ph-no-capture">{{ message.text }}</span>
        </div>
      </div>
    </template>

    <!-- text -->
    <template v-else-if="message.say === 'text'">
      <WithCopyButton
        ref="contentRef"
        :on-mouse-up="handleMouseUp"
        position="bottom-right"
        :text-to-copy="message.text"
      >
        <Markdown :markdown="message.text" />
        <QuoteButton
          v-if="quoteButtonState.visible"
          :left="quoteButtonState.left"
          :top="quoteButtonState.top"
          @click="handleQuoteClick"
        />
      </WithCopyButton>
    </template>

    <!-- reasoning -->
    <template v-else-if="message.say === 'reasoning'">
      <template v-if="message.text">
        <div
          class="cursor-pointer text-[var(--vscode-descriptionForeground)] italic overflow-hidden"
          @click="handleToggle"
        >
          <template v-if="isExpanded">
            <div class="-mt-0.75">
              <span class="block mb-1 font-bold">
                Thinking
                <span
                  class="i-codicon:chevron-down inline-block translate-y-[3px] ml-[1.5px]"
                ></span>
              </span>
              <span class="ph-no-capture">{{ message.text }}</span>
            </div>
          </template>
          <template v-else>
            <div class="flex items-center">
              <span class="mr-1 font-bold">Thinking:</span>
              <span
                class="overflow-hidden flex-1 text-left whitespace-nowrap ph-no-capture text-ellipsis rtl"
              >
                {{ message.text }}\u200E
              </span>
              <span class="flex-shrink-0 ml-1 i-codicon:chevron-right"></span>
            </div>
          </template>
        </div>
      </template>
    </template>

    <!-- user_feedback -->
    <template v-else-if="message.say === 'user_feedback'">
      <UserMessage
        :files="message.files"
        :images="message.images"
        :message-ts="message.ts"
        :send-message-from-chat-row="sendMessageFromChatRow"
        :text="message.text"
      />
    </template>

    <!-- user_feedback_diff -->
    <template v-else-if="message.say === 'user_feedback_diff' && feedbackDiffTool">
      <div class="-mt-2.5 w-full">
        <CodeAccordian
          :diff="feedbackDiffTool.diff!"
          :is-expanded="isExpanded"
          :is-feedback="true"
          :on-toggle-expand="handleToggle"
        />
      </div>
    </template>

    <!-- error -->
    <template v-else-if="message.say === 'error'">
      <ErrorRow error-type="error" :message="message" />
    </template>

    <!-- diff_error -->
    <template v-else-if="message.say === 'diff_error'">
      <ErrorRow error-type="diff_error" :message="message" />
    </template>

    <!-- clineignore_error -->
    <template v-else-if="message.say === 'clineignore_error'">
      <ErrorRow error-type="clineignore_error" :message="message" />
    </template>

    <!-- checkpoint_created -->
    <template v-else-if="message.say === 'checkpoint_created'">
      <CheckmarkControl
        :is-checkpoint-checked-out="message.isCheckpointCheckedOut"
        :message-ts="message.ts"
      />
    </template>

    <!-- load_mcp_documentation -->
    <template v-else-if="message.say === 'load_mcp_documentation'">
      <div class="flex items-center text-[var(--vscode-foreground)] opacity-70 text-xs py-1">
        <i class="mr-1.5 i-codicon:book"></i>
        Loading MCP documentation
      </div>
    </template>

    <!-- load_tool_set -->
    <template v-else-if="message.say === 'load_tool_set'">
      <div class="flex items-center text-[var(--vscode-foreground)] opacity-70 text-xs py-1">
        <i class="mr-1.5 i-codicon:package"></i>
        Loading ToolSet
      </div>
    </template>

    <!-- completion_result -->
    <template v-else-if="message.say === 'completion_result'">
      <div class="flex gap-2.5 items-center mb-2.5">
        <component :is="icon" />
        <component :is="title" />
      </div>
      <WithCopyButton
        ref="contentRef"
        :on-mouse-up="handleMouseUp"
        position="bottom-right"
        :style="{ color: 'var(--vscode-charts-green)', paddingTop: 10 }"
        :text-to-copy="completionResultText"
      >
        <Markdown :markdown="completionResultText" />
        <QuoteButton
          v-if="quoteButtonState.visible"
          :left="quoteButtonState.left"
          :top="quoteButtonState.top"
          @click="handleQuoteClick"
        />
      </WithCopyButton>
      <div v-if="message.partial !== true && completionResultHasChanges" class="pt-4.25">
        <SuccessButton
          :disabled="seeNewChangesDisabled"
          :style="{
            cursor: seeNewChangesDisabled ? 'wait' : 'pointer',
            width: '100%',
          }"
          @click="handleSeeNewChanges"
        >
          <i class="mr-1.5 i-codicon:new-file"></i>
          See new changes
        </SuccessButton>
      </div>
    </template>

    <!-- shell_integration_warning -->
    <template v-else-if="message.say === 'shell_integration_warning'">
      <div
        class="flex flex-col bg-[var(--vscode-textBlockQuote-background)] p-2 rounded-[3px] text-xs"
      >
        <div class="flex items-center mb-1">
          <i
            class="i-codicon:warning mr-2 text-sm text-[var(--vscode-descriptionForeground)]"
          ></i>
          <span class="font-medium text-[var(--vscode-foreground)]">
            Shell Integration Unavailable
          </span>
        </div>
        <div class="text-[var(--vscode-foreground)] opacity-80">
          Cline may have trouble viewing the command's output. Please update VSCode (
          <code>CMD/CTRL + Shift + P</code> → "Update") and make sure you're using a supported shell:
          zsh, bash, fish, or PowerShell (
          <code>CMD/CTRL + Shift + P</code> → "Terminal: Select Default Profile").
          <a
            href="https://github.com/cline/cline/wiki/Troubleshooting-%E2%80%90-Shell-Integration-Unavailable"
            class="underline text-inherit"
          >
            Still having trouble?
          </a>
        </div>
      </div>
    </template>

    <!-- error_retry -->
    <template v-else-if="message.say === 'error_retry'">
      <ErrorRetryDisplay :message="message" />
    </template>

    <!-- shell_integration_warning_with_suggestion -->
    <template v-else-if="message.say === 'shell_integration_warning_with_suggestion'">
      <ShellIntegrationSuggestion />
    </template>

    <!-- task_progress -->
    <template v-else-if="message.say === 'task_progress'">
      <!-- task_progress messages should be displayed in TaskHeader only, not in chat -->
    </template>

    <!-- default say message -->
    <template v-else>
      <div v-if="title" class="flex gap-2.5 items-center mb-3">
        <component :is="icon" />
        <component :is="title" />
      </div>
      <div class="pt-2.5">
        <Markdown :markdown="message.text" />
      </div>
    </template>
  </template>

  <!-- ask message types -->
  <template v-else-if="message.type === 'ask'">
    <!-- mistake_limit_reached -->
    <template v-if="message.ask === 'mistake_limit_reached'">
      <ErrorRow error-type="mistake_limit_reached" :message="message" />
    </template>

    <!-- auto_approval_max_req_reached -->
    <template v-else-if="message.ask === 'auto_approval_max_req_reached'">
      <ErrorRow error-type="auto_approval_max_req_reached" :message="message" />
    </template>

    <!-- completion_result -->
    <template v-else-if="message.ask === 'completion_result'">
      <template v-if="message.text">
        <div class="flex gap-2.5 items-center mb-2.5">
          <component :is="icon" />
          <component :is="title" />
          <TaskFeedbackButtons
            :is-from-history="
              !isLast ||
              lastModifiedMessage?.ask === 'resume_completed_task' ||
              lastModifiedMessage?.ask === 'resume_task'
            "
            :message-ts="message.ts"
            :style="{ marginLeft: 'auto' }"
          />
        </div>
        <WithCopyButton
          ref="contentRef"
          :on-mouse-up="handleMouseUp"
          position="bottom-right"
          :style="{ color: 'var(--vscode-charts-green)', paddingTop: 10 }"
          :text-to-copy="askCompletionResultText"
        >
          <Markdown :markdown="askCompletionResultText" />
          <QuoteButton
            v-if="quoteButtonState.visible"
            :left="quoteButtonState.left"
            :top="quoteButtonState.top"
            @click="handleQuoteClick"
          />
        </WithCopyButton>
        <div v-if="message.partial !== true && askCompletionResultHasChanges" class="mt-3.75">
          <SuccessButton
            appearance="secondary"
            :disabled="seeNewChangesDisabled"
            @click="handleSeeNewChanges"
          >
            <i
              class="mr-1.5 i-codicon:new-file"
              :style="{
                cursor: seeNewChangesDisabled ? 'wait' : 'pointer',
              }"
            ></i>
            See new changes
          </SuccessButton>
        </div>
      </template>
    </template>

    <!-- followup -->
    <template v-else-if="message.ask === 'followup'">
      <div v-if="title" class="flex gap-2.5 items-center mb-3">
        <component :is="icon" />
        <component :is="title" />
      </div>
      <WithCopyButton
        ref="contentRef"
        :on-mouse-up="handleMouseUp"
        position="bottom-right"
        :style="{ paddingTop: 10 }"
        :text-to-copy="followupQuestion"
      >
        <Markdown :markdown="followupQuestion" />
        <OptionsButtons
          :input-value="inputValue"
          :is-active="
            (isLast && lastModifiedMessage?.ask === 'followup') ||
            (!followupSelected && followupOptions && followupOptions.length > 0)
          "
          :options="followupOptions"
          :selected="followupSelected"
        />
        <QuoteButton
          v-if="quoteButtonState.visible"
          :left="quoteButtonState.left"
          :top="quoteButtonState.top"
          @click="handleQuoteClick"
        />
      </WithCopyButton>
    </template>

    <!-- new_task -->
    <template v-else-if="message.ask === 'new_task'">
      <div class="flex gap-2.5 items-center mb-3">
        <span class="i-codicon:new-file text-[var(--vscode-foreground)] -mb-[1.5px]"></span>
        <span class="text-[var(--vscode-foreground)] font-bold">
          Cline wants to start a new task:
        </span>
      </div>
      <NewTaskPreview :context="message.text || ''" />
    </template>

    <!-- condense -->
    <template v-else-if="message.ask === 'condense'">
      <div class="flex gap-2.5 items-center mb-3">
        <span class="i-codicon:new-file text-[var(--vscode-foreground)] -mb-[1.5px]"></span>
        <span class="text-[var(--vscode-foreground)] font-bold">
          Cline wants to condense your conversation:
        </span>
      </div>
      <NewTaskPreview :context="message.text || ''" />
    </template>

    <!-- report_bug -->
    <template v-else-if="message.ask === 'report_bug'">
      <div class="flex gap-2.5 items-center mb-3">
        <span class="i-codicon:new-file text-[var(--vscode-foreground)] -mb-[1.5px]"></span>
        <span class="text-[var(--vscode-foreground)] font-bold">
          Cline wants to create a Github issue:
        </span>
      </div>
      <ReportBugPreview :data="message.text || ''" />
    </template>

    <!-- plan_mode_respond -->
    <template v-else-if="message.ask === 'plan_mode_respond'">
      <WithCopyButton
        ref="contentRef"
        :on-mouse-up="handleMouseUp"
        position="bottom-right"
        :text-to-copy="planModeResponse"
      >
        <Markdown :markdown="planModeResponse" />
        <OptionsButtons
          :input-value="inputValue"
          :is-active="
            (isLast && lastModifiedMessage?.ask === 'plan_mode_respond') ||
            (!planModeSelected && planModeOptions && planModeOptions.length > 0)
          "
          :options="planModeOptions"
          :selected="planModeSelected"
        />
        <QuoteButton
          v-if="quoteButtonState.visible"
          :left="quoteButtonState.left"
          :top="quoteButtonState.top"
          @click="handleQuoteClick"
        />
      </WithCopyButton>
    </template>
  </template>
</template>

<script setup lang="ts">
import { computed, h, onMounted, ref, watch } from 'vue'
import { COMMAND_OUTPUT_STRING, COMMAND_REQ_APP_STRING } from '@/shared/combineCommandSequences'
import type {
  ClineApiReqInfo,
  ClineAskQuestion,
  ClineAskUseMcpServer,
  ClineMessage,
  ClinePlanModeResponse,
  ClineSayTool,
} from '@/shared/ExtensionMessage'
import { COMPLETION_RESULT_CHANGES_FLAG } from '@/shared/ExtensionMessage'
import { useExtensionStateStore } from "@/stores/extensionState"
import CheckmarkControl from '@/components/common/CheckmarkControl.vue'
import CodeAccordian from '@/components/common/CodeAccordian.vue'
import CodeBlock from '@/components/common/CodeBlock.vue'
import WithCopyButton from '@/components/common/WithCopyButton.vue'
import SuccessButton from '@/components/common/SuccessButton.vue'
import { findMatchingResourceOrTemplate, getMcpServerDisplayName } from '@/utils/mcp'
import McpResourceRow from '@/components/mcp/configuration/tabs/installed/server-row/McpResourceRow.vue'
import McpToolRow from '@/components/mcp/configuration/tabs/installed/server-row/McpToolRow.vue'
import McpResponseDisplay from '@/components/mcp/chat-display/McpResponseDisplay.vue'
import CommandOutput from './CommandOutput.vue'
import ErrorRow from './ErrorRow.vue'
import Markdown from './Markdown.vue'
import NewTaskPreview from './NewTaskPreview.vue'
import OptionsButtons from './OptionsButtons.vue'
import ProgressIndicator from './ProgressIndicator.vue'
import QuoteButton from './QuoteButton.vue'
import ReportBugPreview from './ReportBugPreview.vue'
import SearchResultsDisplay from './SearchResultsDisplay.vue'
import TaskFeedbackButtons from './TaskFeedbackButtons.vue'
import ToolIcon from './ToolIcon.vue'
import UserMessage from './UserMessage.vue'
import { getErrorBlockTitle } from './utils/errorBlockTitle'
import ErrorRetryDisplay from './ErrorRetryDisplay.vue'
import ShellIntegrationSuggestion from './ShellIntegrationSuggestion.vue'
import { fileService } from '@/api/file'
import { taskService } from '@/api/task'
import { McpMarketplaceCatalog, McpServer } from '@/shared/mcp'

interface Props {
  message: ClineMessage
  isExpanded: boolean
  onToggleExpand: (ts: number) => void
  lastModifiedMessage?: ClineMessage
  isLast: boolean
  inputValue?: string
  sendMessageFromChatRow?: (text: string, images: string[], files: string[]) => void
  onSetQuote: (text: string) => void
  onCancelCommand?: () => void
}

const props = defineProps<Props>()

  
const extensionState = computed(() => useExtensionStateStore().extensionState)
const extensionStateStore = useExtensionStateStore()
const mcpServers = computed(() => extensionStateStore.mcpServers || [])
const mcpMarketplaceCatalog = computed(() => extensionStateStore.mcpMarketplaceCatalog)
const vscodeTerminalExecutionMode = computed(
  () => extensionState.value?.vscodeTerminalExecutionMode || ''
)

const normalColor = 'var(--vscode-foreground)'
const errorColor = 'var(--vscode-errorForeground)'
const successColor = 'var(--vscode-charts-green)'
const cleanPathPrefix = (path: string): string =>
  path.replace(/^[^\u4e00-\u9fa5a-zA-Z0-9]+/, "");


const seeNewChangesDisabled = ref(false)
const quoteButtonState = ref<{
  visible: boolean
  top: number
  left: number
  selectedText: string
}>({
  visible: false,
  top: 0,
  left: 0,
  selectedText: '',
})
const contentRef = ref<HTMLDivElement | null>(null)
const isOutputFullyExpanded = ref(false)
const prevCommandExecutingRef = ref<boolean>(false)

const apiReqInfo = computed(() => {
  if (props.message.text != null && props.message.say === 'api_req_started') {
    try {
      return JSON.parse(props.message.text) as ClineApiReqInfo
    } catch {
      return null
    }
  }
  return null
})

const cost = computed(() => apiReqInfo.value?.cost)
const apiReqCancelReason = computed(() => apiReqInfo.value?.cancelReason)
const apiReqStreamingFailedMessage = computed(() => apiReqInfo.value?.streamingFailedMessage)
const retryStatus = computed(() => apiReqInfo.value?.retryStatus)

const apiRequestFailedMessage = computed(() => {
  return props.isLast && props.lastModifiedMessage?.ask === 'api_req_failed'
    ? props.lastModifiedMessage?.text
    : undefined
})

const isCommandMessage = computed(() => {
  return props.message.ask === 'command' || props.message.say === 'command'
})

const commandHasOutput = computed(() => {
  return props.message.text?.includes(COMMAND_OUTPUT_STRING) ?? false
})

const isCommandExecuting = computed(() => {
  return (
    isCommandMessage.value &&
    !props.message.commandCompleted &&
    commandHasOutput.value
  )
})

const isCommandPending = computed(() => {
  return (
    isCommandMessage.value &&
    props.isLast &&
    !props.message.commandCompleted &&
    !commandHasOutput.value
  )
})

const isCommandCompleted = computed(() => {
  return isCommandMessage.value && props.message.commandCompleted === true
})

const isMcpServerResponding = computed(() => {
  return props.isLast && props.lastModifiedMessage?.say === 'mcp_server_request_started'
})

const messageType = computed(() => {
  return props.message.type === 'ask' ? props.message.ask : props.message.say
})

const handleToggle = () => {
  props.onToggleExpand(props.message.ts)
}

// onRelinquishControl hook
onMounted(() => {
  const state = extensionState.value as { onRelinquishControl?: (callback: () => void) => () => void }
  const onRelinquishControl = state?.onRelinquishControl
  if (onRelinquishControl) {
    onRelinquishControl(() => {
      seeNewChangesDisabled.value = false
    })
  }
})

const handleQuoteClick = () => {
  props.onSetQuote(quoteButtonState.value.selectedText)
  window.getSelection()?.removeAllRanges()
  quoteButtonState.value = { visible: false, top: 0, left: 0, selectedText: '' }
}

const handleMouseUp = (event: MouseEvent) => {
  const targetElement = event.target as Element
  const isClickOnButton = !!targetElement.closest('.quote-button-class')

  setTimeout(() => {
    const selection = window.getSelection()
    const selectedText = selection?.toString().trim() ?? ''

    let shouldShowButton = false
    let buttonTop = 0
    let buttonLeft = 0
    let textToQuote = ''

    if (
      selectedText &&
      contentRef.value &&
      selection &&
      selection.rangeCount > 0 &&
      !selection.isCollapsed
    ) {
      const range = selection.getRangeAt(0)
      const rangeRect = range.getBoundingClientRect()
      const contentEl = (contentRef.value as { $el?: HTMLElement } | null)?.$el
      const containerRect = contentEl?.getBoundingClientRect?.()

      if (containerRect) {
        const tolerance = 5
        const isSelectionWithin =
          rangeRect.top >= containerRect.top &&
          rangeRect.left >= containerRect.left &&
          rangeRect.bottom <= containerRect.bottom + tolerance &&
          rangeRect.right <= containerRect.right

        if (isSelectionWithin) {
          shouldShowButton = true
          const buttonHeight = 30
          buttonTop = rangeRect.top - containerRect.top - buttonHeight - 5
          buttonLeft = Math.max(0, rangeRect.left - containerRect.left)
          textToQuote = selectedText
        }
      }
    }

    if (shouldShowButton) {
      quoteButtonState.value = {
        visible: true,
        top: buttonTop,
        left: buttonLeft,
        selectedText: textToQuote,
      }
    } else if (!isClickOnButton) {
      quoteButtonState.value = { visible: false, top: 0, left: 0, selectedText: '' }
    }
  }, 0)
}

const [icon, title] = computed(() => {
  switch (messageType.value) {
    case 'error':
      return [
        h('span', {
          class: 'i-codicon:error',
          style: { color: errorColor, marginBottom: '-1.5px' },
        }),
        h('span', { style: { color: errorColor, fontWeight: 'bold' } }, 'Error'),
      ]
    case 'mistake_limit_reached':
      return [
        h('span', {
          class: 'i-codicon:error',
          style: { color: errorColor, marginBottom: '-1.5px' },
        }),
        h('span', { style: { color: errorColor, fontWeight: 'bold' } }, 'Cline is having trouble...'),
      ]
    case 'auto_approval_max_req_reached':
      return [
        h('span', {
          class: 'i-codicon:warning',
          style: { color: errorColor, marginBottom: '-1.5px' },
        }),
        h('span', { style: { color: errorColor, fontWeight: 'bold' } }, 'Maximum Requests Reached'),
      ]
    case 'command':
      return [
        h('span', {
          class: 'i-codicon:terminal',
          style: { color: normalColor, marginBottom: '-1.5px' },
        }),
        h('span', { style: { color: normalColor, fontWeight: 'bold' } }, 'Cline wants to execute this command:'),
      ]
    case 'use_mcp_server': {
      const mcpServerUse = JSON.parse(props.message.text || '{}') as ClineAskUseMcpServer
      return [
        isMcpServerResponding.value
          ? h(ProgressIndicator)
          : h('span', {
              class: 'i-codicon:server',
              style: { color: normalColor, marginBottom: '-1.5px' },
            }),
        h('span', {
          class: 'ph-no-capture',
          style: { color: normalColor, fontWeight: 'bold', wordBreak: 'break-word' },
        }, [
          'Cline wants to ',
          mcpServerUse.type === 'use_mcp_tool' ? 'use a tool' : 'access a resource',
          ' on the ',
          h('code', { style: { wordBreak: 'break-all' } }, [
            getMcpServerDisplayName(mcpServerUse.serverName, mcpMarketplaceCatalog.value || { items: [] }),
          ]),
          ' MCP server:',
        ]),
      ]
    }
    case 'completion_result':
      return [
        h('span', {
          class: 'i-codicon:check',
          style: { color: successColor, marginBottom: '-1.5px' },
        }),
        h('span', { style: { color: successColor, fontWeight: 'bold' } }, 'Task Completed'),
      ]
    case 'api_req_started':
      return getErrorBlockTitle({
        cost: cost.value,
        apiReqCancelReason: apiReqCancelReason.value,
        apiRequestFailedMessage: apiRequestFailedMessage.value,
        retryStatus: retryStatus.value,
      })
    case 'followup':
      return [
        h('span', {
          class: 'i-codicon:question',
          style: { color: normalColor, marginBottom: '-1.5px' },
        }),
        h('span', { style: { color: normalColor, fontWeight: 'bold' } }, 'Cline has a question:'),
      ]
    default:
      return [null, null]
  }
}).value

const tool = computed(() => {
  if (props.message.ask === 'tool' || props.message.say === 'tool') {
    try {
      return JSON.parse(props.message.text || '{}') as ClineSayTool
    } catch {
      return null
    }
  }
  return null
})

const isImageFile = (filePath: string): boolean => {
  const imageExtensions = ['.png', '.jpg', '.jpeg', '.webp']
  const extension = filePath.toLowerCase().split('.').pop()
  return extension ? imageExtensions.includes(`.${extension}`) : false
}

// Command message processing
const splitMessage = (text: string) => {
  const outputIndex = text.indexOf(COMMAND_OUTPUT_STRING)
  if (outputIndex === -1) {
    return { command: text, output: '' }
  }
  return {
    command: text.slice(0, outputIndex).trim(),
    output: text
      .slice(outputIndex + COMMAND_OUTPUT_STRING.length)
      .trim()
      .split('')
      .map((char) => {
        switch (char) {
          case '\t':
            return '→   '
          case '\b':
            return '⌫'
          case '\f':
            return '⏏'
          case '\v':
            return '⇳'
          default:
            return char
        }
      })
      .join(''),
  }
}

const commandData = computed(() => {
  if (isCommandMessage.value) {
    return splitMessage(props.message.text || '')
  }
  return { command: '', output: '' }
})

const rawCommand = computed(() => commandData.value.command)
const output = computed(() => commandData.value.output)

const requestsApproval = computed(() => {
  return rawCommand.value.endsWith(COMMAND_REQ_APP_STRING)
})

const command = computed(() => {
  return requestsApproval.value
    ? rawCommand.value.slice(0, -COMMAND_REQ_APP_STRING.length)
    : rawCommand.value
})

const showCancelButton = computed(() => {
  return (
    (isCommandExecuting.value || isCommandPending.value) &&
    typeof props.onCancelCommand === 'function' &&
    vscodeTerminalExecutionMode.value === 'backgroundExec'
  )
})

const subagentPrompt = computed(() => {
  const clineCommandRegex = /^cline\s+"([^"]+)"(?:\s+--no-interactive)?/
  const match = command.value.match(clineCommandRegex)
  return match ? match[1] : undefined
})

const ClineIcon = () => {
  return h('svg', {
    height: 16,
    style: { marginBottom: '-1.5px' },
    viewBox: '0 0 92 96',
    width: 16,
  }, [
    h('g', { fill: 'currentColor' }, [
      h('path', {
        d: 'M65.4492701,16.3 C76.3374701,16.3 85.1635558,25.16479 85.1635558,36.1 L85.1635558,42.7 L90.9027661,54.1647464 C91.4694141,55.2966923 91.4668177,56.6300535 90.8957658,57.7597839 L85.1635558,69.1 L85.1635558,75.7 C85.1635558,86.63554 76.3374701,95.5 65.4492701,95.5 L26.0206986,95.5 C15.1328272,95.5 6.30641291,86.63554 6.30641291,75.7 L6.30641291,69.1 L0.448507752,57.7954874 C-0.14693501,56.6464093 -0.149634367,55.2802504 0.441262896,54.1288283 L6.30641291,42.7 L6.30641291,36.1 C6.30641291,25.16479 15.1328272,16.3 26.0206986,16.3 L65.4492701,16.3 Z M62.9301895,22 L29.189529,22 C19.8723267,22 12.3191987,29.5552188 12.3191987,38.875 L12.3191987,44.5 L7.44288578,53.9634655 C6.84794449,55.1180686 6.85066096,56.4896598 7.45017099,57.6418974 L12.3191987,67 L12.3191987,72.625 C12.3191987,81.9450625 19.8723267,89.5 29.189529,89.5 L62.9301895,89.5 C72.2476729,89.5 79.8005198,81.9450625 79.8005198,72.625 L79.8005198,67 L84.5682187,57.6061395 C85.1432011,56.473244 85.1458141,55.1345713 84.5752587,53.9994398 L79.8005198,44.5 L79.8005198,38.875 C79.8005198,29.5552188 72.2476729,22 62.9301895,22 Z',
      }),
      h('ellipse', { cx: '45.7349843', cy: '11', rx: '12', ry: '14' }),
      h('ellipse', { cx: '33.5', cy: '55.5', rx: '8', ry: '9' }),
      h('ellipse', { cx: '57.5', cy: '55.5', rx: '8', ry: '9' }),
    ]),
  ])
}

const displayIcon = computed(() => {
  return h('span', { style: { color: normalColor } }, [ClineIcon()])
})

const displayTitle = computed(() => {
  return h('span', { style: { color: normalColor, fontWeight: 'bold' } }, [
    'Cline wants to use a subagent:',
  ])
})

// Reset output expansion state when command stops
watch(
  [isCommandMessage, isCommandExecuting],
  ([isCmdMsg, isExecuting]) => {
    if (isCmdMsg && prevCommandExecutingRef.value && !isExecuting) {
      isOutputFullyExpanded.value = false
    }
    prevCommandExecutingRef.value = isExecuting
  },
  { immediate: true }
)

// Auto-expand when command starts executing (only if running > 500ms)
watch(
  [isCommandMessage, isCommandExecuting, () => props.isExpanded],
  ([isCmdMsg, isExecuting, isExp]) => {
    if (isCmdMsg && isExecuting && !isExp) {
      const timer = setTimeout(() => {
        props.onToggleExpand(props.message.ts)
      }, 500)
      return () => clearTimeout(timer)
    }
  }
)

const handleCancelCommand = (e: MouseEvent) => {
  e.stopPropagation()
  if (vscodeTerminalExecutionMode.value === 'backgroundExec') {
    props.onCancelCommand?.()
  } else {
    alert(
      'This command is running in the VSCode terminal. You can manually stop it using Ctrl+C in the terminal, or switch to Background Execution mode in settings for cancellable commands.'
    )
  }
}

const handleReadFileClick = (content: string) => {
  fileService.openFile(content).catch((err: any) =>
    console.error('Failed to open file:', err)
  )
}

const handleOpenUrl = (url: string) => {
  window.open(url, '_blank')
}

const handleSeeNewChanges = () => {
  seeNewChangesDisabled.value = true
  taskService.taskCompletionViewChanges(
    props.message.ts
  ).catch((err: any) => console.error('Failed to show task completion view changes:', err))
}

// Completion result processing
const completionResultHasChanges = computed(() => {
  return props.message.text?.endsWith(COMPLETION_RESULT_CHANGES_FLAG) ?? false
})

const completionResultText = computed(() => {
  const hasChanges = completionResultHasChanges.value
  return hasChanges
    ? props.message.text?.slice(0, -COMPLETION_RESULT_CHANGES_FLAG.length)
    : props.message.text
})

const askCompletionResultHasChanges = computed(() => {
  if (props.message.ask === 'completion_result' && props.message.text) {
    return props.message.text.endsWith(COMPLETION_RESULT_CHANGES_FLAG) ?? false
  }
  return false
})

const askCompletionResultText = computed(() => {
  if (props.message.ask === 'completion_result' && props.message.text) {
    const hasChanges = askCompletionResultHasChanges.value
    return hasChanges
      ? props.message.text.slice(0, -COMPLETION_RESULT_CHANGES_FLAG.length)
      : props.message.text
  }
  return ''
})

// Followup processing
const followupData = computed(() => {
  if (props.message.ask === 'followup') {
    try {
      return JSON.parse(props.message.text || '{}') as ClineAskQuestion
    } catch {
      return {
        question: props.message.text,
        options: undefined,
        selected: undefined,
      }
    }
  }
  return { question: undefined, options: undefined, selected: undefined }
})

const followupQuestion = computed(() => followupData.value.question)
const followupOptions = computed(() => followupData.value.options)
const followupSelected = computed(() => followupData.value.selected)

// Plan mode response processing
const planModeData = computed(() => {
  if (props.message.ask === 'plan_mode_respond') {
    try {
      return JSON.parse(props.message.text || '{}') as ClinePlanModeResponse
    } catch {
      return {
        response: props.message.text,
        options: undefined,
        selected: undefined,
      }
    }
  }
  return { response: undefined, options: undefined, selected: undefined }
})

const planModeResponse = computed(() => planModeData.value.response)
const planModeOptions = computed(() => planModeData.value.options)
const planModeSelected = computed(() => planModeData.value.selected)

// use_mcp_server processing
const useMcpServer = computed(() => {
  if (props.message.ask === 'use_mcp_server' || props.message.say === 'use_mcp_server') {
    try {
      return JSON.parse(props.message.text || '{}') as ClineAskUseMcpServer
    } catch {
      return null
    }
  }
  return null
})

const server = computed(() => {
  if (useMcpServer.value) {
    return mcpServers.value.find((s: McpServer) => s.name === useMcpServer.value?.serverName) as McpServer | undefined
  }
  return null
})

// user_feedback_diff processing
const feedbackDiffTool = computed(() => {
  if (props.message.say === 'user_feedback_diff') {
    try {
      return JSON.parse(props.message.text || '{}') as ClineSayTool
    } catch {
      return null
    }
  }
  return null
})
</script>

