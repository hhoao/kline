package com.hhoa.kline.core.core.task.statemachine;

/**
 * Hook for Transition. Post state is decided by Transition hook. Post state must be one of the
 * valid post states registered in StateMachine.
 */
public interface MultipleArcTransition<OPERAND, EVENT, STATE extends Enum<STATE>> {

    /**
     * Transition hook.
     *
     * @return the postState. Post state must be one of the valid post states registered in
     *     StateMachine.
     * @param operand the entity attached to the FSM, whose internal state may change.
     * @param event causal event
     */
    public STATE transition(OPERAND operand, EVENT event);
}
