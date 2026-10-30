package com.example;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * The languages the window can be shown in. Each one has a resource bundle
 * MessagesBundle_&lt;locale&gt;.properties and a row in the ui_language table with the same code.
 */
public enum Language {

    ENGLISH(Locale.of("en", "US"), "English", false),
    FINNISH(Locale.of("fi", "FI"), "Suomi", false),
    SWEDISH(Locale.of("sv", "SE"), "Svenska", false),
    JAPANESE(Locale.of("ja", "JP"), "日本語", false),
    ARABIC(Locale.of("ar", "SA"), "العربية", true);

    /** Base name of the .properties files in src/main/resources. */
    public static final String BUNDLE_NAME = "MessagesBundle";

    private final Locale locale;
    private final String nativeName;
    private final boolean rightToLeft;

    Language(Locale locale, String nativeName, boolean rightToLeft) {
        this.locale = locale;
        this.nativeName = nativeName;
        this.rightToLeft = rightToLeft;
    }

    public Locale getLocale() {
        return locale;
    }

    /** The locale as text, e.g. "fi_FI". Also the code in the ui_language table. */
    public String getCode() {
        return locale.toString();
    }

    /** The name of the language in the language itself, e.g. "Suomi". */
    public String getNativeName() {
        return nativeName;
    }

    /** True for Arabic, which is written from right to left, so the window is mirrored. */
    public boolean isRightToLeft() {
        return rightToLeft;
    }

    /** The texts of the window in this language. Since Java 9 the .properties files are read as UTF-8. */
    public ResourceBundle getBundle() {
        return ResourceBundle.getBundle(BUNDLE_NAME, locale);
    }

    /** The language with the given code, or English if there is none. */
    public static Language fromCode(String code) {
        for (Language language : values()) {
            if (language.getCode().equals(code)) {
                return language;
            }
        }
        return ENGLISH;
    }

    /** Shown in the language drop-down, so every language is listed in its own script. */
    @Override
    public String toString() {
        return nativeName;
    }
}
