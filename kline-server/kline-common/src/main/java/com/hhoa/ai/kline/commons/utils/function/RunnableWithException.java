package com.hhoa.ai.kline.commons.utils.function;

/**
 * Similar to a {@link Runnable}, this interface is used to capture a block of code to be executed.
 * In contrast to {@code Runnable}, this interface allows throwing checked exceptions.
 */
@FunctionalInterface
public interface RunnableWithException extends ThrowingRunnable<Exception> {

    /**
     * The work method.
     *
     * @throws Exception Exceptions may be thrown.
     */
    @Override
    void run() throws Exception;
}
