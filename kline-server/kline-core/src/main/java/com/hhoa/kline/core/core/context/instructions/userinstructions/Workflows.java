package com.hhoa.kline.core.core.context.instructions.userinstructions;

import com.hhoa.kline.core.core.storage.GlobalFileNames;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/** 负责刷新工作流切换状态 */
@Slf4j
public class Workflows {

    @Getter
    public static class WorkflowTogglesResult {
        private final Map<String, Boolean> globalWorkflowToggles;
        private final Map<String, Boolean> localWorkflowToggles;

        public WorkflowTogglesResult(
                Map<String, Boolean> globalWorkflowToggles,
                Map<String, Boolean> localWorkflowToggles) {
            this.globalWorkflowToggles = globalWorkflowToggles;
            this.localWorkflowToggles = localWorkflowToggles;
        }
    }

    /**
     * 刷新工作流切换状态 注意：此方法需要 TaskManager 来访问 StateManager
     *
     * @param workingDirectory 工作目录
     * @param globalWorkflowToggles 全局工作流切换状态
     * @param workflowRulesToggles 本地工作流切换状态
     * @return 更新后的切换状态
     */
    public static WorkflowTogglesResult refreshWorkflowToggles(
            String globalClineWorkflowsFilePath,
            String workingDirectory,
            Map<String, Boolean> globalWorkflowToggles,
            Map<String, Boolean> workflowRulesToggles) {

        try {
            Map<String, Boolean> updatedGlobalWorkflowToggles =
                    RuleHelpers.synchronizeRuleToggles(
                            globalClineWorkflowsFilePath, globalWorkflowToggles);
            // 注意：实际使用时需要调用 stateManager.setGlobalState("globalWorkflowToggles",
            // updatedGlobalWorkflowToggles)

            // 本地工作流
            String workflowsDirPath =
                    Paths.get(workingDirectory, GlobalFileNames.WORKFLOWS).toString();
            Map<String, Boolean> updatedWorkflowToggles =
                    RuleHelpers.synchronizeRuleToggles(workflowsDirPath, workflowRulesToggles);
            // 注意：实际使用时需要调用 stateManager.setWorkspaceState("workflowToggles",
            // updatedWorkflowToggles)

            return new WorkflowTogglesResult(updatedGlobalWorkflowToggles, updatedWorkflowToggles);
        } catch (Exception e) {
            log.error("Failed to refresh workflow toggles: {}", e.getMessage(), e);
            return new WorkflowTogglesResult(
                    globalWorkflowToggles != null ? globalWorkflowToggles : new HashMap<>(),
                    workflowRulesToggles != null ? workflowRulesToggles : new HashMap<>());
        }
    }
}
