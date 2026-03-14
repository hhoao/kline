package com.hhoa.kline.core.common.utils;

/** 脏数据追踪的 ListIterator 包装类 */
public class DirtyTrackingListIterator<E> implements java.util.ListIterator<E> {
    private final java.util.ListIterator<E> delegate;
    private final Runnable onDirty;

    public DirtyTrackingListIterator(java.util.ListIterator<E> delegate, Runnable onDirty) {
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
    public boolean hasPrevious() {
        return delegate.hasPrevious();
    }

    @Override
    public E previous() {
        return delegate.previous();
    }

    @Override
    public int nextIndex() {
        return delegate.nextIndex();
    }

    @Override
    public int previousIndex() {
        return delegate.previousIndex();
    }

    @Override
    public void remove() {
        delegate.remove();
        onDirty.run();
    }

    @Override
    public void set(E e) {
        delegate.set(e);
        onDirty.run();
    }

    @Override
    public void add(E e) {
        delegate.add(e);
        onDirty.run();
    }
}
