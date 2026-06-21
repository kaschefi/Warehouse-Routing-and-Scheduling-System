package com.warehouse.ui;

import com.warehouse.ui.WarehouseGridView.PlacementMode;
import com.warehouse.service.RobotManagementService;
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
    private final RobotManagementService robotManagementService;
    private final RightSidebarView rightSidebar;

    private Button btnSelect;
    private Button btnObstacle;
    private Button btnStation;
    private Button btnDropZone;
    private Button btnRobot;
    private Button btnClear;
    private Button btnLoadDemo;

    public LeftSidebarView(WarehouseGridView gridView, StatusBarView statusBar, RobotManagementService robotManagementService, RightSidebarView rightSidebar) {
        this.gridView = gridView;
        this.statusBar = statusBar;
        this.robotManagementService = robotManagementService;
        this.rightSidebar = rightSidebar;

        this.getStyleClass().add("left-sidebar");

        VBox card = new VBox(10);
        card.getStyleClass().add("tactical-card");

        Label title = new Label("WAREHOUSE CONTROLS");
        title.getStyleClass().add("card-title");

        // Select mode button
        btnSelect = new Button("Select Node");
        btnSelect.setMaxWidth(Double.MAX_VALUE);
        btnSelect.setOnAction(e -> {
            gridView.setCurrentPlacementMode(PlacementMode.SELECT);
            updateButtonStyles(btnSelect);
        });

        // Placing mode buttons
        btnObstacle = createButton("Add Obstacle", PlacementMode.OBSTACLE);
        btnStation = createButton("Add Charging Station", PlacementMode.CHARGING_STATION);
        btnDropZone = createButton("Add Drop Zone", PlacementMode.DROP_ZONE);

        btnRobot = new Button("Add Robot");
        btnRobot.setMaxWidth(Double.MAX_VALUE);
        btnRobot.setOnAction(e -> {
            gridView.setCurrentPlacementMode(PlacementMode.ROBOT);
            updateButtonStyles(btnRobot);
        });

        btnLoadDemo = new Button("Load Demo Scenario");
        btnLoadDemo.setMaxWidth(Double.MAX_VALUE);
        btnLoadDemo.getStyleClass().add("load-demo-btn");
        btnLoadDemo.setOnAction(e -> {
            gridView.clearMap();
            robotManagementService.clearFleet();
            rightSidebar.clearCustomTasks();

            gridView.loadDemoScenario();
            rightSidebar.loadDemoScenarioTasks(gridView.getStationLabels());

            gridView.setCurrentPlacementMode(PlacementMode.SELECT);
            updateButtonStyles(btnSelect);
        });

        btnClear = new Button("Clear Map");
        btnClear.setMaxWidth(Double.MAX_VALUE);
        btnClear.getStyleClass().add("clear-btn");
        btnClear.setOnAction(e -> {
            gridView.clearMap();
            robotManagementService.clearFleet();
            rightSidebar.clearCustomTasks();
            gridView.setCurrentPlacementMode(PlacementMode.SELECT);
            updateButtonStyles(btnSelect);
        });

        card.getChildren().addAll(title, btnSelect, btnObstacle, btnStation, btnDropZone, btnRobot, btnLoadDemo, btnClear);
        this.getChildren().add(card);
        
        // Initially set Select mode as highlighted
        updateButtonStyles(btnSelect);
    }

    private Button createButton(String text, PlacementMode mode) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> {
            gridView.setCurrentPlacementMode(mode);
            updateButtonStyles(btn);
        });
        return btn;
    }

    private void updateButtonStyles(Button activeBtn) {
        // If current placement mode is SELECT, select node is active
        if (gridView.getCurrentPlacementMode() == PlacementMode.SELECT) {
            activeBtn = btnSelect;
        }

        // Clear active styles
        btnSelect.getStyleClass().removeAll("active-select", "active-obstacle", "active-station", "active-dropzone", "active-robot");
        btnObstacle.getStyleClass().removeAll("active-select", "active-obstacle", "active-station", "active-dropzone", "active-robot");
        btnStation.getStyleClass().removeAll("active-select", "active-obstacle", "active-station", "active-dropzone", "active-robot");
        btnDropZone.getStyleClass().removeAll("active-select", "active-obstacle", "active-station", "active-dropzone", "active-robot");
        btnRobot.getStyleClass().removeAll("active-select", "active-obstacle", "active-station", "active-dropzone", "active-robot");

        if (activeBtn != null) {
            if (activeBtn == btnSelect) {
                btnSelect.getStyleClass().add("active-select");
                statusBar.setStatus("Active tool: Select / Navigation");
            } else if (activeBtn == btnObstacle) {
                btnObstacle.getStyleClass().add("active-obstacle");
                statusBar.setStatus("Active tool: Add Obstacle (Paint on grid)");
            } else if (activeBtn == btnStation) {
                btnStation.getStyleClass().add("active-station");
                statusBar.setStatus("Active tool: Add Charging Station (Paint on grid)");
            } else if (activeBtn == btnDropZone) {
                btnDropZone.getStyleClass().add("active-dropzone");
                statusBar.setStatus("Active tool: Add Drop Zone (Paint on grid)");
            } else if (activeBtn == btnRobot) {
                btnRobot.getStyleClass().add("active-robot");
                statusBar.setStatus("Active tool: Add Robot (Paint on grid)");
            }
        }
    }
}
