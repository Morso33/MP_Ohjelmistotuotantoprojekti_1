package com.example;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Locale;
import java.util.Scanner;

/**
 * Console front end for {@link TemperatureConverter}. Shows a menu and
 * converts the numbers the user types, so the app can be tried out with
 * {@code docker run -it}.
 */
public class Main {

    private final TemperatureConverter converter = new TemperatureConverter();

    public static void main(String[] args) {
        new Main().run(System.in, System.out);
    }

    /**
     * Runs the menu loop until the user picks 0 or the input ends.
     *
     * @param input where the user's choices and numbers are read from
     * @param out   where the menu and results are printed
     */
    void run(InputStream input, PrintStream out) {
        Scanner scanner = new Scanner(input);
        out.println("=== Temperature Converter ===");

        while (true) {
            out.println();
            out.println("1) Fahrenheit -> Celsius");
            out.println("2) Celsius -> Fahrenheit");
            out.println("3) Kelvin -> Celsius");
            out.println("4) Is a Celsius temperature extreme?");
            out.println("0) Exit");
            out.print("Choose an option: ");

            if (!scanner.hasNextLine()) {
                out.println();
                out.println("No more input, exiting.");
                return;
            }
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "0" -> {
                    out.println("Goodbye!");
                    return;
                }
                case "1" -> {
                    Double f = readNumber(scanner, out, "Temperature in Fahrenheit: ");
                    if (f != null) {
                        out.println(format(f) + " F = " + format(converter.fahrenheitToCelsius(f)) + " C");
                    }
                }
                case "2" -> {
                    Double c = readNumber(scanner, out, "Temperature in Celsius: ");
                    if (c != null) {
                        out.println(format(c) + " C = " + format(converter.celsiusToFahrenheit(c)) + " F");
                    }
                }
                case "3" -> {
                    Double k = readNumber(scanner, out, "Temperature in Kelvin: ");
                    if (k != null) {
                        out.println(format(k) + " K = " + format(converter.kelvinToCelsius(k)) + " C");
                    }
                }
                case "4" -> {
                    Double c = readNumber(scanner, out, "Temperature in Celsius: ");
                    if (c != null) {
                        boolean extreme = converter.isExtremeTemperature(c);
                        out.println(format(c) + " C is " + (extreme ? "extreme" : "not extreme"));
                    }
                }
                default -> out.println("Unknown option: " + choice);
            }
        }
    }

    /** Prompts for one number; returns null if the input is missing or not a number. */
    private Double readNumber(Scanner scanner, PrintStream out, String prompt) {
        out.print(prompt);
        if (!scanner.hasNextLine()) {
            out.println();
            return null;
        }
        String text = scanner.nextLine().trim().replace(',', '.');
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            out.println("Not a number: " + text);
            return null;
        }
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
