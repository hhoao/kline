package com.hhoa.kline.core.core.controller;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author hhoa
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryItem {
    private String id;

    private String ulid;

    private Long ts;

    private String task;

    private Integer tokensIn;

    private Integer tokensOut;

    private Integer cacheWrites;

    private Integer cacheReads;

    private Double totalCost;

    private Long size;

    private String shadowGitConfigWorkTree;

    private String cwdOnTaskInitialization;

    private int[] conversationHistoryDeletedRange;

    private Boolean favorited;

    private String checkpointManagerErrorMessage;
}
