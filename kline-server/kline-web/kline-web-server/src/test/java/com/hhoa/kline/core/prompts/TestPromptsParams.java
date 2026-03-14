package com.hhoa.kline.core.prompts;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 测试参数类
 *
 * <p>使用Builder模式创建测试参数，包含所有测试所需的配置信息。 只能通过 {@link TestParamsBuilder} 创建实例。
 *
 * <p>使用示例：
 *
 * <pre>
 * // 使用默认 latest 版本（所有版本参数默认为"latest"）
 * TestPromptsParams params = TestPromptsParams.builder()
 *     .historyGroup(1)     // 历史组编号是必需参数
 *     .historyNumber("1")  // 历史编号是必需参数，同时决定 history 和 arg 文件
 *     .templateArgsGroup(1)  // 模板参数组编号是必需参数
 *     .build();
 *
 * // 指定特定版本
 * TestPromptsParams customParams = TestPromptsParams.builder()
 *     .systemPromptsVersion("1")
 *     .templatesVersion("2")
 *     .assistantMessageVersion("1")
 *     .historyGroup(1)
 *     .historyNumber("1")
 *     .templateArgsGroup(1)
 *     .build();
 * </pre>
 *
 * @author xianxing
 * @since 2025/12/30
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TestPromptsParams {
    /** 系统提示版本号（数字或"latest"），默认为"latest" */
    @Builder.Default private final String systemPromptsVersion = "latest";

    /** 模板版本号（数字或"latest"），默认为"latest" */
    @Builder.Default private final String templatesVersion = "latest";

    /** 助手消息版本号（数字或"latest"），默认为"latest" */
    @Builder.Default private final String assistantMessageVersion = "latest";

    /** 历史组编号（决定使用哪个 history-group，必需参数） */
    private final int historyGroup;

    /** 历史编号（同时决定使用哪个 history 和 arg 文件，必需参数，不支持"latest"） */
    private final String historyNumber;

    /** 模板参数组编号（用于 args-group，必需参数） */
    private final int templateArgsGroup;

    /** 是否打印流式输出 */
    @Builder.Default private final boolean printStream = true;

    /** 是否使用系统提示 */
    @Builder.Default private final boolean useSystemPrompts = true;
}
