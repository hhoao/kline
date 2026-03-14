package com.hhoa.kline.core.core.task;

import com.hhoa.kline.core.core.shared.ClineAskResponse;
import java.util.List;
import lombok.Data;

@Data
public class AskResult {
    private ClineAskResponse response;
    private String text;
    private List<String> images;
    private List<String> files;
}
