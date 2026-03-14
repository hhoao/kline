package com.hhoa.kline.web.service.impl;

import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.core.controller.TaskManagerFactory;
import com.hhoa.kline.core.core.shared.ExtensionState;
import com.hhoa.kline.core.core.shared.storage.GlobalState;
import com.hhoa.kline.core.core.shared.storage.Secrets;
import com.hhoa.kline.core.core.shared.storage.Settings;
import com.hhoa.kline.core.core.storage.StateManager;
import com.hhoa.kline.web.service.StateService;
import com.hhoa.kline.web.utils.LoginUserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StateServiceImpl implements StateService {

    private final TaskManagerFactory taskManagerFactory;

    @Override
    public String getLatestState() {
        ExtensionState state =
                taskManagerFactory.getOrCreateTaskManager().getStateToPostToWebview(null);
        return state != null ? JsonUtils.toJsonString(state) : "{}";
    }

    private String getUserId() {
        Long loginId = LoginUserUtil.getLoginIdDefaultNull();
        return loginId != null ? String.valueOf(loginId) : "default";
    }

    @Override
    public void toggleFavoriteModel(String request) {
        // TODO: 实现切换收藏模型逻辑
    }

    @Override
    public void resetState() {
        // TODO: 实现重置状态逻辑
    }

    private StateManager getStateManager() {
        return taskManagerFactory.getOrCreateTaskManager().getStateManager();
    }

    @Override
    public GlobalState getGlobalState() {
        return getStateManager().getGlobalState();
    }

    @Override
    public void updateGlobalState(GlobalState globalState) {
        getStateManager().updateGlobalState(globalState);
    }

    @Override
    public Secrets getSecrets() {
        return getStateManager().getSecrets();
    }

    @Override
    public void updateSecrets(Secrets secrets) {
        getStateManager().updateSecrets(secrets);
    }

    @Override
    public Settings getSettings() {
        return getStateManager().getSettings();
    }

    @Override
    public void updateSettings(Settings settings) {
        getStateManager().updateSettings(settings);
    }
}
