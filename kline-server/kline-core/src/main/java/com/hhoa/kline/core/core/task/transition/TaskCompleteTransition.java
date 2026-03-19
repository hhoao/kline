package com.hhoa.kline.core.core.task.transition;

import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.task.TaskV2;
import com.hhoa.kline.core.core.task.event.StartTaskEvent;
import com.hhoa.kline.core.core.task.event.TaskEvent;
import com.hhoa.kline.core.core.task.statemachine.SingleArcTransition;
import java.util.Queue;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TaskCompleteTransition implements SingleArcTransition<TaskV2, TaskEvent> {

    @Override
    public void transition(TaskV2 operand, TaskEvent event) {
        log.info("Task {} completed", operand.getTaskId());
        Queue<AskResult> pendingUserResponses = operand.getTaskState().getPendingUserResponses();
        if (!pendingUserResponses.isEmpty()) {
            AskResult askResult = pendingUserResponses.poll();
            operand.handle(
                    new StartTaskEvent(
                            operand.getTaskId(),
                            askResult.getText(),
                            askResult.getImages(),
                            askResult.getFiles()));
        }
        operand.getAbortHandler().abort();
    }
}
