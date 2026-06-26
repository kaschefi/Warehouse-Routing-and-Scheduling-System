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
        this.getStyleClass().add("status-bar-pane");

        statusLabel = new Label();
        statusLabel.getStyleClass().add("status-default");
        this.getChildren().add(statusLabel);
        
        setStatus("SYSTEM READY");
    }

    public void setStatus(String message) {
        if (message == null) {
            message = "";
        }
        
        statusLabel.getStyleClass().removeAll("status-default", "status-calculation", "status-error");
        
        String lower = message.toLowerCase();
        if (lower.contains("error") || lower.contains("warning") || lower.contains("circular") || lower.contains("deadlock") || lower.contains("loop") || lower.contains("impossible")) {
            statusLabel.getStyleClass().add("status-error");
            statusLabel.setText("> [ALERT] " + message.toUpperCase());
        } else if (lower.contains("calculated") || lower.contains("established") || lower.contains("cost") || lower.contains("schedule") || lower.contains("roadmap") || lower.contains("order") || lower.contains("steps")) {
            statusLabel.getStyleClass().add("status-calculation");
            statusLabel.setText("> [COMPUTE] " + message.toUpperCase());
        } else {
            statusLabel.getStyleClass().add("status-default");
            statusLabel.setText("> " + message.toUpperCase());
        }
    }
}
