package com.hhoa.kline.core.core.api;

import java.util.function.Supplier;

public interface TaskContext {

    Long getTaskManagerId();

    void run(Runnable r);

    <T> T run(Supplier<T> supplier);
}
