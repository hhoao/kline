package com.hhoa.kline.core.prompts;

import com.hhoa.kline.core.core.task.ApiChunk;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
public abstract class FixedVersionPromptsTestBase extends PromptsTestBase {
    public abstract int getTemplateArgsGroup();

    public abstract int getHistoryGroup();

    public String getSystemPromptsVersion() {
        return "latest";
    }

    public String getTemplatesVersion() {
        return "latest";
    }

    public String getAssistantMessageVersion() {
        return "latest";
    }

    protected ApiChunk executeTest(String historyDataNumber) throws IOException {
        TestPromptsParams params =
                TestPromptsParams.builder()
                        .systemPromptsVersion(getSystemPromptsVersion())
                        .templatesVersion(getTemplatesVersion())
                        .assistantMessageVersion(getAssistantMessageVersion())
                        .historyGroup(getHistoryGroup())
                        .historyNumber(historyDataNumber)
                        .templateArgsGroup(getTemplateArgsGroup())
                        .build();
        return executeTest(params);
    }
}
