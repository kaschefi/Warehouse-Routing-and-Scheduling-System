package com.warehouse.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/**
 * The bottom status bar for displaying application state and messages.
 */
public class StatusBarView extends HBox {

    private Label statusLabel;

    public StatusBarView() {
        this.setPadding(new Insets(5, 10, 5, 10));
        this.setStyle("-fx-background-color: #e0e0e0; -fx-border-color: #cccccc; -fx-border-width: 1 0 0 0;");

        statusLabel = new Label("Ready");
        this.getChildren().add(statusLabel);
    }

    public void setStatus(String message) {
        statusLabel.setText(message);
    }
}
