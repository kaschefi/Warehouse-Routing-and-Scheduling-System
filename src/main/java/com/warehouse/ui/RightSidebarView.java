package com.warehouse.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * The right sidebar containing operations for pathfinding, MST, and scheduling.
 */
public class RightSidebarView extends VBox {

    public RightSidebarView() {
        this.setPadding(new Insets(15));
        this.setSpacing(10);
        this.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #cccccc; -fx-border-width: 0 0 0 1;");
        this.setPrefWidth(250);

        setupPathfindingSection();
        this.getChildren().add(new Separator());
        setupNetworkPlanningSection();
        this.getChildren().add(new Separator());
        setupTaskSchedulingSection();
    }

    private void setupPathfindingSection() {
        Label title = new Label("Pathfinding");
        title.setFont(Font.font("System", FontWeight.BOLD, 14));

        TextField startNodeField = new TextField();
        startNodeField.setPromptText("Start Node");

        TextField endNodeField = new TextField();
        endNodeField.setPromptText("End Node");

        Button btnFindPath = new Button("Find Shortest Path");
        btnFindPath.setMaxWidth(Double.MAX_VALUE);
        btnFindPath.setOnAction(e -> {
            // TODO: Call Dijkstra implementation and draw route on grid
            System.out.println("TODO: Find Shortest Path from " + startNodeField.getText() + " to " + endNodeField.getText());
        });

        this.getChildren().addAll(title, startNodeField, endNodeField, btnFindPath);
    }

    private void setupNetworkPlanningSection() {
        Label title = new Label("Network Planning");
        title.setFont(Font.font("System", FontWeight.BOLD, 14));

        Button btnMST = new Button("Generate MST");
        btnMST.setMaxWidth(Double.MAX_VALUE);
        btnMST.setOnAction(e -> {
            // TODO: Call Prim/Kruskal implementation and draw MST on grid
            System.out.println("TODO: Generate MST");
        });

        this.getChildren().addAll(title, btnMST);
    }

    private void setupTaskSchedulingSection() {
        Label title = new Label("Task Scheduling");
        title.setFont(Font.font("System", FontWeight.BOLD, 14));

        Button btnSchedule = new Button("Schedule Tasks");
        btnSchedule.setMaxWidth(Double.MAX_VALUE);
        btnSchedule.setOnAction(e -> {
            // TODO: Call Topological Sort implementation
            System.out.println("TODO: Schedule Tasks");
        });

        this.getChildren().addAll(title, btnSchedule);
    }
}
