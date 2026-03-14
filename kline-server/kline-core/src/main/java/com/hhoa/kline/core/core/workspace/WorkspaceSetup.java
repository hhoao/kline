package com.hhoa.kline.core.core.workspace;

import com.hhoa.kline.core.core.services.telemetry.TelemetryService;
import com.hhoa.kline.core.core.shared.multiroot.WorkspaceRoot;
import com.hhoa.kline.core.core.storage.StateManager;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * WorkspaceSetup - 工作区管理器初始化和设置工具
 *
 * <p>初始化并持久化 WorkspaceRootManager， 发出遥测数据，并在出错时处理回退
 */
@Slf4j
public class WorkspaceSetup {

    /**
     * 初始化并持久化 WorkspaceRootManager， 发出遥测数据，并在出错时处理回退
     *
     * <p>调用者注入 detectRoots 以避免与 TaskManager 紧密耦合
     *
     * @param config 设置配置
     * @return WorkspaceRootManager 实例
     */
    public static WorkspaceRootManager setupWorkspaceManager(SetupConfig config) {
        long startTime = System.currentTimeMillis();

        WorkspaceRootManager manager;

        List<WorkspaceRoot> roots = config.detectRoots().detectRoots();

        if (roots == null) {
            roots = new ArrayList<>();
        }

        manager = new WorkspaceRootManager(roots, 0);
        log.info("[WorkspaceManager] 多根模式: 检测到 {} 个根目录", roots.size());

        if (config.telemetryService() != null) {
            List<String> vcsTypes =
                    roots.stream()
                            .filter(r -> r != null && r.getVcs() != null)
                            .map(r -> r.getVcs().toString())
                            .collect(Collectors.toList());

            config.telemetryService()
                    .captureWorkspaceInitialized(
                            roots.size(), vcsTypes, System.currentTimeMillis() - startTime, true);
        }

        config.stateManager().getGlobalState().setWorkspaceRoots(manager.getRoots());
        config.stateManager().getGlobalState().setPrimaryRootIndex(manager.getPrimaryIndex());
        return manager;
    }

    @FunctionalInterface
    public interface RootsDetector {
        List<WorkspaceRoot> detectRoots();
    }

    public interface MessageService {
        void showWarningMessage(String message);
    }

    public interface WorkspacePathsProvider {
        Integer getWorkspacePathsCount();
    }

    public record SetupConfig(
            StateManager stateManager,
            RootsDetector detectRoots,
            TelemetryService telemetryService,
            MessageService messageService,
            WorkspacePathsProvider workspacePathsProvider) {
        public SetupConfig(
                StateManager stateManager,
                RootsDetector detectRoots,
                TelemetryService telemetryService) {
            this(stateManager, detectRoots, telemetryService, null, null);
        }

        public SetupConfig {
            if (stateManager == null) {
                throw new IllegalArgumentException("stateManager 不能为 null");
            }
            if (detectRoots == null) {
                throw new IllegalArgumentException("detectRoots 不能为 null");
            }
        }
    }
}
