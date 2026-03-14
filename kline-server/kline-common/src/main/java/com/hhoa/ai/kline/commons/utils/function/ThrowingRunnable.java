package com.hhoa.ai.kline.commons.utils.function;

import com.hhoa.ai.kline.commons.utils.ExceptionUtils;

/**
 * Similar to a {@link Runnable}, this interface is used to capture a block of code to be executed.
 * In contrast to {@code Runnable}, this interface allows throwing checked exceptions.
 */
@FunctionalInterface
public interface ThrowingRunnable<E extends Throwable> {

    /**
     * The work method.
     *
     * @throws E Exceptions may be thrown.
     */
    void run() throws E;

    /**
     * Converts a {@link ThrowingRunnable} into a {@link Runnable} which throws all checked
     * exceptions as unchecked.
     *
     * @param throwingRunnable to convert into a {@link Runnable}
     * @return {@link Runnable} which throws all checked exceptions as unchecked.
     */
    static Runnable unchecked(ThrowingRunnable<?> throwingRunnable) {
        return () -> {
            try {
                throwingRunnable.run();
            } catch (Throwable t) {
                ExceptionUtils.rethrow(t);
            }
        };
    }
}
