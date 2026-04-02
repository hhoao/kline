package com.hhoa.kline.core.core.task.transition;

import com.hhoa.kline.core.core.task.TaskV2;
import com.hhoa.kline.core.core.task.event.PrepareContextEvent;
import com.hhoa.kline.core.core.task.event.StartTaskEvent;
import com.hhoa.kline.core.core.task.event.TaskEvent;
import com.hhoa.kline.core.core.task.statemachine.SingleArcTransition;

public class StartTaskTransition implements SingleArcTransition<TaskV2, TaskEvent> {

    @Override
    public void transition(TaskV2 operand, TaskEvent event) {
        StartTaskEvent startTaskEvent = (StartTaskEvent) event;
        boolean shouldContinue =
                operand.getStartTaskHandler()
                        .startTask(
                                startTaskEvent.getTaskText(),
                                startTaskEvent.getImages(),
                                startTaskEvent.getFiles());
        if (shouldContinue) {
            operand.handle(new PrepareContextEvent(operand.getTaskId()));
        } else {
            // Hook cancelled the task — let cancelTask handle cleanup
            operand.getCancelTask().run();
        }
    }
}
