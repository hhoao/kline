package com.hhoa.kline.core.core.task.statemachine;

public interface StateMachine<STATE extends Enum<STATE>, EVENTTYPE extends Enum<EVENTTYPE>, EVENT> {
    STATE getCurrentState();

    STATE getPreviousState();

    STATE doTransition(EVENTTYPE eventType, EVENT event) throws InvalidStateTransitionException;
}
