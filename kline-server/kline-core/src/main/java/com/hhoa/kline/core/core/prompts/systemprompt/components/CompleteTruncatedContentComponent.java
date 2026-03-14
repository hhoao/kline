package com.hhoa.kline.core.core.prompts.systemprompt.components;

import com.hhoa.kline.core.core.prompts.systemprompt.ConfigOverride;
import com.hhoa.kline.core.core.prompts.systemprompt.PromptVariant;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptComponent;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptSection;
import com.hhoa.kline.core.core.prompts.systemprompt.templates.TemplateEngine;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * 完成截断内容组件 当工具调用内容因 maxTokens 限制而被截断时，帮助完成剩余部分
 *
 * @author hhoa
 */
@RequiredArgsConstructor
public class CompleteTruncatedContentComponent implements SystemPromptComponent {

    private final TemplateEngine templateEngine;

    @Override
    public String apply(PromptVariant variant, SystemPromptContext context) {
        if (context.getHasTruncatedConversationHistory() == null
                || !context.getHasTruncatedConversationHistory()) {
            return null;
        }

        String template = getTemplateText();

        if (variant.getComponentOverrides() != null
                && variant.getComponentOverrides()
                        .containsKey(SystemPromptSection.COMPLETE_TRUNCATED_CONTENT)) {
            ConfigOverride override =
                    variant.getComponentOverrides()
                            .get(SystemPromptSection.COMPLETE_TRUNCATED_CONTENT);
            if (override != null && override.getTemplate() != null) {
                template = override.getTemplate();
            }
        }

        return templateEngine.resolve(template, context, Map.of());
    }

    @Override
    public SystemPromptSection getSystemPromptSection() {
        return SystemPromptSection.COMPLETE_TRUNCATED_CONTENT;
    }

    private String getTemplateText() {
        return """
        Complete Truncated Content

        If tool call content has been truncated due to the maxTokens limit, please complete it（Only the remaining part after truncation is output, which must strictly match the closed format of the corresponding tool.）. The completion must fully comply with the grammatical rules of the original tool, without additional instruction information.
        你现在需要处理因maxTokens限制导致的工具调用内容截断问题，核心任务是补全被截断的工具调用内容，确保工具调用标签闭合、内容完整。以下是具体要求：

        ### 核心目标
        精准补全工具调用的截断部分，恢复完整的工具调用格式（含正确的闭合标签、缺失的内容片段），确保工具能正常识别执行。

        ### 前置信息
        1. 截断场景：AI输出因maxTokens限制，导致工具调用的标签未闭合（如缺少`</content>`、`</write_to_file>`等）或内容片段缺失（如代码未写完、路径不完整）。

        ### 补全规则
        1. 标签闭合优先：先检查并补全缺失的闭合标签（如`</content>`、`</write_to_file>`、`</read_file>`等），确保工具调用结构完整。
        2. 内容片段补全：基于上下文逻辑补全缺失的内容（如截断的代码、未写完的路径），补全内容需与前文语义连贯、格式一致。
        3. 无额外创作：仅补全被截断的部分，不新增与前文无关的内容或修改已输出的完整内容。

        ### 输入输出规范
        - 输入：包含被截断的工具调用内容的上下文文本。
        - 输出：仅输出补全后的工具调用剩余部分（无需重复已输出的完整内容），确保与前文拼接后形成完整、合法的工具调用。

        ###  Example 1: Complete Write HelloWorld File

        ```
        Let me generate Java HelloWorld file.
        <write_to_file>
        <path>db/public/cpt_lowcode_config/弱点-清单/page_config</path>
        <content>public class HelloWorld {
            public static void main(String[] args) {
                System.ou
        ```

        ```
        t.println("Hello, World!");
            }
        }</content>
        </write_to_file>
        ```
        """;
    }
}
