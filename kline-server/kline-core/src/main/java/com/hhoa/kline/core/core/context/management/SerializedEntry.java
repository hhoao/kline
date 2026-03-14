package com.hhoa.kline.core.core.context.management;

import com.fasterxml.jackson.annotation.JsonCreator;

/** 序列化的条目：[messageIndex, [EditType, Array<[blockIndex, ContextUpdate[]]>]>] */
public class SerializedEntry {
    private final int messageIndex;
    private final InnerTuple tuple;

    @JsonCreator
    public SerializedEntry(int messageIndex, InnerTuple tuple) {
        this.messageIndex = messageIndex;
        this.tuple = tuple;
    }

    public int getMessageIndex() {
        return messageIndex;
    }

    public InnerTuple getTuple() {
        return tuple;
    }
}
