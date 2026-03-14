package com.hhoa.kline.core.prompts;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import com.hhoa.kline.core.core.assistant.AssistantMessage;
import com.hhoa.kline.core.core.assistant.MessageParam;
import com.hhoa.kline.core.core.assistant.TextContentBlock;
import com.hhoa.kline.core.core.assistant.UserMessage;
import com.hhoa.kline.core.core.prompts.systemprompt.templates.TemplateEngine;
import com.hhoa.kline.core.core.task.ApiChunk;
import com.hhoa.kline.web.core.SpringAIApiHandler;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

/**
 * Prompts测试基类
 *
 * <p>提供通用的测试资源加载和消息构建方法，支持版本化的测试用例结构：
 *
 * <ul>
 *   <li>系统提示（systemPrompts/）：包含 1.txt, 2.txt, latest.txt, ...
 *   <li>历史组（histories/history-groupX/）：按组组织历史数据
 *       <ul>
 *         <li>assistantMessages/: 助手消息（1.txt, latest.txt, ...）
 *         <li>data/{historyNumber}/: 历史数据和参数
 *             <ul>
 *               <li>history.json: 历史对话数据
 *               <li>args-group{templateArgsGroup}/: 参数文件（文件名=变量名，内容=变量值）
 *             </ul>
 *         <li>nextUserMessageTemplates/args-group{templateArgsGroup}/: 用户消息模板（1.txt, latest.txt,
 *             ...）
 *       </ul>
 *   <li>测试时需要指定：历史组编号、历史编号、组编号
 * </ul>
 *
 * <p>目录结构示例：
 *
 * <pre>
 * prompts/
 *   └── complete-truncated-content/
 *       ├── systemPrompts/
 *       │   ├── 1.txt
 *       │   └── latest.txt
 *       └── histories/
 *           └── history-group1/
 *               ├── assistantMessages/
 *               │   ├── 1.txt
 *               │   └── latest.txt
 *               ├── data/
 *               │   ├── 1/
 *               │   │   ├── history.json
 *               │   │   └── args-group1/
 *               │   │       └── FOCUS_CHAIN_EXAMPLE_NEW_FILE
 *               │   └── 2/
 *               │       ├── history.json
 *               │       └── args-group1/
 *               │           └── FOCUS_CHAIN_EXAMPLE_NEW_FILE
 *               └── nextUserMessageTemplates/
 *                   └── args-group1/
 *                       ├── 1.txt
 *                       └── latest.txt
 * </pre>
 *
 * <p>所有版本号支持数字或"latest"（自动选择最大版本号）。
 *
 * @author xianxing
 * @since 2025/12/30
 */
@SpringBootTest
@Slf4j
public abstract class PromptsTestBase {
    @Autowired protected SpringAIApiHandler springAIApiHandler;
    protected ObjectMapper objectMapper = new ObjectMapper();
    @Autowired protected TemplateEngine templateEngine;

    /** 获取测试类型，对应 prompts/ 下的目录名 */
    public abstract String getType();

    /** 获取指定版本的系统提示 */
    protected String getSystemPrompts(String systemPromptsVersion) throws IOException {
        String path =
                String.format("prompts/%s/systemPrompts/%s.txt", getType(), systemPromptsVersion);
        URL resource = Resources.getResource(path);
        return Files.readString(Path.of(resource.getPath()));
    }

    /**
     * 获取指定编号的历史对话数据
     *
     * @param historyGroup 历史组编号
     * @param historyNumber 历史编号
     * @return 历史对话数据
     */
    protected List<MessageParam> getHistory(int historyGroup, String historyNumber)
            throws IOException {
        String path =
                String.format(
                        "prompts/%s/histories/history-group%d/data/%s/history.json",
                        getType(), historyGroup, historyNumber);
        URL resource = Resources.getResource(path);
        String history = Files.readString(Path.of(resource.getPath()));
        return objectMapper.readValue(history, new TypeReference<List<MessageParam>>() {});
    }

    /**
     * 获取指定版本的助手消息数据（通常是截断提示）
     *
     * @param historyGroup 历史组编号
     * @param assistantMessageVersion 助手消息版本号
     * @return 助手消息内容
     */
    protected String getAssistantMessage(int historyGroup, String assistantMessageVersion)
            throws IOException {
        String path =
                String.format(
                        "prompts/%s/histories/history-group%d/assistantMessages/%s.txt",
                        getType(), historyGroup, assistantMessageVersion);
        URL resource = Resources.getResource(path);
        return Files.readString(Path.of(resource.getPath()));
    }

