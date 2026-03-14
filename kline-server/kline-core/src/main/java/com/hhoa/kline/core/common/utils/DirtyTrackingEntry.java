package com.hhoa.kline.core.common.utils;

import java.util.Map;

/** 脏数据追踪的 Map Entry 包装类 */
public class DirtyTrackingEntry<K, V> implements Map.Entry<K, V> {
    private final Map.Entry<K, V> delegate;
    private final Runnable onDirty;

    public DirtyTrackingEntry(Map.Entry<K, V> delegate, Runnable onDirty) {
        this.delegate = delegate;
        this.onDirty = onDirty;
    }

    @Override
    public K getKey() {
        return delegate.getKey();
    }

    @Override
    public V getValue() {
        return delegate.getValue();
    }

    @Override
    public V setValue(V value) {
        V result = delegate.setValue(value);
        onDirty.run();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }
}
