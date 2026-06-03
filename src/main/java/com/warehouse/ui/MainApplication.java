package com.warehouse.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main entry point for the RoboFlow application.
 * Initializes the primary stage and sets up the root layout.
 */
public class MainApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("RoboFlow");

        // Create the main view which acts as the root of the scene graph
        MainView root = new MainView();

        Scene scene = new Scene(root, 1024, 768);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
