package com.hhoa.kline.core.core.task.statemachine;

public interface Recoverable<State> {
    void recover(State state) throws Exception;
}
