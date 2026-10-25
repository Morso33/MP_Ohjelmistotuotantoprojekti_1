package com.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    /** Runs the app with the given lines as keyboard input and returns everything it printed. */
    private static String run(String... lines) {
        String input = lines.length == 0 ? "" : String.join("\n", lines) + "\n";
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new Main(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(output, true, StandardCharsets.UTF_8)).run();
        return output.toString(StandardCharsets.UTF_8);
    }

    // --- language selection ---

    @ParameterizedTest(name = "choice {0} -> {1}")
    @CsvSource({"1, en_US", "2, fi_FI", "3, sv_SE", "4, ja_JP"})
    void languageChoiceGivesLocale(String choice, String expectedLocale) {
        assertEquals(expectedLocale, Main.localeForChoice(choice).toString());
    }

    @ParameterizedTest(name = "\"{0}\" falls back to English")
    @NullSource
    @ValueSource(strings = {"", "5", "0", "ja"})
    void invalidLanguageChoiceFallsBackToEnglish(String choice) {
        assertEquals(Locale.of("en", "US"), Main.localeForChoice(choice));
    }

    @ParameterizedTest(name = "choice {0} prints \"{1}\"")
    @CsvSource({
            "1, Temperature Converter",
            "2, Lämpötilamuunnin",
            "3, Temperaturomvandlare",
            "4, 温度変換ツール"
    })
    void appRunsInSelectedLanguage(String choice, String expectedTitle) {
        String output = run(choice, "0");
        assertTrue(output.contains(expectedTitle), output);
    }

    @Test
    void japaneseConversionIsLocalized() {
        String output = run("4", "2", "25", "0");
        assertTrue(output.contains("摂氏の温度を入力してください:"), output);
        assertTrue(output.contains("25 °C = 77 °F"), output);
        assertTrue(output.contains("さようなら！"), output);
    }

    @Test
    void finnishNumbersUseDecimalComma() {
        String output = run("2", "2", "37,5", "0");
        assertTrue(output.contains("37,5 °C = 99,5 °F"), output);
    }

    // --- conversions ---

    @Test
    void convertsFahrenheitToCelsius() {
        String output = run("1", "1", "98.6", "0");
        assertTrue(output.contains("98.6 °F = 37 °C"), output);
        assertFalse(output.contains("Warning"), output);
        assertTrue(output.contains("Goodbye!"), output);
    }

    @Test
    void convertsCelsiusToFahrenheit() {
        String output = run("1", "2", "100", "0");
        assertTrue(output.contains("100 °C = 212 °F"), output);
    }

    @Test
    void warnsAboutExtremeHeat() {
        String output = run("1", "2", "51", "0");
        assertTrue(output.contains("Warning: 51 °C is an extreme temperature!"), output);
    }

    @Test
    void warnsAboutExtremeColdAfterFahrenheitConversion() {
        String output = run("1", "1", "-50", "0");
        assertTrue(output.contains("-50 °F = -45.56 °C"), output);
        assertTrue(output.contains("Warning: -45.56 °C is an extreme temperature!"), output);
    }

    // --- invalid input ---

    @Test
    void invalidNumberIsAskedAgain() {
        String output = run("1", "2", "abc", "20", "0");
        assertTrue(output.contains("Invalid number, please try again."), output);
        assertTrue(output.contains("20 °C = 68 °F"), output);
    }

    @Test
    void invalidMenuChoiceIsReported() {
        String output = run("1", "9", "0");
        assertTrue(output.contains("Invalid choice, please try again."), output);
    }

    @Test
    void endOfInputExitsCleanly() {
        assertTrue(run().contains("Goodbye!"));
        assertTrue(run("1", "1").contains("Goodbye!"));
        assertTrue(run("1", "2").contains("Goodbye!"));
    }

    @ParameterizedTest(name = "\"{0}\" is not a temperature")
    @ValueSource(strings = {"", "abc", "1.2.3", "NaN", "Infinity"})
    void parseRejectsInvalidText(String text) {
        assertNull(Main.parseTemperature(text));
    }

    @Test
    void parseAcceptsCommaAndWhitespace() {
        assertEquals(37.5, Main.parseTemperature(" 37,5 "));
        assertEquals(-40.0, Main.parseTemperature("-40"));
    }

    @Test
    void mainStartsAndExitsWithStandardStreams() {
        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream("4\n0\n".getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
            assertDoesNotThrow(() -> Main.main(new String[0]));
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
        }
        // main() writes UTF-8 whatever the console encoding is
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("さようなら！"));
    }

    // --- resource bundles ---

    @Test
    void allBundlesHaveTheSameKeysAsEnglish() {
        ResourceBundle english = Main.loadMessages(Main.LOCALES[0]);
        for (Locale locale : Main.LOCALES) {
            ResourceBundle bundle = Main.loadMessages(locale);
            assertEquals(locale, bundle.getLocale());
            assertEquals(english.keySet(), bundle.keySet(), "keys of " + locale);
        }
    }

    @Test
    void bundleFilesAreValidUtf8() throws IOException {
        for (Locale locale : Main.LOCALES) {
            String file = "/" + Main.BUNDLE_NAME + "_" + locale + ".properties";
            try (InputStream stream = MainTest.class.getResourceAsStream(file)) {
                assertNotNull(stream, file);
                byte[] bytes = stream.readAllBytes();
                // A strict decoder fails on anything that is not UTF-8 (e.g. ISO-8859-1 ä)
                assertDoesNotThrow(() -> StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes)), file);
            }
        }
    }

    @Test
    void nonAsciiTextIsReadCorrectly() {
        assertEquals("Näkemiin!", Main.loadMessages(Main.LOCALES[1]).getString("goodbye"));
        assertEquals("Hej då!", Main.loadMessages(Main.LOCALES[2]).getString("goodbye"));
        assertEquals("さようなら！", Main.loadMessages(Main.LOCALES[3]).getString("goodbye"));
    }
}
