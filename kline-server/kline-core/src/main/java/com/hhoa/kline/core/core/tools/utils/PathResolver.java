package com.hhoa.kline.core.core.tools.utils;

import com.hhoa.kline.core.core.tools.ToolValidator;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.workspace.WorkspaceConfig;
import com.hhoa.kline.core.core.workspace.WorkspaceResolver;

/** 与 Cline {@code PathResolver.ts} 对齐：解析和验证任务上下文中的文件路径。 */
public class PathResolver {

    private final ToolContext config;
    private final ToolValidator validator;

    public PathResolver(ToolContext config, ToolValidator validator) {
        this.config = config;
        this.validator = validator;
    }

    public record PathResolution(String absolutePath, String resolvedPath) {}

    /**
     * 解析文件路径到绝对路径。
     *
     * @param filePath 要解析的路径
     * @param caller 调用者标识
     * @return 解析结果，解析失败返回 null
     */
    public PathResolution resolve(String filePath, String caller) {
        try {
            if (config.getWorkspaceManager() != null) {
                WorkspaceResolver.WorkspacePathResult pathResult =
                        WorkspaceResolver.resolveWorkspacePath(
                                new WorkspaceConfig(config.getWorkspaceManager()),
                                filePath,
                                caller);
                return new PathResolution(pathResult.absolutePath(), filePath);
            }
            // 无 workspace manager 时，直接使用 cwd 拼接
            String cwd = config.getCwd();
            if (cwd == null) {
                return null;
            }
            String absolutePath = filePath.startsWith("/") ? filePath : cwd + "/" + filePath;
            return new PathResolution(absolutePath, filePath);
        } catch (Exception e) {
            return null;
        }
    }

    /** 验证路径是否被 .clineignore 阻止。 */
    public ToolValidator.Result validate(String resolvedPath) {
        return validator.checkClineIgnorePath(resolvedPath);
    }

    /**
     * 解析并验证路径。
     *
     * @return 解析结果，验证失败返回 null
     */
    public PathResolution resolveAndValidate(String filePath, String caller) {
        PathResolution resolution = resolve(filePath, caller);
        if (resolution == null) {
            return null;
        }

        ToolValidator.Result validation = validate(resolution.resolvedPath());
        if (!validation.ok()) {
            return null;
        }

        return resolution;
    }
}
