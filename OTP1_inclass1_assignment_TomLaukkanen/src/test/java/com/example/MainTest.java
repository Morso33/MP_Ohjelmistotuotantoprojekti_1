package com.example;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    /** Feeds the given lines to the menu and returns everything it printed. */
    private String runWithInput(String... lines) {
        String input = String.join("\n", lines) + "\n";
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new Main().run(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(output, true, StandardCharsets.UTF_8));
        return output.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("Option 1 converts Fahrenheit to Celsius")
    void fahrenheitToCelsius() {
        assertTrue(runWithInput("1", "212", "0").contains("212.00 F = 100.00 C"));
    }

    @Test
    @DisplayName("Option 2 converts Celsius to Fahrenheit")
    void celsiusToFahrenheit() {
        assertTrue(runWithInput("2", "100", "0").contains("100.00 C = 212.00 F"));
    }

    @Test
    @DisplayName("Option 3 converts Kelvin to Celsius")
    void kelvinToCelsius() {
        assertTrue(runWithInput("3", "300", "0").contains("300.00 K = 26.85 C"));
    }

    @Test
    @DisplayName("Option 4 tells whether a temperature is extreme")
    void extremeTemperature() {
        String output = runWithInput("4", "60", "4", "20", "0");
        assertTrue(output.contains("60.00 C is extreme"));
        assertTrue(output.contains("20.00 C is not extreme"));
    }

    @Test
    @DisplayName("A comma works as the decimal separator")
    void acceptsCommaDecimal() {
        assertTrue(runWithInput("2", "37,5", "0").contains("37.50 C = 99.50 F"));
    }

    @Test
    @DisplayName("Non-numeric input is reported and the menu keeps going")
    void rejectsNonNumber() {
        String output = runWithInput("1", "hot", "0");
        assertTrue(output.contains("Not a number: hot"));
        assertTrue(output.contains("Goodbye!"));
    }

    @Test
    @DisplayName("Unknown menu options are reported")
    void unknownOption() {
        assertTrue(runWithInput("9", "0").contains("Unknown option: 9"));
    }

    @Test
    @DisplayName("The program exits cleanly when input runs out")
    void endOfInput() {
        assertTrue(runWithInput("1").contains("No more input, exiting."));
        assertTrue(runWithInput("5").contains("No more input, exiting."));
    }
}
