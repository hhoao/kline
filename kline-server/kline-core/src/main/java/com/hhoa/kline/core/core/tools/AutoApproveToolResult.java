package com.hhoa.kline.core.core.tools;

import java.util.Objects;
import lombok.Getter;

/** 工具自动批准结果：单布尔或 (local/safe, external/all) 二元组 */
@Getter
public final class AutoApproveToolResult {
    private final boolean single;
    private final boolean first;
    private final boolean second;

    private AutoApproveToolResult(boolean single, boolean first, boolean second) {
        this.single = single;
        this.first = first;
        this.second = second;
    }

    public static AutoApproveToolResult of(boolean value) {
        return new AutoApproveToolResult(true, value, false);
    }

    public static AutoApproveToolResult of(boolean first, boolean second) {
        return new AutoApproveToolResult(false, first, second);
    }

    public boolean asBoolean() {
        return single ? first : (first || second);
    }

    public boolean getFirst() {
        return first;
    }

    public boolean getSecond() {
        return second;
    }

    public boolean isPair() {
        return !single;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AutoApproveToolResult that = (AutoApproveToolResult) o;
        return single == that.single && first == that.first && second == that.second;
    }

    @Override
    public int hashCode() {
        return Objects.hash(single, first, second);
    }
}
