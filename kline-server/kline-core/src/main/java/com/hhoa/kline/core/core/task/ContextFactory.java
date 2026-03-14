package com.hhoa.kline.core.core.task;

import reactor.util.context.Context;
import reactor.util.context.ContextView;

/**
 * ContextFactory
 *
 * @author xianxing
 * @since 2026/2/12
 */
public interface ContextFactory {
    Context modifyContext(Context context);

    void runWithContext(ContextView context, Runnable runnable);
}
