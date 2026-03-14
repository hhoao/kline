package com.hhoa.kline.core.prompts;

import com.hhoa.kline.core.core.task.ApiChunk;
import java.io.IOException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

/**
 * 测试补全截断内容的测试类
 *
 * <p>测试当AI生成内容因maxTokens限制被截断时，如何继续补全剩余部分。
 */
@Slf4j
@Getter
public class TestContinuePartialContentPrompts extends FixedVersionPromptsTestBase {
    private final int historyGroup = 1;
    private final int templateArgsGroup = 1;
    private final String systemPromptsVersion = "latest";
    private final String templatesVersion = "latest";
    private final String assistantMessageVersion = "latest";

    @Test
    void test1() throws IOException {
        ApiChunk result = executeTest("1");
    }

    @Test
    void test2() throws IOException {
        ApiChunk result = executeTest("2");
    }

    @Test
    void test3() throws IOException {
        ApiChunk result = executeTest("3");
    }

    @Test
    void test4() throws IOException {
        ApiChunk result = executeTest("4");
    }

    @Override
    public String getType() {
        return "complete-truncated-content";
    }
}
