package com.example;

/**
 * Main class of the runnable jar. Java refuses to start a class that extends
 * javafx.application.Application from a plain jar ("JavaFX runtime components are
 * missing"), so this class starts {@link Main} instead.
 */
public class Launcher {

    public static void main(String[] args) {
        Main.main(args);
    }
}
