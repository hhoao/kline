package com.hhoa.kline.core.core.shared;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClineAskQuestion {
    private String question;
    private List<String> options;
    private String selected;
}
