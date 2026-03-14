package com.hhoa.kline.core.core.shared;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class LanguageOptions {
    public static final List<LanguageOption> LANGUAGE_OPTIONS = new ArrayList<>();

    static {
        LANGUAGE_OPTIONS.add(new LanguageOption(LanguageKey.EN, LanguageDisplay.ENGLISH));
        LANGUAGE_OPTIONS.add(new LanguageOption(LanguageKey.AR, LanguageDisplay.ARABIC));
        LANGUAGE_OPTIONS.add(new LanguageOption(LanguageKey.PT_BR, LanguageDisplay.PORTUGUESE_BR));
        LANGUAGE_OPTIONS.add(new LanguageOption(LanguageKey.CS, LanguageDisplay.CZECH));
        LANGUAGE_OPTIONS.add(new LanguageOption(LanguageKey.FR, LanguageDisplay.FRENCH));
        LANGUAGE_OPTIONS.add(new LanguageOption(LanguageKey.DE, LanguageDisplay.GERMAN));
        LANGUAGE_OPTIONS.add(new LanguageOption(LanguageKey.HI, LanguageDisplay.HINDI));
        LANGUAGE_OPTIONS.add(new LanguageOption(LanguageKey.HU, LanguageDisplay.HUNGARIAN));
        LANGUAGE_OPTIONS.add(new LanguageOption(LanguageKey.IT, LanguageDisplay.ITALIAN));
        LANGUAGE_OPTIONS.add(new LanguageOption(LanguageKey.JA, LanguageDisplay.JAPANESE));
        LANGUAGE_OPTIONS.add(new LanguageOption(LanguageKey.KO, LanguageDisplay.KOREAN));
        LANGUAGE_OPTIONS.add(new LanguageOption(LanguageKey.PL, LanguageDisplay.POLISH));
        LANGUAGE_OPTIONS.add(new LanguageOption(LanguageKey.PT_PT, LanguageDisplay.PORTUGUESE_PT));
        LANGUAGE_OPTIONS.add(new LanguageOption(LanguageKey.RU, LanguageDisplay.RUSSIAN));
        LANGUAGE_OPTIONS.add(
                new LanguageOption(LanguageKey.ZH_CN, LanguageDisplay.SIMPLIFIED_CHINESE));
        LANGUAGE_OPTIONS.add(new LanguageOption(LanguageKey.ES, LanguageDisplay.SPANISH));
        LANGUAGE_OPTIONS.add(
                new LanguageOption(LanguageKey.ZH_TW, LanguageDisplay.TRADITIONAL_CHINESE));
        LANGUAGE_OPTIONS.add(new LanguageOption(LanguageKey.TR, LanguageDisplay.TURKISH));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LanguageOption {
        private LanguageKey key;
        private LanguageDisplay display;
    }
}
