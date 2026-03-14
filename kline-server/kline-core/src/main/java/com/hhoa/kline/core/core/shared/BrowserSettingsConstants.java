package com.hhoa.kline.core.core.shared;

import com.hhoa.kline.core.core.shared.proto.cline.BrowserSettings;
import com.hhoa.kline.core.core.shared.proto.cline.Viewport;
import java.util.HashMap;
import java.util.Map;

public class BrowserSettingsConstants {
    public static final BrowserSettings DEFAULT_BROWSER_SETTINGS =
            BrowserSettings.builder()
                    .viewport(Viewport.builder().width(900).height(600).build())
                    .remoteBrowserEnabled(false)
                    .remoteBrowserHost("http://localhost:9222")
                    .chromeExecutablePath("")
                    .disableToolUse(false)
                    .customArgs("")
                    .build();

    public static final Map<String, Viewport> BROWSER_VIEWPORT_PRESETS = new HashMap<>();

    static {
        BROWSER_VIEWPORT_PRESETS.put(
                "Large Desktop (1280x800)", Viewport.builder().width(1280).height(800).build());
        BROWSER_VIEWPORT_PRESETS.put(
                "Small Desktop (900x600)", Viewport.builder().width(900).height(600).build());
        BROWSER_VIEWPORT_PRESETS.put(
                "Tablet (768x1024)", Viewport.builder().width(768).height(1024).build());
        BROWSER_VIEWPORT_PRESETS.put(
                "Mobile (360x640)", Viewport.builder().width(360).height(640).build());
    }
}
