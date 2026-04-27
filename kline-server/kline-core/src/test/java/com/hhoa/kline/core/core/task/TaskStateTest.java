package com.hhoa.kline.core.core.task;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hhoa.kline.core.core.assistant.TextContent;
import com.hhoa.kline.core.core.assistant.TextContentBlock;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class TaskStateTest {

    @Test
    void resetForNewApiRequestClearsTurnScopedPresentationAndToolState() {
        TaskState state = new TaskState();
        state.getPresentationState().setAssistantMessageContent(new ArrayList<>());
        state.getPresentationState()
                .getAssistantMessageContent()
                .add(new TextContent("old", false));
        state.getPresentationState().setCurrentStreamingContentIndex(3);
        state.getStreamState().setDidCompleteReadingStream(true);
        state.getPresentationState().setNextUserMessageContent(new ArrayList<>());
        state.getPresentationState()
                .getNextUserMessageContent()
                .add(new TextContentBlock("old result"));
        state.getPresentationState().setUserMessageContentReady(true);
        state.getToolExecutionState().setDidRejectTool(true);
        state.getToolExecutionState().setDidAlreadyUseTool(true);
        state.getApiTurnState().setDidAutomaticallyRetryFailedApiRequest(true);
        state.markNativeToolCallsPresent();

        state.resetForNewApiRequest();

        assertTrue(state.getPresentationState().getAssistantMessageContent().isEmpty());
        assertTrue(state.getPresentationState().getNextUserMessageContent().isEmpty());
        assertFalse(state.getPresentationState().isUserMessageContentReady());
        assertFalse(state.getStreamState().isDidCompleteReadingStream());
        assertFalse(state.getToolExecutionState().isDidRejectTool());
        assertFalse(state.getToolExecutionState().isDidAlreadyUseTool());
        assertFalse(state.getApiTurnState().isDidAutomaticallyRetryFailedApiRequest());
        assertFalse(state.getToolExecutionState().isUseNativeToolCalls());
    }

    @Test
    void nativeToolCallsAreMarkedExplicitly() {
        TaskState state = new TaskState();

        assertFalse(state.getToolExecutionState().isUseNativeToolCalls());

        state.markNativeToolCallsPresent();

        assertTrue(state.getToolExecutionState().isUseNativeToolCalls());
    }
}
