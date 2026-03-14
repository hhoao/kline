package com.hhoa.kline.core.common.utils;

/** 脏数据追踪的 Iterator 包装类 */
public class DirtyTrackingIterator<E> implements java.util.Iterator<E> {
    private final java.util.Iterator<E> delegate;
    private final Runnable onDirty;

    public DirtyTrackingIterator(java.util.Iterator<E> delegate, Runnable onDirty) {
        this.delegate = delegate;
        this.onDirty = onDirty;
    }

    @Override
    public boolean hasNext() {
        return delegate.hasNext();
    }

    @Override
    public E next() {
        return delegate.next();
    }

    @Override
    public void remove() {
        delegate.remove();
        onDirty.run();
    }
}
