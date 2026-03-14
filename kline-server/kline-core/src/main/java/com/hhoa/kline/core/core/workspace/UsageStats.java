package com.hhoa.kline.core.core.workspace;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsageStats {

    private int count;

    private List<String> examples = new ArrayList<>();

    private LocalDateTime lastUsed;

    public UsageStats(int count, LocalDateTime lastUsed) {
        this.count = count;
        this.lastUsed = lastUsed;
        this.examples = new ArrayList<>();
    }
}
