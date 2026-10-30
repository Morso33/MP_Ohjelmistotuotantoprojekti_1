package com.example;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageTest {

    @Test
    @DisplayName("The five languages of the assignment, in drop-down order")
    void fiveLanguages() {
        assertEquals(List.of("en_US", "fi_FI", "sv_SE", "ja_JP", "ar_SA"),
                List.of(Language.values()).stream().map(Language::getCode).toList());
    }

    @ParameterizedTest(name = "{0} is shown as {1}")
    @CsvSource({
            "ENGLISH,  English",
            "FINNISH,  Suomi",
            "SWEDISH,  Svenska",
            "JAPANESE, 日本語",
            "ARABIC,   العربية"
    })
    void nativeNames(Language language, String expectedName) {
        assertEquals(expectedName, language.getNativeName());
        assertEquals(expectedName, language.toString(), "the drop-down shows the native name");
    }

    @Test
    @DisplayName("Only Arabic is written right to left")
    void onlyArabicIsRightToLeft() {
        for (Language language : Language.values()) {
            assertEquals(language == Language.ARABIC, language.isRightToLeft(), language.name());
        }
    }

    @Test
    @DisplayName("The code is the locale as text")
    void codeIsLocale() {
        assertEquals(Locale.of("ja", "JP"), Language.JAPANESE.getLocale());
        assertEquals("ja_JP", Language.JAPANESE.getCode());
    }

    @ParameterizedTest(name = "fromCode finds {0}")
    @EnumSource(Language.class)
    void fromCodeFindsEveryLanguage(Language language) {
        assertSame(language, Language.fromCode(language.getCode()));
    }

    @ParameterizedTest(name = "\"{0}\" falls back to English")
    @NullSource
    @ValueSource(strings = {"", "de_DE", "fi", "FI_fi"})
    void unknownCodeFallsBackToEnglish(String code) {
        assertSame(Language.ENGLISH, Language.fromCode(code));
    }

    // --- resource bundles ---

    @ParameterizedTest(name = "{0} has its own bundle")
    @EnumSource(Language.class)
    void everyLanguageHasItsOwnBundle(Language language) {
        // Not a fallback to another language's file, whatever the computer's default locale is
        assertEquals(language.getLocale(), language.getBundle().getLocale());
    }

    @ParameterizedTest(name = "{0} has the same keys as English")
    @EnumSource(Language.class)
    void allBundlesHaveTheSameKeys(Language language) {
        assertEquals(Language.ENGLISH.getBundle().keySet(), language.getBundle().keySet());
    }

    @ParameterizedTest(name = "{0} is translated")
    @EnumSource(value = Language.class, names = "ENGLISH", mode = EnumSource.Mode.EXCLUDE)
    void otherLanguagesAreTranslated(Language language) {
        ResourceBundle english = Language.ENGLISH.getBundle();
        ResourceBundle bundle = language.getBundle();
        for (String key : List.of("app.title", "button.convert", "button.clear", "history.title", "error.empty")) {
            assertFalse(english.getString(key).equals(bundle.getString(key)), key + " in " + language);
        }
    }

    @ParameterizedTest(name = "MessagesBundle_{0}.properties is valid UTF-8")
    @EnumSource(Language.class)
    void bundleFilesAreValidUtf8(Language language) throws IOException {
        String file = "/" + Language.BUNDLE_NAME + "_" + language.getCode() + ".properties";
        try (InputStream stream = LanguageTest.class.getResourceAsStream(file)) {
            assertNotNull(stream, file);
            byte[] bytes = stream.readAllBytes();
            // A strict decoder fails on anything that is not UTF-8 (e.g. an ISO-8859-1 ä)
            assertDoesNotThrow(() -> StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)), file);
        }
    }

    @Test
    @DisplayName("Letters outside ISO-8859-1 are read correctly from the bundles")
    void nonAsciiTextIsReadCorrectly() {
        assertEquals("Lämpötilamuunnin", Language.FINNISH.getBundle().getString("app.title"));
        assertEquals("Temperaturomvandlare", Language.SWEDISH.getBundle().getString("app.title"));
        assertEquals("Språk:", Language.SWEDISH.getBundle().getString("label.language"));
        assertEquals("温度変換ツール", Language.JAPANESE.getBundle().getString("app.title"));
        assertEquals("محول درجات الحرارة", Language.ARABIC.getBundle().getString("app.title"));
    }

    @Test
    @DisplayName("Every language has a row with the same code in the ui_language table")
    void everyLanguageIsInTheDatabase() throws SQLException {
        DBConnection database = TestDatabase.withSchema();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT name FROM ui_language WHERE code = ?")) {
            for (Language language : Language.values()) {
                statement.setString(1, language.getCode());
                try (ResultSet rs = statement.executeQuery()) {
                    assertTrue(rs.next(), language.getCode());
                    assertEquals(language.getNativeName(), rs.getString("name"));
                }
            }
        }
    }
}
