package com.example;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Scanner;

/**
 * Console app: asks for a language, then converts temperatures in that language.
 */
public class Main {

    static final String BUNDLE_NAME = "MessagesBundle";

    // Menu order of the language prompt: 1 = English (default), 2 = Finnish, 3 = Swedish, 4 = Japanese
    static final Locale[] LOCALES = {
            Locale.of("en", "US"),
            Locale.of("fi", "FI"),
            Locale.of("sv", "SE"),
            Locale.of("ja", "JP")
    };

    private final Scanner in;
    private final PrintStream out;
    private final TemperatureConverter converter = new TemperatureConverter();
    private Locale locale = LOCALES[0];
    private ResourceBundle messages;

    Main(InputStream in, PrintStream out) {
        this.in = new Scanner(in, StandardCharsets.UTF_8);
        this.out = out;
    }

    public static void main(String[] args) {
        // Always print UTF-8 so ä, ö, å and Japanese are written correctly
        // (on Windows switch the console to UTF-8 first with "chcp 65001")
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        new Main(System.in, out).run();
    }

    void run() {
        selectLanguage();
        out.println();
        out.println(messages.getString("app.title"));

        while (true) {
            printMenu();
            String choice = readLine();
            if (choice == null || choice.equals("0")) {
                break;
            }
            switch (choice) {
                case "1" -> convertFahrenheitToCelsius();
                case "2" -> convertCelsiusToFahrenheit();
                default -> out.println(messages.getString("error.choice"));
            }
        }
        out.println(messages.getString("goodbye"));
    }

    private void selectLanguage() {
        // Shown before a language is chosen, so it is written in all four languages
        out.println("Select language / Valitse kieli / Välj språk / 言語を選択してください:");
        out.println("  1. English");
        out.println("  2. Suomi");
        out.println("  3. Svenska");
        out.println("  4. 日本語");
        out.print("> ");
        locale = localeForChoice(readLine());
        messages = loadMessages(locale);
        out.println(messages.getString("language.selected"));
    }

    /** Returns the locale for a menu choice; anything else gives the default, English. */
    static Locale localeForChoice(String choice) {
        return switch (choice == null ? "" : choice) {
            case "2" -> LOCALES[1];
            case "3" -> LOCALES[2];
            case "4" -> LOCALES[3];
            default -> LOCALES[0];
        };
    }

    static ResourceBundle loadMessages(Locale locale) {
        // Since Java 9 properties bundles are read as UTF-8
        return ResourceBundle.getBundle(BUNDLE_NAME, locale);
    }

    private void printMenu() {
        out.println();
        out.println(messages.getString("menu.prompt"));
        out.println("  " + messages.getString("menu.f2c"));
        out.println("  " + messages.getString("menu.c2f"));
        out.println("  " + messages.getString("menu.exit"));
        out.print(messages.getString("menu.choice") + " ");
    }

    private void convertFahrenheitToCelsius() {
        Double fahrenheit = readTemperature("input.fahrenheit");
        if (fahrenheit == null) {
            return;
        }
        double celsius = converter.fahrenheitToCelsius(fahrenheit);
        out.println(format("result.f2c", fahrenheit, celsius));
        warnIfExtreme(celsius);
    }

    private void convertCelsiusToFahrenheit() {
        Double celsius = readTemperature("input.celsius");
        if (celsius == null) {
            return;
        }
        out.println(format("result.c2f", celsius, converter.celsiusToFahrenheit(celsius)));
        warnIfExtreme(celsius);
    }

    private void warnIfExtreme(double celsius) {
        if (converter.isExtremeTemperature(celsius)) {
            out.println(format("warning.extreme", celsius));
        }
    }

    /** Asks until a number is given. Returns null if the input ends. */
    private Double readTemperature(String promptKey) {
        while (true) {
            out.print(messages.getString(promptKey) + " ");
            String line = readLine();
            if (line == null) {
                return null;
            }
            Double value = parseTemperature(line);
            if (value != null) {
                return value;
            }
            out.println(messages.getString("error.number"));
        }
    }

    /** Parses a temperature, accepting a comma as the decimal separator (37,5). Returns null if invalid. */
    static Double parseTemperature(String text) {
        try {
            double value = Double.parseDouble(text.trim().replace(',', '.'));
            return Double.isFinite(value) ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String readLine() {
        return in.hasNextLine() ? in.nextLine().trim() : null;
    }

    /** Formats a message with the numbers written the way the chosen locale writes them. */
    private String format(String key, Object... args) {
        return new MessageFormat(messages.getString(key), locale).format(args);
    }
}
