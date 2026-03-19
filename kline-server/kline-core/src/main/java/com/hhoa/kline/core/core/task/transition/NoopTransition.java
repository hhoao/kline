package com.hhoa.kline.core.core.task.transition;

import com.hhoa.kline.core.core.task.TaskV2;
import com.hhoa.kline.core.core.task.event.TaskEvent;
import com.hhoa.kline.core.core.task.statemachine.SingleArcTransition;

public class NoopTransition implements SingleArcTransition<TaskV2, TaskEvent> {

    @Override
    public void transition(TaskV2 operand, TaskEvent event) {}
}
