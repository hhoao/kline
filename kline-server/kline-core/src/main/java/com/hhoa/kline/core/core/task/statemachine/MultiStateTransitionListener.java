package com.hhoa.kline.core.core.task.statemachine;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link StateTransitionListener} that dispatches the pre and post state transitions to multiple
 * registered listeners. NOTE: The registered listeners are called in a for loop. Clients should
 * know that a listener configured earlier might prevent a later listener from being called, if for
 * instance it throws an un-caught Exception.
 */
public abstract class MultiStateTransitionListener<OPERAND, EVENT, STATE extends Enum<STATE>>
        implements StateTransitionListener<OPERAND, EVENT, STATE> {

    private final List<StateTransitionListener<OPERAND, EVENT, STATE>> listeners =
            new ArrayList<>();

    /**
     * Add a listener to the list of listeners.
     *
     * @param listener A listener.
     */
    public void addListener(StateTransitionListener<OPERAND, EVENT, STATE> listener) {
        listeners.add(listener);
    }

    @Override
    public void preTransition(OPERAND op, STATE beforeState, EVENT eventToBeProcessed) {
        for (StateTransitionListener<OPERAND, EVENT, STATE> listener : listeners) {
            listener.preTransition(op, beforeState, eventToBeProcessed);
        }
    }

    @Override
    public void postTransition(
            OPERAND op, STATE beforeState, STATE afterState, EVENT processedEvent) {
        for (StateTransitionListener<OPERAND, EVENT, STATE> listener : listeners) {
            listener.postTransition(op, beforeState, afterState, processedEvent);
        }
    }
}
