package com.warehouse.ui;

import javafx.geometry.Pos;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Represents the 20x20 warehouse grid.
 * Displays the physical space where routing (Dijkstra) and MST will be visualized.
 */
public class WarehouseGridView extends GridPane {

    private static final int ROWS = 20;
    private static final int COLS = 20;
    private static final double CELL_SIZE = 30.0;

    private StatusBarView statusBar;

    public WarehouseGridView(StatusBarView statusBar) {
        this.statusBar = statusBar;
        this.setAlignment(Pos.CENTER);
        this.setHgap(1);
        this.setVgap(1);
        this.setStyle("-fx-background-color: #d3d3d3; -fx-padding: 20;");

        initializeGrid();
    }

    private void initializeGrid() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                StackPane cell = createCell(row, col);
                this.add(cell, col, row);
            }
        }
    }

    private StackPane createCell(int row, int col) {
        StackPane cell = new StackPane();
        Rectangle rect = new Rectangle(CELL_SIZE, CELL_SIZE);
        rect.setFill(Color.WHITE);
        rect.setStroke(Color.LIGHTGRAY);

        cell.getChildren().add(rect);

        // Click handler to print coordinates and update status bar
        cell.setOnMouseClicked(e -> {
            System.out.println("Cell clicked: (" + col + ", " + row + ")");
            statusBar.setStatus("Cell selected: (" + col + "," + row + ")");
            // TODO: Hook up selection logic for algorithms (e.g. setting start/end nodes)
        });

        // Hover effect
        cell.setOnMouseEntered(e -> rect.setFill(Color.LIGHTBLUE));
        cell.setOnMouseExited(e -> rect.setFill(Color.WHITE));

        return cell;
    }
}
