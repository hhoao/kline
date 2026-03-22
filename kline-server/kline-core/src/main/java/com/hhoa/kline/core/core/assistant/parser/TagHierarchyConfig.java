package com.hhoa.kline.core.core.assistant.parser;

import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 分层标签配置：定义哪些标签在不同层级是"结构标签"。
 *
 * <p>结构标签由解析器识别并触发事件回调，非结构标签原样作为文本输出。
 *
 * @author hhoa
 */
public class TagHierarchyConfig implements TagHierarchy {

    private final Set<String> rootTags;
    private final Map<String, Set<String>> childTags;

    private TagHierarchyConfig(Set<String> rootTags, Map<String, Set<String>> childTags) {
        this.rootTags = Set.copyOf(rootTags);
        Map<String, Set<String>> immutable = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : childTags.entrySet()) {
            immutable.put(entry.getKey(), Set.copyOf(entry.getValue()));
        }
        this.childTags = Collections.unmodifiableMap(immutable);
    }

    @Override
    public boolean isStructuralTag(String tagName, Deque<String> tagStack) {
        return getExpectedTags(tagStack).contains(tagName);
    }

    @Override
    public boolean isPossiblePrefix(String prefix, Deque<String> tagStack) {
        if (prefix.isEmpty()) {
            return true;
        }
        for (String tag : getExpectedTags(tagStack)) {
            if (tag.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> getExpectedTags(Deque<String> tagStack) {
        if (tagStack.isEmpty()) {
            return rootTags;
        }
        return childTags.getOrDefault(tagStack.peek(), Collections.emptySet());
    }

    public Set<String> getRootTags() {
        return rootTags;
    }

    public Map<String, Set<String>> getChildTags() {
        return childTags;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Set<String> rootTags = new HashSet<>();
        private final Map<String, Set<String>> childTags = new HashMap<>();

        public Builder rootTag(String tag) {
            rootTags.add(tag);
            return this;
        }

        public Builder rootTags(Set<String> tags) {
            rootTags.addAll(tags);
            return this;
        }

        public Builder childTags(String parent, Set<String> children) {
            childTags.computeIfAbsent(parent, k -> new HashSet<>()).addAll(children);
            return this;
        }

        public TagHierarchyConfig build() {
            return new TagHierarchyConfig(rootTags, childTags);
        }
    }
}
