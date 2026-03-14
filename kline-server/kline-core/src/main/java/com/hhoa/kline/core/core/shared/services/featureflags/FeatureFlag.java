package com.hhoa.kline.core.core.shared.services.featureflags;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;

@Getter
public enum FeatureFlag {
    CUSTOM_INSTRUCTIONS("custom-instructions"),
    DEV_ENV_POSTHOG("dev-env-posthog"),
    DICTATION("dictation"),
    FOCUS_CHAIN_CHECKLIST("focus_chain_checklist"),
    WORKOS_AUTH("workos_auth"),
    DO_NOTHING("do_nothing"),
    HOOKS("hooks");

    private final String value;

    FeatureFlag(String value) {
        this.value = value;
    }

    public static final Map<FeatureFlag, Boolean> FEATURE_FLAG_DEFAULT_VALUE = new HashMap<>();

    static {
        FEATURE_FLAG_DEFAULT_VALUE.put(FeatureFlag.WORKOS_AUTH, true);
        FEATURE_FLAG_DEFAULT_VALUE.put(FeatureFlag.DO_NOTHING, false);
        FEATURE_FLAG_DEFAULT_VALUE.put(FeatureFlag.HOOKS, false);
    }

    public static final List<FeatureFlag> FEATURE_FLAGS = Arrays.asList(values());
}
