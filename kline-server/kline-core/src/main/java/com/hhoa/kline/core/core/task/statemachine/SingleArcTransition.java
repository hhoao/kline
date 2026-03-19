package com.hhoa.kline.core.core.task.statemachine;

/**
 * Hook for Transition. This lead to state machine to move to the post state as registered in the
 * state machine.
 */
public interface SingleArcTransition<OPERAND, EVENT> {
    /**
     * Transition hook.
     *
     * @param operand the entity attached to the FSM, whose internal state may change.
     * @param event causal event
     */
    void transition(OPERAND operand, EVENT event);
}
