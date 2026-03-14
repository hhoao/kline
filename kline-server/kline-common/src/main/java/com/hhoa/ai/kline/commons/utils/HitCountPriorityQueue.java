package com.hhoa.ai.kline.commons.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 基于命中数计数的优先队列工具类 支持线程安全操作，根据元素访问频率动态调整优先级
 *
 * @param <T> 队列中元素的类型
 */
public class HitCountPriorityQueue<T> {

    private final TreeSet<Node<T>> sortedNodes;
    private final Map<T, Node<T>> nodeMap;
    private final ReentrantReadWriteLock lock;
    private final int maxSize;
    private final boolean enableTimeDecay;

    /**
     * 构造函数
     *
     * @param maxSize 队列最大容量，-1表示无限制
     * @param enableTimeDecay 是否启用时间衰减（长时间未访问的元素会降低优先级）
     */
    public HitCountPriorityQueue(int maxSize, boolean enableTimeDecay) {
        this.maxSize = maxSize;
        this.enableTimeDecay = enableTimeDecay;
        this.sortedNodes = new TreeSet<>();
        this.nodeMap = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
    }

    /** 构造函数，默认无容量限制，启用时间衰减 */
    public HitCountPriorityQueue() {
        this(-1, true);
    }

    /** 构造函数，指定容量，启用时间衰减 */
    public HitCountPriorityQueue(int maxSize) {
        this(maxSize, true);
    }

