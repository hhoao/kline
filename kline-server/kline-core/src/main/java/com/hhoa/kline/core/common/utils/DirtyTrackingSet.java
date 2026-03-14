package com.hhoa.kline.core.common.utils;

import java.util.Set;

/** 脏数据追踪的 Set 包装类 */
public class DirtyTrackingSet<E> extends DirtyTrackingCollection<E> implements Set<E> {
    public DirtyTrackingSet(Set<E> delegate, Runnable onDirty) {
        super(delegate, onDirty);
    }
}
