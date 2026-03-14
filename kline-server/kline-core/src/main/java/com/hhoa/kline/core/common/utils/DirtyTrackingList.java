package com.hhoa.kline.core.common.utils;

import java.util.Collection;
import java.util.List;

/** 脏数据追踪的 List 包装类 */
public class DirtyTrackingList<E> extends DirtyTrackingCollection<E> implements List<E> {
    private final List<E> delegate;

    public DirtyTrackingList(List<E> delegate, Runnable onDirty) {
        super(delegate, onDirty);
        this.delegate = delegate;
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        boolean result = delegate.addAll(index, c);
        if (result) {
            onDirty.run();
        }
        return result;
    }

    @Override
    public E get(int index) {
        return delegate.get(index);
    }

    @Override
    public E set(int index, E element) {
        E result = delegate.set(index, element);
        onDirty.run();
        return result;
    }

    @Override
    public void add(int index, E element) {
        delegate.add(index, element);
        onDirty.run();
    }

    @Override
    public E remove(int index) {
        E result = delegate.remove(index);
        onDirty.run();
        return result;
    }

    @Override
    public int indexOf(Object o) {
        return delegate.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return delegate.lastIndexOf(o);
    }

    @Override
    public java.util.ListIterator<E> listIterator() {
        return new DirtyTrackingListIterator<>(delegate.listIterator(), onDirty);
    }

    @Override
    public java.util.ListIterator<E> listIterator(int index) {
        return new DirtyTrackingListIterator<>(delegate.listIterator(index), onDirty);
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return new DirtyTrackingList<>(delegate.subList(fromIndex, toIndex), onDirty);
    }
}
