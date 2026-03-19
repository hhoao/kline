package com.hhoa.kline.core.core.task.deps;

import com.hhoa.kline.core.core.task.event.TaskEvent;

@FunctionalInterface
public interface TaskDispatch {

    void dispatch(TaskEvent event);
}