    /**
     * 获取指定版本的用户消息模板
     *
     * @param historyGroup 历史组编号
     * @param templatesVersion 模板版本号（数字或"latest"）
     * @param templateArgsGroup 模板参数组编号（用于 args-group）
     * @return 模板内容
     */
    protected String getTemplate(int historyGroup, String templatesVersion, int templateArgsGroup)
            throws IOException {
        String path =
                String.format(
                        "prompts/%s/histories/history-group%d/nextUserMessageTemplates/args-group%d/%s.txt",
                        getType(), historyGroup, templateArgsGroup, templatesVersion);
        URL resource = Resources.getResource(path);
        return Files.readString(Path.of(resource.getPath()));
    }

    /**
     * 获取参数文件
     *
     * <p>从
     * histories/history-group{historyGroup}/data/{historyNumber}/args-group{templateArgsGroup}/
     * 目录读取所有文件， 文件名作为变量名，文件内容作为变量值
     *
     * @param historyGroup 历史组编号
     * @param historyNumber 历史编号（对应 data 下的目录名）
     * @param templateArgsGroup 模板参数组编号（对应 args-group 目录名）
     * @return 参数映射（变量名 -> 变量值）
     */
    protected Map<String, String> getNextUserMessageArgs(
            int historyGroup, String historyNumber, int templateArgsGroup) throws IOException {
        Map<String, String> args = new HashMap<>();
        String argsDirPath =
                String.format(
                        "prompts/%s/histories/history-group%d/data/%s/args-group%d",
                        getType(), historyGroup, historyNumber, templateArgsGroup);
        URL resource = Resources.getResource(argsDirPath);

        Path argsDir = Paths.get(resource.getPath());
        if (!Files.exists(argsDir) || !Files.isDirectory(argsDir)) {
            return args;
        }

        // 读取目录下的所有文件
        try (Stream<Path> paths = Files.list(argsDir)) {
            paths.filter(Files::isRegularFile)
                    .forEach(
                            filePath -> {
                                try {
                                    String fileName = filePath.getFileName().toString();
                                    String content = Files.readString(filePath);
                                    args.put(fileName, content);
                                } catch (IOException e) {
                                    log.warn("读取参数文件失败: {}", filePath, e);
                                }
                            });
        }

        return args;
    }

    /**
     * 构建完整的测试消息列表
     *
     * @param params 测试参数
     * @return 消息列表
     */
    protected List<MessageParam> buildTestMessages(TestPromptsParams params) throws IOException {
        List<MessageParam> messageParams =
                getHistory(params.getHistoryGroup(), params.getHistoryNumber());

        // 添加助手消息（截断提示）
        String assistantMessage =
                getAssistantMessage(params.getHistoryGroup(), params.getAssistantMessageVersion());
        if (assistantMessage != null && !assistantMessage.trim().isEmpty()) {
            messageParams.add(
                    AssistantMessage.builder()
                            .content(List.of(new TextContentBlock(assistantMessage)))
                            .build());
        }

        // 解析并添加用户消息模板
        String template =
                getTemplate(
                        params.getHistoryGroup(),
                        params.getTemplatesVersion(),
                        params.getTemplateArgsGroup());
        if (template != null && !template.trim().isEmpty()) {
            // 从
            // histories/history-group{historyGroup}/data/{historyNumber}/args-group{templateArgsGroup}/ 目录读取所有参数文件
            Map<String, String> templateArgs =
                    getNextUserMessageArgs(
                            params.getHistoryGroup(),
                            params.getHistoryNumber(),
                            params.getTemplateArgsGroup());
            String resolvedMessage = templateEngine.resolve(template, null, templateArgs);
            messageParams.add(
                    UserMessage.builder()
                            .content(List.of(new TextContentBlock(resolvedMessage)))
                            .build());
        }

        return messageParams;
    }

    /**
     * 执行流式测试并收集结果
     *
     * @param systemPrompts 系统提示
     * @param messageParams 消息列表
     * @param printStream 是否打印流式输出
     * @return 最后一个chunk
     */
    protected ApiChunk executeStreamTest(
            String systemPrompts, List<MessageParam> messageParams, boolean printStream) {
        Flux<ApiChunk> messageStream =
                springAIApiHandler.createMessageStream(systemPrompts, messageParams);

        if (printStream) {
            return messageStream
                    .doOnNext(
                            apiChunk -> {
                                System.out.print(apiChunk.text());
                                if (apiChunk.outputTokens() != null
                                        && apiChunk.outputTokens() != 0) {
                                    log.info("API输出信息: outputTokens={}", apiChunk.outputTokens());
                                }
                            })
                    .blockLast();
        } else {
            return messageStream.blockLast();
        }
    }

    /**
     * 执行测试流程
     *
     * @param params 测试参数
     * @return 最后一个chunk
     */
    protected ApiChunk executeTest(TestPromptsParams params) throws IOException {
        String systemPrompts =
                params.isUseSystemPrompts()
                        ? getSystemPrompts(params.getSystemPromptsVersion())
                        : "";
        List<MessageParam> messageParams = buildTestMessages(params);
        return executeStreamTest(systemPrompts, messageParams, params.isPrintStream());
    }
}
