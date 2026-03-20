package com.hhoa.kline.core.core.task;

public interface ExistState {
    record Success() implements ExistState {}

    record Abort() implements ExistState {}

    record Failed(String message, Throwable throwable) implements ExistState {}

    record ContextWindowExceeded() implements ExistState {}
}
