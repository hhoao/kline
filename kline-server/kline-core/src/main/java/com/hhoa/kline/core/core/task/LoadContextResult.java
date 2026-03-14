package com.hhoa.kline.core.core.task;

import com.hhoa.kline.core.core.assistant.UserContentBlock;
import java.util.List;

public record LoadContextResult(
        List<UserContentBlock> processedUserContent,
        String environmentDetails,
        boolean clinerulesError) {}
