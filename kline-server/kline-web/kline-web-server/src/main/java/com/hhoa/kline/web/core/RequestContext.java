package com.hhoa.kline.web.core;

import com.hhoa.kline.core.core.api.TaskContext;
import java.util.function.Supplier;
import lombok.Data;

@Data
public class RequestContext implements TaskContext {
    private final Long userId;

    public RequestContext() {
        this.userId = UserInfoContextHolder.getUserInfo().getUserId();
    }

    @Override
    public Long getTaskManagerId() {
        return userId;
    }

    public void run(Runnable r) {
        r.run();
    }

    public <T> T run(Supplier<T> supplier) {
        return supplier.get();
    }
}
