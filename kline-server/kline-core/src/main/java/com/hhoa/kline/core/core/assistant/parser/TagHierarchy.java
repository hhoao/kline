package com.hhoa.kline.core.core.assistant.parser;

import java.util.Deque;

/**
 * 标签层级查询接口：判断某个标签在给定上下文中是否为结构标签。
 *
 * @author hhoa
 * @see TagHierarchyConfig
 */
public interface TagHierarchy {

    /**
     * 在给定的标签栈下，tagName 是否是结构标签
     *
     * @param tagName 标签名
     * @param tagStack 当前标签栈（栈顶为当前最内层的父标签）
     */
    boolean isStructuralTag(String tagName, Deque<String> tagStack);

    /**
     * 在给定的标签栈下，prefix 是否可能是某个结构标签名的前缀
     *
     * @param prefix 待检查的前缀
     * @param tagStack 当前标签栈
     */
    boolean isPossiblePrefix(String prefix, Deque<String> tagStack);
}