    /**
     * 添加元素到队列中，如果元素已存在则增加其命中数
     *
     * @param element 要添加的元素
     * @return 是否添加成功
     */
    public boolean offer(T element) {
        if (element == null) {
            throw new NullPointerException("Element cannot be null");
        }

        lock.writeLock().lock();
        try {
            Node<T> existingNode = nodeMap.get(element);
            if (existingNode != null) {
                // 元素已存在，更新命中数和优先级
                sortedNodes.remove(existingNode);
                existingNode.hit();
                sortedNodes.add(existingNode);
                return true;
            }

            // 检查容量限制
            if (maxSize > 0 && nodeMap.size() >= maxSize) {
                // 移除优先级最低的元素
                Node<T> lowest = sortedNodes.last();
                sortedNodes.remove(lowest);
                nodeMap.remove(lowest.element);
            }

            // 添加新元素
            Node<T> newNode = new Node<>(element);
            nodeMap.put(element, newNode);
            sortedNodes.add(newNode);
            return true;

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取并移除符合条件的元素
     *
     * @param predicate 用于匹配元素的条件
     * @return 符合条件的元素，队列为空时返回null
     */
    public T get(Predicate<T> predicate) {
        lock.readLock().lock();
        Node<T> hitNode = null;
        try {
            for (Node<T> sortedNode : sortedNodes) {
                if (predicate.test(sortedNode.element)) {
                    hitNode = sortedNode;
                    break;
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        if (hitNode == null) {
            return null;
        }
        lock.writeLock().lock();
        try {
            sortedNodes.remove(hitNode);
            hitNode.hit();
            sortedNodes.add(hitNode);
        } finally {
            lock.writeLock().unlock();
        }
        return hitNode.element;
    }

    /**
     * 获取并移除优先级最高的元素
     *
     * @return 优先级最高的元素，队列为空时返回null
     */
    public T poll() {
        lock.writeLock().lock();
        try {
            if (sortedNodes.isEmpty()) {
                return null;
            }

            Node<T> highest = sortedNodes.first();
            sortedNodes.remove(highest);
            nodeMap.remove(highest.element);
            return highest.element;

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 查看优先级最高的元素但不移除
     *
     * @return 优先级最高的元素，队列为空时返回null
     */
    public T peek() {
        lock.readLock().lock();
        try {
            if (sortedNodes.isEmpty()) {
                return null;
            }
            return sortedNodes.first().element;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 访问指定元素，增加其命中数
     *
     * @param element 要访问的元素
     * @return 是否访问成功（元素存在）
     */
    public boolean access(T element) {
        if (element == null) {
            return false;
        }

        lock.writeLock().lock();
        try {
            Node<T> node = nodeMap.get(element);
            if (node != null) {
                sortedNodes.remove(node);
                node.hit();
                sortedNodes.add(node);
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 检查队列中是否包含指定元素
     *
     * @param element 要检查的元素
     * @return 是否包含该元素
     */
    public boolean contains(T element) {
        lock.readLock().lock();
        try {
            return nodeMap.containsKey(element);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 移除指定元素
     *
     * @param element 要移除的元素
     * @return 是否移除成功
     */
    public boolean remove(T element) {
        if (element == null) {
            return false;
        }

        lock.writeLock().lock();
        try {
            Node<T> node = nodeMap.remove(element);
            if (node != null) {
                sortedNodes.remove(node);
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取指定元素的命中次数
     *
     * @param element 要查询的元素
     * @return 命中次数，元素不存在时返回-1
     */
    public long getHitCount(T element) {
        lock.readLock().lock();
        try {
            Node<T> node = nodeMap.get(element);
            return node != null ? node.hitCount : -1;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 获取队列大小
     *
     * @return 队列中元素的数量
     */
    public int size() {
        lock.readLock().lock();
        try {
            return nodeMap.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 检查队列是否为空
     *
     * @return 队列是否为空
     */
    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return nodeMap.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }

    /** 清空队列 */
    public void clear() {
        lock.writeLock().lock();
        try {
            sortedNodes.clear();
            nodeMap.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取所有元素按优先级排序的列表
     *
     * @return 按优先级从高到低排序的元素列表
     */
    public List<T> toList() {
        lock.readLock().lock();
        try {
            return sortedNodes.stream().map(node -> node.element).collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 获取前N个优先级最高的元素
     *
     * @param n 要获取的元素数量
     * @return 前N个优先级最高的元素列表
     */
    public List<T> getTopN(int n) {
        if (n <= 0) {
            return new ArrayList<>();
        }

        lock.readLock().lock();
        try {
            return sortedNodes.stream()
                    .limit(n)
                    .map(node -> node.element)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 应用时间衰减，减少长时间未访问元素的优先级
     *
     * @param decayFactor 衰减因子 (0.0 - 1.0)
     * @param maxAgeMillis 最大年龄（毫秒），超过此时间的元素会被衰减
     */
    public void applyTimeDecay(double decayFactor, long maxAgeMillis) {
        if (!enableTimeDecay || decayFactor < 0 || decayFactor > 1) {
            return;
        }

        lock.writeLock().lock();
        try {
            long currentTime = System.currentTimeMillis();
            List<Node<T>> nodesToUpdate = new ArrayList<>();

            for (Node<T> node : sortedNodes) {
                long age = currentTime - node.lastAccessTime;
                if (age > maxAgeMillis) {
                    nodesToUpdate.add(node);
                }
            }

            for (Node<T> node : nodesToUpdate) {
                sortedNodes.remove(node);
                node.hitCount = (long) (node.hitCount * decayFactor);
                sortedNodes.add(node);
            }

        } finally {
            lock.writeLock().unlock();
        }
    }

    /** 内部节点类，存储元素和其命中计数 */
    private static class Node<T> implements Comparable<Node<T>> {
        T element;
        long hitCount;
        long lastAccessTime;
        long createTime;

        Node(T element) {
            this.element = element;
            this.hitCount = 0;
            this.lastAccessTime = System.currentTimeMillis();
            this.createTime = System.currentTimeMillis();
        }

        /** 增加命中次数 */
        void hit() {
            this.hitCount++;
            this.lastAccessTime = System.currentTimeMillis();
        }

        @Override
        public int compareTo(Node<T> other) {
            if (this == other) {
                return 0;
            }
            // 首先按命中数降序排序（命中数高的优先级高）
            int hitCompare = Long.compare(other.hitCount, this.hitCount);
            if (hitCompare != 0) {
                return hitCompare;
            }
            // 命中数相同时，按最近访问时间降序排序（最近访问的优先级高）
            int createCompare = Long.compare(other.lastAccessTime, this.lastAccessTime);
            if (createCompare != 0) {
                return createCompare;
            }
            return -1;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Node<?> node = (Node<?>) obj;
            return Objects.equals(element, node.element);
        }

        @Override
        public int hashCode() {
            return Objects.hash(element);
        }

        @Override
        public String toString() {
            return String.format(
                    "Node{element=%s, hitCount=%d, lastAccess=%d}",
                    element, hitCount, lastAccessTime);
        }
    }
}
