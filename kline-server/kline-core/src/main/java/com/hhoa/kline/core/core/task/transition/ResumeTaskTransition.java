package com.hhoa.kline.core.core.task.transition;

import com.hhoa.kline.core.core.task.TaskV2;
import com.hhoa.kline.core.core.task.event.PrepareContextEvent;
import com.hhoa.kline.core.core.task.event.TaskEvent;
import com.hhoa.kline.core.core.task.statemachine.SingleArcTransition;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResumeTaskTransition implements SingleArcTransition<TaskV2, TaskEvent> {

    @Override
    public void transition(TaskV2 operand, TaskEvent event) {
        log.info("Resuming task {} from history", operand.getTaskId());
        operand.getResumeHandler().resumeTaskFromHistory();
        operand.handle(new PrepareContextEvent(event.getTaskId()));
    }
}
