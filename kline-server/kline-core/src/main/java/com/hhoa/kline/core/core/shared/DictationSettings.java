package com.hhoa.kline.core.core.shared;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DictationSettings {
    @Builder.Default private boolean featureEnabled = false;

    @Builder.Default private boolean dictationEnabled = false;

    @Builder.Default private String dictationLanguage = "en";

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LanguageItem {
        private String name;
        private String code;
    }

    public static final List<LanguageItem> SUPPORTED_DICTATION_LANGUAGES = new ArrayList<>();

    static {
        SUPPORTED_DICTATION_LANGUAGES.addAll(
                Arrays.asList(
                        new LanguageItem("English", "en"),
                        new LanguageItem("Spanish (Español)", "es"),
                        new LanguageItem("Chinese (中文)", "zh"),
                        new LanguageItem("Japanese (日本語)", "ja"),
                        new LanguageItem("Afrikaans", "af"),
                        new LanguageItem("Arabic (العربية)", "ar"),
                        new LanguageItem("Armenian (Հայերեն)", "hy"),
                        new LanguageItem("Azerbaijani (Azərbaycan)", "az"),
                        new LanguageItem("Belarusian (Беларуская)", "be"),
                        new LanguageItem("Bosnian (Bosanski)", "bs"),
                        new LanguageItem("Bulgarian (Български)", "bg"),
                        new LanguageItem("Catalan (Català)", "ca"),
                        new LanguageItem("Croatian (Hrvatski)", "hr"),
                        new LanguageItem("Czech (Čeština)", "cs"),
                        new LanguageItem("Danish (Dansk)", "da"),
                        new LanguageItem("Dutch (Nederlands)", "nl"),
                        new LanguageItem("Estonian (Eesti)", "et"),
                        new LanguageItem("Finnish (Suomi)", "fi"),
                        new LanguageItem("French (Français)", "fr"),
                        new LanguageItem("Galician (Galego)", "gl"),
                        new LanguageItem("German (Deutsch)", "de"),
                        new LanguageItem("Greek (Ελληνικά)", "el"),
                        new LanguageItem("Hebrew (עברית)", "he"),
                        new LanguageItem("Hindi (हिन्दी)", "hi"),
                        new LanguageItem("Hungarian (Magyar)", "hu"),
                        new LanguageItem("Icelandic (Íslenska)", "is"),
                        new LanguageItem("Indonesian (Bahasa Indonesia)", "id"),
                        new LanguageItem("Italian (Italiano)", "it"),
                        new LanguageItem("Kannada (ಕನ್ನಡ)", "kn"),
                        new LanguageItem("Kazakh (Қазақша)", "kk"),
                        new LanguageItem("Korean (한국어)", "ko"),
                        new LanguageItem("Latvian (Latviešu)", "lv"),
                        new LanguageItem("Lithuanian (Lietuvių)", "lt"),
                        new LanguageItem("Macedonian (Македонски)", "mk"),
                        new LanguageItem("Malay (Bahasa Melayu)", "ms"),
                        new LanguageItem("Marathi (मराठी)", "mr"),
                        new LanguageItem("Maori (Te Reo Māori)", "mi"),
                        new LanguageItem("Nepali (नेपाली)", "ne"),
                        new LanguageItem("Norwegian (Norsk)", "no"),
                        new LanguageItem("Persian (فارسی)", "fa"),
                        new LanguageItem("Polish (Polski)", "pl"),
                        new LanguageItem("Portuguese (Português)", "pt"),
                        new LanguageItem("Romanian (Română)", "ro"),
                        new LanguageItem("Russian (Русский)", "ru"),
                        new LanguageItem("Serbian (Српски)", "sr"),
                        new LanguageItem("Slovak (Slovenčina)", "sk"),
                        new LanguageItem("Slovenian (Slovenščina)", "sl"),
                        new LanguageItem("Swahili (Kiswahili)", "sw"),
                        new LanguageItem("Swedish (Svenska)", "sv"),
                        new LanguageItem("Tagalog", "tl"),
                        new LanguageItem("Tamil (தமிழ்)", "ta"),
                        new LanguageItem("Thai (ไทย)", "th"),
                        new LanguageItem("Turkish (Türkçe)", "tr"),
                        new LanguageItem("Ukrainian (Українська)", "uk"),
                        new LanguageItem("Urdu (اردو)", "ur"),
                        new LanguageItem("Vietnamese (Tiếng Việt)", "vi"),
                        new LanguageItem("Welsh (Cymraeg)", "cy")));
    }
}
