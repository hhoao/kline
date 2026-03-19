package com.hhoa.kline.core.core.task.event;

import com.hhoa.kline.core.core.task.ApiRequestResult;
import lombok.Getter;

/** API 调用及工具处理全部完成事件。携带原始 API 结果和工具处理结果。 仅在所有内容块（文本 + 工具）处理完毕后才分发。 */
@Getter
public class ApiCompletedEvent extends TaskEvent {

    private final ApiRequestResult apiRequestResult;

    public ApiCompletedEvent(String taskId, ApiRequestResult apiRequestResult) {
        super(TaskEventType.API_COMPLETED, taskId);
        this.apiRequestResult = apiRequestResult;
    }
}
