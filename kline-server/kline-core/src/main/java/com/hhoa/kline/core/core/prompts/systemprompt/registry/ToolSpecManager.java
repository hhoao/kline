package com.hhoa.kline.core.core.prompts.systemprompt.registry;

import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpecConverter;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.AccessMcpResourceTool;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.ActModeRespondTool;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.ApplyPatchTool;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.AskFollowupQuestionTool;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.AttemptCompletionTool;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.BrowserActionTool;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.CliSubagentsTool;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.ExecuteCommandTool;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.FocusChainTool;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.GenerateExplanationTool;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.ListCodeDefinitionNamesTool;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.ListFilesTool;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.LoadMcpDocumentationTool;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.NewTaskTool;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.PlanModeRespondTool;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.ReadFileTool;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.ReplaceInFileTool;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.SearchFilesTool;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.SubagentTool;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.UseMcpToolTool;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.UseSkillTool;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.WebFetchTool;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.WebSearchTool;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.WriteToFileTool;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class ToolSpecManager {

    private static final Map<ModelFamily, Set<ClineToolSpec>> VARIANTS = new ConcurrentHashMap<>();
    private static final String PLAN_MODE_RESPOND_ID = ClineDefaultTool.PLAN_MODE.getValue();

    private static final List<Function<ModelFamily, ClineToolSpec>> STATIC_TOOL_SUPPLIERS =
            List.of(
                    AccessMcpResourceTool::create,
                    ActModeRespondTool::create,
                    ApplyPatchTool::create,
                    AskFollowupQuestionTool::create,
                    AttemptCompletionTool::create,
                    BrowserActionTool::create,
                    CliSubagentsTool::create,
                    ExecuteCommandTool::create,
                    FocusChainTool::create,
                    GenerateExplanationTool::create,
                    ListCodeDefinitionNamesTool::create,
                    ListFilesTool::create,
                    LoadMcpDocumentationTool::create,
                    NewTaskTool::create,
                    ReadFileTool::create,
                    ReplaceInFileTool::create,
                    SearchFilesTool::create,
                    SubagentTool::create,
                    UseSkillTool::create,
                    UseMcpToolTool::create,
                    WebFetchTool::create,
                    WebSearchTool::create,
                    WriteToFileTool::create);

    static {
        for (ModelFamily family : ModelFamily.values()) {
            Set<ClineToolSpec> set = VARIANTS.computeIfAbsent(family, k -> new LinkedHashSet<>());
            for (var supplier : STATIC_TOOL_SUPPLIERS) {
                ClineToolSpec spec = supplier.apply(family);
                if (spec == null) {
                    continue;
                }
                if (set.stream()
                        .noneMatch(t -> spec.getId() != null && spec.getId().equals(t.getId()))) {
                    set.add(spec);
                }
            }
        }
    }

    public static ClineToolSpec register(ClineToolSpec config) {
        ModelFamily variant =
                config.getVariant() != null ? config.getVariant() : ModelFamily.GENERIC;
        Set<ClineToolSpec> existing = VARIANTS.computeIfAbsent(variant, k -> new LinkedHashSet<>());
        if (existing.stream()
                .noneMatch(t -> t.getId() != null && t.getId().equals(config.getId()))) {
            existing.add(config);
        }
        return config;
    }

    public static List<ClineToolSpec> getTools(ModelFamily variant) {
        Set<ClineToolSpec> toolsSet = VARIANTS.get(variant);
        Set<ClineToolSpec> defaultSet = VARIANTS.get(ModelFamily.GENERIC);
        Set<ClineToolSpec> source =
                (toolsSet != null && !toolsSet.isEmpty()) ? toolsSet : defaultSet;
        return source != null ? new ArrayList<>(source) : List.of();
    }

    public static List<ClineToolSpec> getToolSpecs(
            ModelFamily variant, SystemPromptContext context) {
        List<ClineToolSpec> list = new ArrayList<>(getTools(variant));
        list.removeIf(spec -> PLAN_MODE_RESPOND_ID.equals(spec.getId()));
        ClineToolSpec planModeSpec = PlanModeRespondTool.create(variant, context);
        if (planModeSpec != null) {
            list.add(planModeSpec);
        }
        list.sort(Comparator.comparing(s -> s.getId() != null ? s.getId() : "", String::compareTo));
        return list;
    }

    public static List<String> getRegisteredModelIds() {
        return new ArrayList<>(VARIANTS.keySet().stream().map(Enum::name).toList());
    }

    public static ClineToolSpec getToolByName(String toolName, ModelFamily variant) {
        List<ClineToolSpec> tools = getTools(variant);
        if (tools.isEmpty()) {
            return null;
        }
        return tools.stream()
                .filter(tool -> toolName.equals(tool.getId()))
                .findFirst()
                .orElse(null);
    }

    public static ClineToolSpec getToolByNameWithFallback(String toolName, ModelFamily variant) {
        ClineToolSpec exact = getToolByName(toolName, variant);
        if (exact != null) {
            return exact;
        }
        ClineToolSpec generic = getToolByName(toolName, ModelFamily.GENERIC);
        if (generic != null) {
            return generic;
        }
        for (ModelFamily family : EnumSet.allOf(ModelFamily.class)) {
            if (family != variant && family != ModelFamily.GENERIC) {
                ClineToolSpec found = getToolByName(toolName, family);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    public static List<ClineToolSpec> getToolsForVariantWithFallback(
            ModelFamily variant, List<String> requestedIds) {
        return getToolsForVariantWithFallback(variant, requestedIds, null);
    }

    public static List<ClineToolSpec> getToolsForVariantWithFallback(
            ModelFamily variant, List<String> requestedIds, SystemPromptContext context) {
        List<ClineToolSpec> resolved = new ArrayList<>();
        for (String id : requestedIds) {
            ClineToolSpec tool;
            if (PLAN_MODE_RESPOND_ID.equals(id) && context != null) {
                tool = PlanModeRespondTool.create(variant, context);
            } else {
                tool = getToolByNameWithFallback(id, variant);
            }
            if (tool != null && resolved.stream().noneMatch(t -> id.equals(t.getId()))) {
                resolved.add(tool);
            }
        }
        return resolved;
    }

    /** 获取经过上下文过滤的已启用工具列表。 对应 TS ClineToolSet.getEnabledTools() */
    public static List<ClineToolSpec> getEnabledTools(
            ModelFamily variant, SystemPromptContext context) {
        List<ClineToolSpec> allTools = getToolSpecs(variant, context);
        return allTools.stream()
                .filter(
                        tool -> {
                            if (tool.getContextRequirements() != null) {
                                try {
                                    return Boolean.TRUE.equals(
                                            tool.getContextRequirements().apply(context));
                                } catch (Exception e) {
                                    return false;
                                }
                            }
                            return true;
                        })
                .toList();
    }

    /**
     * 获取已启用工具的原生工具定义（用于 API 调用）。 对应 TS ClineToolSet.getNativeTools()
     *
     * @param variant 模型家族
     * @param context 系统提示上下文
     * @param converter 工具转换函数，将 ClineToolSpec 转为提供商格式的 Map
     * @return 原生工具定义列表
     */
    public static List<Map<String, Object>> getNativeTools(
            ModelFamily variant,
            SystemPromptContext context,
            Function<ClineToolSpecConverter.ToolConversionInput, Map<String, Object>> converter) {
        List<ClineToolSpec> enabledTools = getEnabledTools(variant, context);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ClineToolSpec tool : enabledTools) {
            try {
                result.add(
                        converter.apply(
                                new ClineToolSpecConverter.ToolConversionInput(tool, context)));
            } catch (Exception e) {
                // 跳过不满足上下文要求的工具
            }
        }
        return result;
    }
}
