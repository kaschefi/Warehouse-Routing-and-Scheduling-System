package com.warehouse.ui;

import com.warehouse.model.domain.NodeType;
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

    private final WarehouseGridView gridView;
    private final StatusBarView statusBar;

    private Button btnSelect;
    private Button btnObstacle;
    private Button btnStation;
    private Button btnDropZone;
    private Button btnRobot;
    private Button btnClear;

    public LeftSidebarView(WarehouseGridView gridView, StatusBarView statusBar) {
        this.gridView = gridView;
        this.statusBar = statusBar;

        this.setPadding(new Insets(15));
        this.setSpacing(10);
        this.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e2e8f0; -fx-border-width: 0 1 0 0;");
        this.setPrefWidth(200);

        Label title = new Label("Warehouse Controls");
        title.setFont(Font.font("System", FontWeight.BOLD, 14));
        title.setStyle("-fx-text-fill: #2d3748; -fx-padding: 0 0 10 0;");

        // Select mode button
        btnSelect = new Button("Select Node");
        btnSelect.setMaxWidth(Double.MAX_VALUE);
        btnSelect.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #4a5568; -fx-border-color: #cbd5e0; -fx-border-radius: 4; -fx-background-radius: 4;");
        btnSelect.setOnAction(e -> {
            gridView.setCurrentPlacementMode(NodeType.EMPTY);
            updateButtonStyles(null);
        });

        // Placing mode buttons
        btnObstacle = createButton("Add Obstacle", NodeType.OBSTACLE);
        btnStation = createButton("Add Charging Station", NodeType.CHARGING_STATION);
        btnDropZone = createButton("Add Drop Zone", NodeType.DROP_ZONE);

        btnRobot = new Button("Add Robot");
        btnRobot.setMaxWidth(Double.MAX_VALUE);
        btnRobot.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #4a5568; -fx-border-color: #cbd5e0; -fx-border-radius: 4; -fx-background-radius: 4;");
        btnRobot.setOnAction(e -> {
            statusBar.setStatus("Add Robot functionality is not implemented yet.");
        });

        btnClear = new Button("Clear Map");
        btnClear.setMaxWidth(Double.MAX_VALUE);
        btnClear.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #e53e3e; -fx-border-color: #fc8181; -fx-border-radius: 4; -fx-background-radius: 4;");
        btnClear.setOnAction(e -> {
            gridView.clearMap();
            gridView.setCurrentPlacementMode(NodeType.EMPTY);
            updateButtonStyles(null);
        });

        this.getChildren().addAll(title, btnSelect, btnObstacle, btnStation, btnDropZone, btnRobot, btnClear);
        
        // Initially set Select mode as highlighted
        updateButtonStyles(null);
    }

    private Button createButton(String text, NodeType mode) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #4a5568; -fx-border-color: #cbd5e0; -fx-border-radius: 4; -fx-background-radius: 4;");
        btn.setOnAction(e -> {
            gridView.setCurrentPlacementMode(mode);
            updateButtonStyles(btn);
        });
        return btn;
    }

    private void updateButtonStyles(Button activeBtn) {
        // If current placement mode is EMPTY, select node is active
        if (gridView.getCurrentPlacementMode() == NodeType.EMPTY) {
            activeBtn = btnSelect;
        }

        String normalStyle = "-fx-background-color: #ffffff; -fx-text-fill: #4a5568; -fx-border-color: #cbd5e0; -fx-border-radius: 4; -fx-background-radius: 4;";
        btnSelect.setStyle(normalStyle);
        btnObstacle.setStyle(normalStyle);
        btnStation.setStyle(normalStyle);
        btnDropZone.setStyle(normalStyle);

        if (activeBtn != null) {
            String activeStyle = "";
            if (activeBtn == btnSelect) {
                activeStyle = "-fx-background-color: #3182ce; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-background-radius: 4;";
                statusBar.setStatus("Active tool: Select / Navigation");
            } else if (activeBtn == btnObstacle) {
                activeStyle = "-fx-background-color: #e74c3c; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-background-radius: 4;";
                statusBar.setStatus("Active tool: Add Obstacle (Paint on grid)");
            } else if (activeBtn == btnStation) {
                activeStyle = "-fx-background-color: #f39c12; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-background-radius: 4;";
                statusBar.setStatus("Active tool: Add Charging Station (Paint on grid)");
            } else if (activeBtn == btnDropZone) {
                activeStyle = "-fx-background-color: #2ecc71; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-background-radius: 4;";
                statusBar.setStatus("Active tool: Add Drop Zone (Paint on grid)");
            }
            activeBtn.setStyle(activeStyle);
        }
    }
}
