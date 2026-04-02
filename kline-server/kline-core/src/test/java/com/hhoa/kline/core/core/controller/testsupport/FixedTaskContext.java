package com.hhoa.kline.core.core.controller.testsupport;

import com.hhoa.kline.core.core.api.TaskContext;
import java.util.function.Supplier;

public final class FixedTaskContext implements TaskContext {

    private final Long id;

    public FixedTaskContext(long id) {
        this.id = id;
    }

    @Override
    public Long getTaskManagerId() {
        return id;
    }

    @Override
    public void run(Runnable r) {
        r.run();
    }

    @Override
    public <T> T run(Supplier<T> supplier) {
        return supplier.get();
    }
}
