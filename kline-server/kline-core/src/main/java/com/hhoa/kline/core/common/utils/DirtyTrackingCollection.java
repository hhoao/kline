package com.hhoa.kline.core.common.utils;

import java.util.Collection;

/** 脏数据追踪的 Collection 包装类 */
public class DirtyTrackingCollection<E> implements Collection<E> {
    protected final Collection<E> delegate;
    protected final Runnable onDirty;

    public DirtyTrackingCollection(Collection<E> delegate, Runnable onDirty) {
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
    public boolean contains(Object o) {
        return delegate.contains(o);
    }

    @Override
    public java.util.Iterator<E> iterator() {
        return new DirtyTrackingIterator<>(delegate.iterator(), onDirty);
    }

    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return delegate.toArray(a);
    }

    @Override
    public boolean add(E e) {
        boolean result = delegate.add(e);
        if (result) {
            onDirty.run();
        }
        return result;
    }

    @Override
    public boolean remove(Object o) {
        boolean result = delegate.remove(o);
        if (result) {
            onDirty.run();
        }
        return result;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return delegate.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean result = delegate.addAll(c);
        if (result) {
            onDirty.run();
        }
        return result;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean result = delegate.removeAll(c);
        if (result) {
            onDirty.run();
        }
        return result;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean result = delegate.retainAll(c);
        if (result) {
            onDirty.run();
        }
        return result;
    }

    @Override
    public void clear() {
        if (!delegate.isEmpty()) {
            delegate.clear();
            onDirty.run();
        }
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
