package com.hhoa.kline.web.service;

import com.hhoa.kline.core.core.shared.storage.GlobalState;
import com.hhoa.kline.core.core.shared.storage.Secrets;
import com.hhoa.kline.core.core.shared.storage.Settings;

public interface StateService {

    String getLatestState();

    void toggleFavoriteModel(String request);

    void resetState();

    GlobalState getGlobalState();

    void updateGlobalState(GlobalState globalState);

    Secrets getSecrets();

    void updateSecrets(Secrets secrets);

    Settings getSettings();

    void updateSettings(Settings settings);
}
