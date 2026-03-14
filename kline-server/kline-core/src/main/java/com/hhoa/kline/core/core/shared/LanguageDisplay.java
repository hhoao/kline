package com.hhoa.kline.core.core.shared;

public enum LanguageDisplay {
    ENGLISH("English"),
    ARABIC("Arabic - العربية"),
    PORTUGUESE_BR("Portuguese - Português (Brasil)"),
    CZECH("Czech - Čeština"),
    FRENCH("French - Français"),
    GERMAN("German - Deutsch"),
    HINDI("Hindi - हिन्दी"),
    HUNGARIAN("Hungarian - Magyar"),
    ITALIAN("Italian - Italiano"),
    JAPANESE("Japanese - 日本語"),
    KOREAN("Korean - 한국어"),
    POLISH("Polish - Polski"),
    PORTUGUESE_PT("Portuguese - Português (Portugal)"),
    RUSSIAN("Russian - Русский"),
    SIMPLIFIED_CHINESE("Simplified Chinese - 简体中文"),
    SPANISH("Spanish - Español"),
    TRADITIONAL_CHINESE("Traditional Chinese - 繁體中文"),
    TURKISH("Turkish - Türkçe");

    private final String display;

    LanguageDisplay(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }

    @Override
    public String toString() {
        return display;
    }

    public static LanguageDisplay fromDisplay(String display) {
        if (display == null) {
            return ENGLISH;
        }
        for (LanguageDisplay langDisplay : values()) {
            if (langDisplay.display.equals(display)) {
                return langDisplay;
            }
        }
        return ENGLISH;
    }
}
