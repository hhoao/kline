package com.hhoa.kline.core.common.utils;

import java.util.Map;

/** 脏数据追踪的 Entry Iterator 包装类 */
public class DirtyTrackingEntryIterator<K, V> implements java.util.Iterator<Map.Entry<K, V>> {
    private final java.util.Iterator<Map.Entry<K, V>> delegate;
    private final Runnable onDirty;

    public DirtyTrackingEntryIterator(
            java.util.Iterator<Map.Entry<K, V>> delegate, Runnable onDirty) {
        this.delegate = delegate;
        this.onDirty = onDirty;
    }

    @Override
    public boolean hasNext() {
        return delegate.hasNext();
    }

    @Override
    public Map.Entry<K, V> next() {
        Map.Entry<K, V> entry = delegate.next();
        // 包装 Entry，监听 setValue 操作
        return new DirtyTrackingEntry<>(entry, onDirty);
    }

    @Override
    public void remove() {
        delegate.remove();
        onDirty.run();
    }
}
