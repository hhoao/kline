package com.hhoa.kline.core.core.task.statemachine;

import lombok.Getter;

/** The exception that happens when you call invalid state transition. */
@Getter
public class InvalidStateTransitionException extends RuntimeException {
    private static final long serialVersionUID = -6188669113571351684L;
    private Enum<?> currentState;
    private Enum<?> event;

    public InvalidStateTransitionException(Enum<?> currentState, Enum<?> event) {
        super("Invalid event: " + event + " at " + currentState);
        this.currentState = currentState;
        this.event = event;
    }
}
