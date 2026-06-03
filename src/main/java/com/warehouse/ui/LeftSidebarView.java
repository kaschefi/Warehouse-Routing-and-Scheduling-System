package com.warehouse.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * The left sidebar containing warehouse controls.
 * Used for modifying the map (adding obstacles, charging stations, etc.).
 */
public class LeftSidebarView extends VBox {

    public LeftSidebarView() {
        this.setPadding(new Insets(15));
        this.setSpacing(10);
        this.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #cccccc; -fx-border-width: 0 1 0 0;");
        this.setPrefWidth(200);

        Label title = new Label("Warehouse Controls");
        title.setFont(Font.font("System", FontWeight.BOLD, 14));

        Button btnObstacle = createButton("Add Obstacle");
        Button btnStation = createButton("Add Charging Station");
        Button btnDropZone = createButton("Add Drop Zone");
        Button btnRobot = createButton("Add Robot");
        Button btnClear = createButton("Clear Map");

        this.getChildren().addAll(title, btnObstacle, btnStation, btnDropZone, btnRobot, btnClear);
    }

    private Button createButton(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> {
            // TODO: Implement functionality
            System.out.println("TODO: " + text);
        });
        return btn;
    }
}
