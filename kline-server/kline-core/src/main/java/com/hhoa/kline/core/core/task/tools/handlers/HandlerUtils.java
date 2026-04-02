package com.hhoa.kline.core.core.task.tools.handlers;

import com.hhoa.kline.core.core.assistant.TextContentBlock;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.task.tools.types.ToolContext;
import com.hhoa.kline.core.core.task.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.workspace.WorkspaceConfig;
import com.hhoa.kline.core.core.workspace.WorkspaceResolver;
import com.hhoa.kline.core.core.workspace.WorkspaceResolver.WorkspacePathResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Handler 公共工具类 提供所有 handlers 共用的工具方法
 *
 * @author hhoa
 */
public class HandlerUtils {

    /**
     * 从 ToolUse 块中获取字符串参数
     *
     * @param block ToolUse 块
     * @param key 参数键
     * @return 参数值，如果不存在则返回 null
     */
    public static String getStringParam(ToolUse block, String key) {
        if (block.getParams() == null) {
            return null;
        }
        Object v = block.getParams().get(key);
        return v == null ? null : String.valueOf(v);
    }

    /**
     * 从 ToolUse 块中获取路径参数，支持 path 和 absolutePath 两种参数名。
     * 优先使用 path，回退到 absolutePath（供 NATIVE_GPT_5/NATIVE_NEXT_GEN 变体使用）。
     *
     * @param block ToolUse 块
     * @return 路径值，如果都不存在则返回 null
     */
    public static String getPathParam(ToolUse block)
    {
        String path = getStringParam(block, "path");
        if (path != null)
        {
            return path;
        }
        return getStringParam(block, "absolutePath");
    }

    /**
     * 解析路径（相对路径转绝对路径）
     *
     * @param cwd 当前工作目录
     * @param relPath 相对路径
     * @return 绝对路径
     */
    public static Path resolvePath(String cwd, String relPath) {
        if (relPath == null) {
            return Paths.get(cwd);
        }
        Path p = Paths.get(relPath);
        if (p.isAbsolute()) {
            return p;
        }
        return Paths.get(cwd).resolve(relPath).normalize();
    }

    /**
     * 获取可读的路径字符串
     *
     * @param cwd 当前工作目录
     * @param relPath 相对路径
     * @return 可读的路径字符串
     */
    public static String getReadablePath(String cwd, String relPath) {
        if (relPath == null) {
            return cwd;
        }
        Path p = resolvePath(cwd, relPath);
        return p.toString();
    }

    /**
     * 检查路径是否位于工作区中
     *
     * @param pathToCheck 要检查的路径
     * @param config 任务配置（用于获取工作区信息）
     * @return 如果路径在工作区中则返回 true
     */
    public static boolean isLocatedInWorkspace(String pathToCheck, ToolContext config) {
        if (pathToCheck == null || pathToCheck.isEmpty()) {
            return false;
        }

        if (config.getWorkspaceManager() == null) {
            throw new IllegalStateException("workspaceManager 未配置，无法判断路径归属");
        }

        WorkspacePathResult resolvedPathResult =
                WorkspaceResolver.resolveWorkspacePath(
                        new WorkspaceConfig(config.getWorkspaceManager()),
                        pathToCheck,
                        "HandlerUtils.isLocatedInWorkspace");
        String resolvedPath = resolvedPathResult.absolutePath();
        return config.getWorkspaceManager().isPathInWorkspace(resolvedPath);
    }

    /**
     * 创建文本内容块列表
     *
     * @param text 文本内容
     * @return UserContentBlock 列表
     */
    public static ToolExecuteResult createToolExecuteResult(String text) {
        List<UserContentBlock> blocks = new ArrayList<>();
        TextContentBlock block = new TextContentBlock(text);
        blocks.add(block);
        return new ToolExecuteResult.Immediate(blocks);
    }

    public static List<UserContentBlock> createTextBlocks(String text) {
        List<UserContentBlock> blocks = new ArrayList<>();
        TextContentBlock block = new TextContentBlock(text);
        blocks.add(block);
        return blocks;
    }
}
