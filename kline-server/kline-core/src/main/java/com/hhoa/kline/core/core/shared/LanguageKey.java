package com.hhoa.kline.core.core.shared;

public enum LanguageKey {
    EN("en", "English"),
    AR("ar", "Arabic - العربية"),
    PT_BR("pt-BR", "Portuguese - Português (Brasil)"),
    CS("cs", "Czech - Čeština"),
    FR("fr", "French - Français"),
    DE("de", "German - Deutsch"),
    HI("hi", "Hindi - हिन्दी"),
    HU("hu", "Hungarian - Magyar"),
    IT("it", "Italian - Italiano"),
    JA("ja", "Japanese - 日本語"),
    KO("ko", "Korean - 한국어"),
    PL("pl", "Polish - Polski"),
    PT_PT("pt-PT", "Portuguese - Português (Portugal)"),
    RU("ru", "Russian - Русский"),
    ZH_CN("zh-CN", "Simplified Chinese - 简体中文"),
    ES("es", "Spanish - Español"),
    ZH_TW("zh-TW", "Traditional Chinese - 繁體中文"),
    TR("tr", "Turkish - Türkçe");

    private final String key;
    private final String display;

    LanguageKey(String key, String display) {
        this.key = key;
        this.display = display;
    }

    public String getKey() {
        return key;
    }

    public String getDisplay() {
        return display;
    }

    /** 默认语言设置 */
    public static final LanguageKey DEFAULT_LANGUAGE_SETTINGS = ZH_CN;

    /**
     * @param display 显示名称（LanguageDisplay 的字符串值）
     * @return 对应的语言键，如果未找到则返回默认值
     */
    public static LanguageKey getLanguageKey(String display) {
        if (display == null) {
            return DEFAULT_LANGUAGE_SETTINGS;
        }
        for (LanguageOptions.LanguageOption option : LanguageOptions.LANGUAGE_OPTIONS) {
            if (option.getDisplay().getDisplay().equals(display)) {
                return option.getKey();
            }
        }
        return DEFAULT_LANGUAGE_SETTINGS;
    }

    /**
     * 从键值获取语言键
     *
     * @param key 键值
     * @return 对应的语言键，如果未找到则返回默认值
     */
    public static LanguageKey fromKey(String key) {
        if (key == null) {
            return DEFAULT_LANGUAGE_SETTINGS;
        }
        for (LanguageKey languageKey : values()) {
            if (languageKey.key.equals(key)) {
                return languageKey;
            }
        }
        return DEFAULT_LANGUAGE_SETTINGS;
    }

    @Override
    public String toString() {
        return key;
    }
}
