package com.hhoa.kline.core.common.utils;

import java.util.Map;
import java.util.Set;

/** 脏数据追踪的 Map EntrySet 包装类 */
public class DirtyTrackingEntrySet<K, V> extends DirtyTrackingSet<Map.Entry<K, V>> {
    private final Runnable onDirty;

    public DirtyTrackingEntrySet(Set<Map.Entry<K, V>> delegate, Runnable onDirty) {
        super(delegate, onDirty);
        this.onDirty = onDirty;
    }

    @Override
    public java.util.Iterator<Map.Entry<K, V>> iterator() {
        return new DirtyTrackingEntryIterator<>(delegate.iterator(), onDirty);
    }
}
