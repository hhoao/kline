package com.hhoa.kline.core.core.shared;

import java.util.ArrayList;
import java.util.List;

/** 注意：LanguageModelChatSelector 来自 vscode，在 Java 中使用对象表示 */
public class VsCodeSelectorUtils {
    public static final String SELECTOR_SEPARATOR = "/";

    /**
     * 将 VS Code 语言模型聊天选择器转换为字符串
     *
     * @param selector 选择器对象，包含 vendor, family, version, id 字段
     * @return 连接后的字符串，例如 "vendor/family/version/id"
     */
    public static String stringifyVsCodeLmModelSelector(LanguageModelChatSelector selector) {
        if (selector == null) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        if (selector.getVendor() != null && !selector.getVendor().isEmpty()) {
            parts.add(selector.getVendor());
        }
        if (selector.getFamily() != null && !selector.getFamily().isEmpty()) {
            parts.add(selector.getFamily());
        }
        if (selector.getVersion() != null && !selector.getVersion().isEmpty()) {
            parts.add(selector.getVersion());
        }
        if (selector.getId() != null && !selector.getId().isEmpty()) {
            parts.add(selector.getId());
        }

        return String.join(SELECTOR_SEPARATOR, parts);
    }
}
