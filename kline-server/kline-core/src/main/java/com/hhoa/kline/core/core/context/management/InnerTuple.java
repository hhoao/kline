package com.hhoa.kline.core.core.context.management;

import java.util.List;
import java.util.Map;
import lombok.Data;

/** 内部元组：[EditType, Map<blockIndex, ContextUpdate[]>] */
@Data
public class InnerTuple {
    private final EditType editType;
    private final Map<Integer, List<ContextUpdate>> innerMap;

    public InnerTuple(EditType editType, Map<Integer, List<ContextUpdate>> innerMap) {
        this.editType = editType;
        this.innerMap = innerMap;
    }

    public EditType getEditType() {
        return editType;
    }

    public Map<Integer, List<ContextUpdate>> getInnerMap() {
        return innerMap;
    }
}
