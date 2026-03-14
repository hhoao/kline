package com.hhoa.kline.core.common.utils;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/** 脏数据追踪的 Map 包装类 */
public class DirtyTrackingMap<K, V> implements Map<K, V> {
    private final Map<K, V> delegate;
    private final Runnable onDirty;

    public DirtyTrackingMap(Map<K, V> delegate, Runnable onDirty) {
        this.delegate = delegate;
        this.onDirty = onDirty;
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return delegate.get(key);
    }

    @Override
    public V put(K key, V value) {
        V result = delegate.put(key, value);
        onDirty.run();
        return result;
    }

    @Override
    public V remove(Object key) {
        V result = delegate.remove(key);
        if (result != null || delegate.containsKey(key)) {
            onDirty.run();
        }
        return result;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        if (!m.isEmpty()) {
            delegate.putAll(m);
            onDirty.run();
        }
    }

    @Override
    public void clear() {
        if (!delegate.isEmpty()) {
            delegate.clear();
            onDirty.run();
        }
    }

    @Override
    public Set<K> keySet() {
        // keySet 是 Map 的视图，修改会反映到原 Map，所以直接返回原视图
        // 但为了监听通过 keySet 的修改，我们需要包装
        return new DirtyTrackingSet<>(delegate.keySet(), onDirty);
    }

    @Override
    public Collection<V> values() {
        // values 是 Map 的视图，修改会反映到原 Map
        return new DirtyTrackingCollection<>(delegate.values(), onDirty);
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        // entrySet 是 Map 的视图，修改会反映到原 Map
        return new DirtyTrackingEntrySet<>(delegate.entrySet(), onDirty);
    }

    @Override
    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
