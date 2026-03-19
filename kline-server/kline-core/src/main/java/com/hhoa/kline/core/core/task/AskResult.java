package com.hhoa.kline.core.core.task;

import com.hhoa.kline.core.core.shared.ClineAskResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AskResult {
    private ClineAskResponse response;
    private String text;
    private List<String> images;
    private String pendingId;
    private List<String> files;
}
