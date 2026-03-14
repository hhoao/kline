package com.hhoa.kline.core.core.shared.proto.cline;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskHistoryArray {
    @Builder.Default private List<TaskItem> tasks = new ArrayList<>();
    @Builder.Default private int totalCount = 0;
}
