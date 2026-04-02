package com.hhoa.kline.core.core.hooks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Hook 工厂，负责发现和创建 hook runner。
 *
 * <p>发现逻辑：
 *
 * <ul>
 *   <li>全局 hook：~/Documents/Cline/Hooks/{HookName} 或 {HookName}.ps1
 *   <li>工作区 hook：{workspaceRoot}/.clinerules/hooks/{HookName}
 *   <li>Linux/macOS 使用无扩展名的可执行文件；Windows 使用 .ps1
 * </ul>
 */
@Slf4j
public class HookFactory {

    private final List<String> hooksDirs;
    private final List<String> workspaceRoots;
    private final String primaryCwd;

    /**
     * @param hooksDirs 所有 hook 搜索目录（全局 + 各工作区）
     * @param workspaceRoots 工作区根目录列表
     */
    public HookFactory(List<String> hooksDirs, List<String> workspaceRoots) {
        this.hooksDirs = hooksDirs != null ? hooksDirs : List.of();
        this.workspaceRoots = workspaceRoots != null ? workspaceRoots : List.of();
        this.primaryCwd = this.workspaceRoots.isEmpty() ? null : this.workspaceRoots.get(0);
    }

    /** 获取发现的 hook 脚本路径信息 */
    public List<String> getHookScriptPaths(HookName hookName) {
        return findHookScripts(hookName);
    }

    /** 检查是否存在任何 hook 脚本 */
    public boolean hasHook(HookName hookName) {
        return !findHookScripts(hookName).isEmpty();
    }

    /** 创建不带流式回调的 hook runner */
    public HookRunner create(HookName hookName) {
        return createWithStreaming(hookName, null);
    }

    /**
     * 创建带可选流式回调的 hook runner。
     *
     * <ol>
     *   <li>发现 hook 脚本
     *   <li>为每个脚本创建 StdioHookRunner
     *   <li>无脚本返回 NoOpRunner（Null-Object 模式）
     *   <li>多脚本返回 CombinedHookRunner（并行执行）
     * </ol>
     */
    public HookRunner createWithStreaming(HookName hookName, HookStreamCallback streamCallback) {
        List<String> scripts = findHookScripts(hookName);

        List<HookRunner> runners =
                scripts.stream()
                        .map(
                                script -> {
                                    String source = determineScriptSource(script);
                                    String cwd = determineHookCwd(script);
                                    return (HookRunner)
                                            new StdioHookRunner(
                                                    hookName, script, source, streamCallback, cwd);
                                })
                        .collect(Collectors.toList());

        if (runners.isEmpty()) {
            return new NoOpRunner(hookName);
        }
        return runners.size() == 1 ? runners.get(0) : new CombinedHookRunner(hookName, runners);
    }

    /** 搜索所有 hook 目录中匹配的脚本 */
    private List<String> findHookScripts(HookName hookName) {
        List<String> scripts = new ArrayList<>();
        for (String hooksDir : hooksDirs) {
            String script = findHookInDir(hookName, hooksDir);
            if (script != null) {
                scripts.add(script);
            }
        }
        return scripts;
    }

    /** 在单个目录中查找 hook 脚本 */
    private String findHookInDir(HookName hookName, String hooksDir) {
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        if (isWindows) {
            return findWindowsHook(hookName, hooksDir);
        } else {
            return findUnixHook(hookName, hooksDir);
        }
    }

    private String findWindowsHook(HookName hookName, String hooksDir) {
        Path psFile = Paths.get(hooksDir, hookName.getValue() + ".ps1");
        try {
            if (Files.isRegularFile(psFile)) {
                return psFile.toString();
            }
        } catch (Exception e) {
            handleDiscoveryError(e, hookName, psFile.toString());
        }
        return null;
    }

    private String findUnixHook(HookName hookName, String hooksDir) {
        Path candidate = Paths.get(hooksDir, hookName.getValue());
        try {
            if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {
                return candidate.toString();
            }
        } catch (Exception e) {
            handleDiscoveryError(e, hookName, candidate.toString());
        }
        return null;
    }

    private void handleDiscoveryError(Exception error, HookName hookName, String candidate) {
        if (error instanceof java.nio.file.NoSuchFileException
                || error instanceof java.nio.file.AccessDeniedException
                || (error instanceof IOException
                        && error.getMessage() != null
                        && error.getMessage().contains("Not a directory"))) {
            // 预期的错误，静默处理
            return;
        }
        log.warn(
                "Unexpected error while searching for hook '{}' at '{}': {}",
                hookName,
                candidate,
                error.getMessage());
    }

    /** 判断 hook 来源（global 或 workspace） */
    private String determineScriptSource(String scriptPath) {
        String containingDir =
                hooksDirs.stream().filter(scriptPath::startsWith).findFirst().orElse(null);
        if (containingDir != null && isGlobalHooksDir(containingDir)) {
            return "global";
        }
        return "workspace";
    }

    /** 确定 hook 脚本的工作目录 */
    private String determineHookCwd(String scriptPath) {
        String containingDir =
                hooksDirs.stream().filter(scriptPath::startsWith).findFirst().orElse(null);

        // 全局 hook 使用主工作区
        if (containingDir != null && isGlobalHooksDir(containingDir)) {
            return primaryCwd;
        }

        // 工作区 hook 使用其所属的工作区根目录
        if (containingDir != null) {
            for (String root : workspaceRoots) {
                if (containingDir.startsWith(root)) {
                    return root;
                }
            }
        }

        return primaryCwd;
    }

    /** 检查目录路径是否为全局 hook 目录 */
    private static boolean isGlobalHooksDir(String dir) {
        return dir.toLowerCase().matches(".*[/\\\\][ck]line[/\\\\]hooks.*");
    }
}
