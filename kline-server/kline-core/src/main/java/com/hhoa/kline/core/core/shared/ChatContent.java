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
public class ChatContent {
    private String message;
    private List<String> images;
    private List<String> files;
}
