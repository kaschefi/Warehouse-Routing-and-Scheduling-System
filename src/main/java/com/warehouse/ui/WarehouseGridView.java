package com.warehouse.ui;

import com.warehouse.model.domain.NodeType;
import com.warehouse.service.RoutingService;
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

    private final StatusBarView statusBar;
    private final RoutingService routingService;
    private final RightSidebarView rightSidebar;

    private final NodeType[][] gridState = new NodeType[ROWS][COLS];
    private NodeType currentPlacementMode = NodeType.EMPTY;

    public WarehouseGridView(StatusBarView statusBar, RoutingService routingService, RightSidebarView rightSidebar) {
        this.statusBar = statusBar;
        this.routingService = routingService;
        this.rightSidebar = rightSidebar;
        this.setAlignment(Pos.CENTER);
        this.setHgap(1);
        this.setVgap(1);
        this.setStyle("-fx-background-color: #d3d3d3; -fx-padding: 20;");

        // Initialize grid state matrix
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                gridState[r][c] = NodeType.EMPTY;
            }
        }

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
        rect.setStroke(Color.web("#e0e0e0"));
        rect.setArcWidth(4);
        rect.setArcHeight(4);

        cell.getChildren().add(rect);

        // Click handler to update state or select node for pathfinding
        cell.setOnMouseClicked(e -> {
            handleCellClick(row, col, rect);
        });

        // Hover effect to highlight path planning choices nicely
        cell.setOnMouseEntered(e -> {
            rect.setFill(Color.web("#ddecfa")); // Soft premium light blue hover
        });

        cell.setOnMouseExited(e -> {
            rect.setFill(getColorForNodeType(gridState[row][col]));
        });

        return cell;
    }

    private void handleCellClick(int row, int col, Rectangle rect) {
        String cellId = "N_" + col + "_" + row;
        
        if (currentPlacementMode != NodeType.EMPTY) {
            // Edit layout mode: toggle or update node type
            if (gridState[row][col] == currentPlacementMode) {
                gridState[row][col] = NodeType.EMPTY;
                rect.setFill(Color.WHITE);
                statusBar.setStatus("Cell " + cellId + " cleared to EMPTY");
            } else {
                gridState[row][col] = currentPlacementMode;
                rect.setFill(getColorForNodeType(currentPlacementMode));
                statusBar.setStatus("Cell " + cellId + " set to " + currentPlacementMode);
            }

            // If a cell becomes an obstacle, and it was previously selected for routing, clear it
            if (gridState[row][col] == NodeType.OBSTACLE) {
                if (rightSidebar != null) {
                    if (rightSidebar.getStartNodeText().equals(cellId)) {
                        rightSidebar.setStartNodeText("");
                    }
                    if (rightSidebar.getEndNodeText().equals(cellId)) {
                        rightSidebar.setEndNodeText("");
                    }
                }
            }
        } else {
            // Pathfinding Selection Mode
            if (gridState[row][col] == NodeType.OBSTACLE) {
                statusBar.setStatus("Warning: Cannot select an obstacle block (" + cellId + ") as a routing node!");
                return;
            }

            if (rightSidebar != null) {
                String start = rightSidebar.getStartNodeText();
                String end = rightSidebar.getEndNodeText();
                if (start.isEmpty()) {
                    rightSidebar.setStartNodeText(cellId);
                    statusBar.setStatus("Selected Start Node: " + cellId);
                } else if (end.isEmpty()) {
                    rightSidebar.setEndNodeText(cellId);
                    statusBar.setStatus("Selected End Node: " + cellId);
                } else {
                    rightSidebar.setStartNodeText(cellId);
                    rightSidebar.setEndNodeText("");
                    statusBar.setStatus("Selected Start Node: " + cellId + " (End Node cleared)");
                }
            } else {
                statusBar.setStatus("Cell selected: (" + col + "," + row + ")");
            }
        }
    }

    private Color getColorForNodeType(NodeType type) {
        if (type == null) return Color.WHITE;
        switch (type) {
            case OBSTACLE:
                return Color.web("#e74c3c"); // Soft modern red
            case CHARGING_STATION:
                return Color.web("#f39c12"); // Soft modern orange
            case DROP_ZONE:
                return Color.web("#2ecc71"); // Soft modern green
            case EMPTY:
            default:
                return Color.WHITE;
        }
    }

    public void clearMap() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                gridState[r][c] = NodeType.EMPTY;
            }
        }

        // Repaint all cell rectangles to white
        this.getChildren().forEach(node -> {
            if (node instanceof StackPane) {
                StackPane cell = (StackPane) node;
                if (!cell.getChildren().isEmpty() && cell.getChildren().get(0) instanceof Rectangle) {
                    Rectangle rect = (Rectangle) cell.getChildren().get(0);
                    rect.setFill(Color.WHITE);
                }
            }
        });

        statusBar.setStatus("Map cleared.");
    }

    public NodeType[][] getGridState() {
        return gridState;
    }

    public NodeType getCurrentPlacementMode() {
        return currentPlacementMode;
    }

    public void setCurrentPlacementMode(NodeType mode) {
        this.currentPlacementMode = mode;
    }
}
