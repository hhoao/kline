package com.hhoa.kline.core.core.controller;

import com.hhoa.kline.core.core.assistant.MessageParam;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskWithIdResult {
    private HistoryItem historyItem;

    private List<MessageParam> apiConversationHistory;
}
