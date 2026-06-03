package com.warehouse.ui;

/**
 * A workaround launcher class to fix the "JavaFX runtime components are missing" error 
 * when running the application directly from an IDE.
 */
public class Launcher {
    public static void main(String[] args) {
        MainApplication.main(args);
    }
}
