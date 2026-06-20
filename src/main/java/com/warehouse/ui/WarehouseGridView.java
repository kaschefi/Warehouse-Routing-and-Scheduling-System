package com.warehouse.ui;

import com.warehouse.model.domain.Node;
import com.warehouse.model.domain.NodeType;
import com.warehouse.model.domain.Robot;
import com.warehouse.model.graph.Edge;
import com.warehouse.model.graph.Graph;
import com.warehouse.service.RoutingService;
import com.warehouse.service.RobotManagementService;
import javafx.geometry.Pos;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import java.util.List;

/**
 * Represents the 20x20 warehouse grid.
 * Displays the physical space where routing (Dijkstra) and MST will be visualized.
 */
public class WarehouseGridView extends GridPane {

    public enum PlacementMode {
        SELECT,
        OBSTACLE,
        CHARGING_STATION,
        DROP_ZONE,
        ROBOT
    }

    private static final int ROWS = 15;
    private static final int COLS = 15;
    private static final double CELL_SIZE = 40.0;

    private final StatusBarView statusBar;
    private final RoutingService routingService;
    private final RightSidebarView rightSidebar;
    private final RobotManagementService robotManagementService;

    private final NodeType[][] gridState = new NodeType[ROWS][COLS];
    private final StackPane[][] cellGrid = new StackPane[ROWS][COLS];
    private PlacementMode currentPlacementMode = PlacementMode.SELECT;

    private final java.util.Map<String, String> stationLabels = new java.util.HashMap<>();
    private int chargingStationCounter = 0;
    private int dropZoneCounter = 0;
    private javafx.animation.Timeline dijkstraTimeline;
    private javafx.animation.Timeline mstTimeline;
    private final java.util.Set<String> activePathCells = new java.util.HashSet<>();

    public java.util.Map<String, String> getStationLabels() {
        return stationLabels;
    }

    public WarehouseGridView(StatusBarView statusBar,
                             RoutingService routingService,
                             RightSidebarView rightSidebar,
                             RobotManagementService robotManagementService) {
        this.statusBar = statusBar;
        this.routingService = routingService;
        this.rightSidebar = rightSidebar;
        this.robotManagementService = robotManagementService;
        this.setAlignment(Pos.CENTER);
        this.setHgap(1);
        this.setVgap(1);
        this.getStyleClass().add("warehouse-grid-pane");

        // Initialize grid state matrix
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                gridState[r][c] = NodeType.EMPTY;
            }
        }

        // Apply explicit absolute, non-resizable constraints to all columns and rows
        for (int c = 0; c < COLS; c++) {
            javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
            cc.setMinWidth(CELL_SIZE);
            cc.setPrefWidth(CELL_SIZE);
            cc.setMaxWidth(CELL_SIZE);
            this.getColumnConstraints().add(cc);
        }
        for (int r = 0; r < ROWS; r++) {
            javafx.scene.layout.RowConstraints rc = new javafx.scene.layout.RowConstraints();
            rc.setMinHeight(CELL_SIZE);
            rc.setPrefHeight(CELL_SIZE);
            rc.setMaxHeight(CELL_SIZE);
            this.getRowConstraints().add(rc);
        }

        initializeGrid();
    }

    private void initializeGrid() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                StackPane cell = createCell(row, col);
                cellGrid[row][col] = cell;
                this.add(cell, col, row);
            }
        }
    }

    private void updateRobotTelemetryHUD(Robot robot, StackPane robotVisual) {
        if (robotVisual == null) return;
        Text hudText = (Text) robotVisual.lookup("#robot-hud-" + robot.getId());
        if (hudText != null) {
            String taskId = robot.getActiveTaskId();
            if (taskId == null || taskId.isEmpty() || taskId.equals("None")) {
                hudText.setText(String.format("%s: %.0f%%", robot.getId(), robot.getBatteryLevel()));
            } else {
                hudText.setText(String.format("%s: %.0f%% [%s]", robot.getId(), robot.getBatteryLevel(), taskId));
            }
        }
    }

    private StackPane createRobotVisual(Robot robot) {
        StackPane robotVisual = new StackPane();
        robotVisual.setId("robot-visual-" + robot.getId());
        
        // 3D Composite AGV Model
        javafx.scene.shape.Box chassis = new javafx.scene.shape.Box(22, 5, 18);
        javafx.scene.paint.PhongMaterial chassisMaterial = new javafx.scene.paint.PhongMaterial();
        chassisMaterial.setDiffuseColor(Color.web("#FF5722")); // Industrial Orange
        chassisMaterial.setSpecularColor(Color.WHITE);
        chassis.setMaterial(chassisMaterial);
        
        // Rolling Wheel Drive System underneath the chassis
        javafx.scene.shape.Cylinder wheel = new javafx.scene.shape.Cylinder(4, 22);
        javafx.scene.paint.PhongMaterial wheelMat = new javafx.scene.paint.PhongMaterial();
        wheelMat.setDiffuseColor(Color.web("#333333")); // Charcoal gray
        wheel.setMaterial(wheelMat);
        
        // Rotate cylinder 90 degrees on the Z-axis to roll horizontally
        javafx.scene.transform.Rotate wheelRotate = new javafx.scene.transform.Rotate(90, javafx.scene.transform.Rotate.Z_AXIS);
        wheel.getTransforms().add(wheelRotate);
        wheel.setTranslateY(5.0); // Positioned underneath chassis deck (bottom of chassis is Y=2.5)
        
        // "Face Plate" Monitor Bracket & Face Screen Assembly (Console)
        javafx.scene.shape.Box monitorBracket = new javafx.scene.shape.Box(16, 10, 3);
        javafx.scene.paint.PhongMaterial bracketMat = new javafx.scene.paint.PhongMaterial();
        bracketMat.setDiffuseColor(Color.web("#222222")); // Matte black casing
        monitorBracket.setMaterial(bracketMat);
        
        javafx.scene.shape.Box faceScreen = new javafx.scene.shape.Box(14, 8, 1);
        javafx.scene.paint.PhongMaterial screenMat = new javafx.scene.paint.PhongMaterial();
        screenMat.setDiffuseColor(Color.web("#00E5FF")); // Glowing Matrix Cyan face screen
        screenMat.setSpecularColor(Color.WHITE);
        faceScreen.setMaterial(screenMat);
        faceScreen.setTranslateZ(1.6); // Position on the front face of the bracket
        
        // Top Scanning Beacon (translucent warning red)
        javafx.scene.shape.Sphere beacon = new javafx.scene.shape.Sphere(3);
        javafx.scene.paint.PhongMaterial beaconMat = new javafx.scene.paint.PhongMaterial();
        beaconMat.setDiffuseColor(new Color(1.0, 0.2, 0.2, 0.7)); // Glowing translucent red
        beacon.setMaterial(beaconMat);
        beacon.setTranslateY(-6.0); // Sits on top of the monitor bracket (height 10, top is Y=-5)
        
        // Console sub-group for easier unified rotation and translation
        javafx.scene.Group consoleGroup = new javafx.scene.Group();
        consoleGroup.getChildren().addAll(monitorBracket, faceScreen, beacon);
        consoleGroup.setTranslateZ(4.0); // Front third of chassis
        consoleGroup.setTranslateY(-7.5); // Sits on top of chassis (top is Y=-2.5)
        
        // Angle the console slightly backward
        javafx.scene.transform.Rotate consoleRotate = new javafx.scene.transform.Rotate(-15, javafx.scene.transform.Rotate.X_AXIS);
        consoleGroup.getTransforms().add(consoleRotate);
        
        // Group the 3D elements
        javafx.scene.Group agvGroup = new javafx.scene.Group();
        agvGroup.getChildren().addAll(chassis, wheel, consoleGroup);
        
        // Isometric angle transformation
        javafx.scene.transform.Rotate rx = new javafx.scene.transform.Rotate(25, javafx.scene.transform.Rotate.X_AXIS);
        javafx.scene.transform.Rotate ry = new javafx.scene.transform.Rotate(40, javafx.scene.transform.Rotate.Y_AXIS);
        agvGroup.getTransforms().addAll(rx, ry);
        
        // Floating Telemetry HUD
        Text hudText = new Text();
        hudText.setId("robot-hud-" + robot.getId());
        hudText.setFill(Color.web("#00FF66")); // Glowing matrix green
        hudText.setFont(Font.font("Courier New", FontWeight.BOLD, 9.0));
        hudText.setTranslateY(-24.0); // Shifted higher to clear the new tall mast tower perfectly
        
        javafx.scene.effect.DropShadow hudGlow = new javafx.scene.effect.DropShadow();
        hudGlow.setColor(Color.web("#00FF66"));
        hudGlow.setRadius(2.0);
        hudGlow.setSpread(0.2);
        hudText.setEffect(hudGlow);
        
        robotVisual.getChildren().addAll(agvGroup, hudText);
        updateRobotTelemetryHUD(robot, robotVisual);
        return robotVisual;
    }

    private StackPane createCell(int row, int col) {
        StackPane cell = new StackPane();
        cell.setMinSize(CELL_SIZE, CELL_SIZE);
        cell.setPrefSize(CELL_SIZE, CELL_SIZE);
        cell.setMaxSize(CELL_SIZE, CELL_SIZE);
        Rectangle rect = new Rectangle(CELL_SIZE, CELL_SIZE);
        rect.setFill(Color.web("#0B132B"));
        rect.setStroke(Color.web("#00FF66", 0.15));
        rect.setArcWidth(4);
        rect.setArcHeight(4);

        cell.getChildren().add(rect);

        // Click handler to update state or select node for pathfinding
        cell.setOnMouseClicked(e -> {
            handleCellClick(row, col, rect);
        });

        // Hover effect to highlight path planning choices nicely
        cell.setOnMouseEntered(e -> {
            String cellId = "N_" + col + "_" + row;
            if (activePathCells.contains(cellId)) {
                rect.setFill(Color.web("#00E5FF", 0.4));
                rect.setStroke(Color.web("#00FF66"));
                rect.setStrokeWidth(1.5);
            } else {
                rect.setFill(Color.web("#1C2541")); // Lighter obsidian hover
                rect.setStroke(Color.web("#00FF66")); // Bright neon green border
                rect.setStrokeWidth(1.5);
            }
        });

        cell.setOnMouseExited(e -> {
            String cellId = "N_" + col + "_" + row;
            if (activePathCells.contains(cellId)) {
                rect.setFill(Color.web("#00E5FF", 0.4));
                rect.setStroke(Color.web("#00E5FF", 0.4));
                rect.setStrokeWidth(1.5);
            } else {
                rect.setFill(getColorForNodeType(gridState[row][col]));
                if (gridState[row][col] == NodeType.CHARGING_STATION) {
                    rect.setStroke(Color.web("#FFB703"));
                    rect.setStrokeWidth(1.5);
                } else if (gridState[row][col] == NodeType.DROP_ZONE) {
                    rect.setStroke(Color.web("#00FF66"));
                    rect.setStrokeWidth(1.5);
                } else {
                    rect.setStroke(Color.web("#00FF66", 0.15));
                    rect.setStrokeWidth(1.0);
                }
            }
        });

        return cell;
    }

    private void handleCellClick(int row, int col, Rectangle rect) {
        String cellId = "N_" + col + "_" + row;
        
        if (currentPlacementMode == PlacementMode.ROBOT) {
            if (gridState[row][col] == NodeType.OBSTACLE) {
                statusBar.setStatus("Warning: Cannot place a robot on an obstacle block!");
                return;
            }
            
            // Build the graph database to fetch a valid node reference
            routingService.generateGraphFromGrid(gridState);
            Graph graph = routingService.getWarehouseMap();
            Node targetNode = null;
            for (Node n : graph.getNodes()) {
                if (n.getId().equals(cellId)) {
                    targetNode = n;
                    break;
                }
            }
            if (targetNode == null) {
                targetNode = new Node(cellId, col, row, gridState[row][col]);
            }
            
            Robot robot = robotManagementService.registerRobot(targetNode);
            
            // Render the robot visually inside the StackPane cell
            StackPane cell = cellGrid[row][col];
            cell.getChildren().removeIf(node -> node.getId() != null && node.getId().startsWith("robot-visual-"));
            cell.getChildren().add(createRobotVisual(robot));
            
            statusBar.setStatus("Placed " + robot.getName() + " at " + cellId);
            
        } else if (currentPlacementMode != PlacementMode.SELECT) {
            NodeType targetType = NodeType.EMPTY;
            if (currentPlacementMode == PlacementMode.OBSTACLE) {
                targetType = NodeType.OBSTACLE;
            } else if (currentPlacementMode == PlacementMode.CHARGING_STATION) {
                targetType = NodeType.CHARGING_STATION;
            } else if (currentPlacementMode == PlacementMode.DROP_ZONE) {
                targetType = NodeType.DROP_ZONE;
            }
            
            StackPane cell = cellGrid[row][col];
            // Clear any direct Text children (old labels)
            cell.getChildren().removeIf(node -> node instanceof Text);
            stationLabels.remove(cellId);

            // Edit layout mode: toggle or update node type
            if (gridState[row][col] == targetType) {
                gridState[row][col] = NodeType.EMPTY;
                rect.setFill(Color.web("#0B132B"));
                rect.setStroke(Color.web("#00FF66", 0.15));
                rect.setStrokeWidth(1.0);
                statusBar.setStatus("Cell " + cellId + " cleared to EMPTY");
            } else {
                gridState[row][col] = targetType;
                rect.setFill(getColorForNodeType(targetType));
                statusBar.setStatus("Cell " + cellId + " set to " + targetType);

                if (targetType == NodeType.CHARGING_STATION) {
                    chargingStationCounter++;
                    String label = "CS" + chargingStationCounter;
                    stationLabels.put(cellId, label);
                    Text textLabel = new Text(label);
                    textLabel.setFill(Color.web("#FFB703")); // Glowing amber
                    textLabel.setFont(Font.font("Courier New", FontWeight.BOLD, 12));
                    
                    javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
                    glow.setColor(Color.web("#FFB703"));
                    glow.setRadius(3.0);
                    glow.setSpread(0.2);
                    textLabel.setEffect(glow);
                    
                    rect.setStroke(Color.web("#FFB703"));
                    rect.setStrokeWidth(1.5);
                    cell.getChildren().add(textLabel);
                } else if (targetType == NodeType.DROP_ZONE) {
                    dropZoneCounter++;
                    String label = "DZ" + dropZoneCounter;
                    stationLabels.put(cellId, label);
                    Text textLabel = new Text(label);
                    textLabel.setFill(Color.web("#00FF66")); // Glowing green
                    textLabel.setFont(Font.font("Courier New", FontWeight.BOLD, 12));
                    
                    javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
                    glow.setColor(Color.web("#00FF66"));
                    glow.setRadius(3.0);
                    glow.setSpread(0.2);
                    textLabel.setEffect(glow);
                    
                    rect.setStroke(Color.web("#00FF66"));
                    rect.setStrokeWidth(1.5);
                    cell.getChildren().add(textLabel);
                }
            }

            // If a cell becomes an obstacle, and it was previously selected for routing or has a robot, clear it
            if (gridState[row][col] == NodeType.OBSTACLE) {
                cell.getChildren().removeIf(node -> node.getId() != null && node.getId().startsWith("robot-visual-"));
                
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
        if (type == null) return Color.web("#0B132B");
        switch (type) {
            case OBSTACLE:
                return Color.web("#FF3333"); // Crimson Warning Red
            case CHARGING_STATION:
            case DROP_ZONE:
            case EMPTY:
            default:
                return Color.web("#0B132B");
        }
    }

    public void highlightCalculatedRoute(List<Edge> shortestPath) {
        // Reset previously drawn highlights, leaving obstacles untouched
        resetGridHighlights();

        if (dijkstraTimeline != null) {
            dijkstraTimeline.stop();
        }

        if (shortestPath == null || shortestPath.isEmpty()) {
            return;
        }

        Color pathColor = Color.web("#00E5FF", 0.4); // 60% transparent Electric Cyan
        
        // Extract nodes to visit sequentially
        java.util.List<Node> nodes = new java.util.ArrayList<>();
        nodes.add(shortestPath.get(0).getSource());
        for (Edge edge : shortestPath) {
            nodes.add(edge.getDestination());
        }

        activePathCells.clear();
        for (Node node : nodes) {
            activePathCells.add(node.getId());
        }

        dijkstraTimeline = new javafx.animation.Timeline();
        double stepDelayMs = 30.0;

        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            String nodeId = node.getId();
            javafx.animation.KeyFrame keyFrame = new javafx.animation.KeyFrame(
                javafx.util.Duration.millis(i * stepDelayMs),
                event -> {
                    if (activePathCells.contains(nodeId)) {
                        highlightCell(node.getY(), node.getX(), pathColor, pathColor, 1.5);
                    }
                }
            );
            dijkstraTimeline.getKeyFrames().add(keyFrame);
        }
        dijkstraTimeline.play();
    }

    public void highlightMST(List<Edge> mstEdges) {
        // Reset highlights
        resetGridHighlights();

        if (mstTimeline != null) {
            mstTimeline.stop();
        }

        if (mstEdges == null || mstEdges.isEmpty()) {
            return;
        }

        mstTimeline = new javafx.animation.Timeline();
        double edgeDurationMs = 200.0;

        for (int i = 0; i < mstEdges.size(); i++) {
            Edge edge = mstEdges.get(i);
            Node src = edge.getSource();
            Node dest = edge.getDestination();

            int x1 = src.getX();
            int y1 = src.getY();
            int x2 = dest.getX();
            int y2 = dest.getY();

            int dx = x2 - x1;
            int dy = y2 - y1;

            if (y1 >= 0 && y1 < ROWS && x1 >= 0 && x1 < COLS &&
                y2 >= 0 && y2 < ROWS && x2 >= 0 && x2 < COLS) {
                
                StackPane cell1 = cellGrid[y1][x1];
                StackPane cell2 = cellGrid[y2][x2];

                javafx.scene.shape.Line line1 = new javafx.scene.shape.Line();
                line1.setManaged(false);
                line1.setStartX(CELL_SIZE / 2.0);
                line1.setStartY(CELL_SIZE / 2.0);
                line1.setEndX(CELL_SIZE / 2.0);
                line1.setEndY(CELL_SIZE / 2.0);
                line1.setStroke(Color.web("#39FF14")); // Matrix Laser Green
                line1.setStrokeWidth(2.0);

                javafx.scene.shape.Line line2 = new javafx.scene.shape.Line();
                line2.setManaged(false);
                line2.setStartX(CELL_SIZE / 2.0);
                line2.setStartY(CELL_SIZE / 2.0);
                line2.setEndX(CELL_SIZE / 2.0);
                line2.setEndY(CELL_SIZE / 2.0);
                line2.setStroke(Color.web("#39FF14")); // Matrix Laser Green
                line2.setStrokeWidth(2.0);

                javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
                glow.setColor(Color.web("#39FF14"));
                glow.setRadius(5.0);
                glow.setSpread(0.6);
                line1.setEffect(glow);
                line2.setEffect(glow);

                cell1.getChildren().add(line1);
                cell2.getChildren().add(line2);

                double startTime = i * edgeDurationMs;
                double endTime = (i + 1) * edgeDurationMs;

                mstTimeline.getKeyFrames().add(new javafx.animation.KeyFrame(
                    javafx.util.Duration.millis(startTime),
                    new javafx.animation.KeyValue(line1.endXProperty(), CELL_SIZE / 2.0),
                    new javafx.animation.KeyValue(line1.endYProperty(), CELL_SIZE / 2.0),
                    new javafx.animation.KeyValue(line2.endXProperty(), CELL_SIZE / 2.0),
                    new javafx.animation.KeyValue(line2.endYProperty(), CELL_SIZE / 2.0)
                ));

                mstTimeline.getKeyFrames().add(new javafx.animation.KeyFrame(
                    javafx.util.Duration.millis(endTime),
                    new javafx.animation.KeyValue(line1.endXProperty(), CELL_SIZE / 2.0 + dx * (CELL_SIZE / 2.0)),
                    new javafx.animation.KeyValue(line1.endYProperty(), CELL_SIZE / 2.0 + dy * (CELL_SIZE / 2.0)),
                    new javafx.animation.KeyValue(line2.endXProperty(), CELL_SIZE / 2.0 - dx * (CELL_SIZE / 2.0)),
                    new javafx.animation.KeyValue(line2.endYProperty(), CELL_SIZE / 2.0 - dy * (CELL_SIZE / 2.0))
                ));
            }
        }
        mstTimeline.play();
    }

    private void highlightCell(int r, int c, Color fill, Color border, double borderWidth) {
        if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
            if (gridState[r][c] != NodeType.OBSTACLE) {
                StackPane cell = cellGrid[r][c];
                Rectangle rect = (Rectangle) cell.getChildren().get(0);
                rect.setFill(fill);
                rect.setStroke(border);
                rect.setStrokeWidth(borderWidth);
            }
        }
    }

    private void resetGridHighlights() {
        if (dijkstraTimeline != null) {
            dijkstraTimeline.stop();
        }
        activePathCells.clear();

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (gridState[r][c] != NodeType.OBSTACLE) {
                    StackPane cell = cellGrid[r][c];
                    
                    // Remove any Line nodes (from MST highlighting)
                    cell.getChildren().removeIf(node -> node instanceof javafx.scene.shape.Line);
                    
                    Rectangle rect = (Rectangle) cell.getChildren().get(0);
                    rect.setFill(getColorForNodeType(gridState[r][c]));
                    
                    // Restore borders and styles for stations
                    if (gridState[r][c] == NodeType.CHARGING_STATION) {
                        rect.setStroke(Color.web("#FFB703"));
                        rect.setStrokeWidth(1.5);
                    } else if (gridState[r][c] == NodeType.DROP_ZONE) {
                        rect.setStroke(Color.web("#00FF66"));
                        rect.setStrokeWidth(1.5);
                    } else {
                        rect.setStroke(Color.web("#00FF66", 0.15));
                        rect.setStrokeWidth(1.0);
                    }
                }
            }
        }
    }

    public void clearMap() {
        if (dijkstraTimeline != null) {
            dijkstraTimeline.stop();
        }
        if (mstTimeline != null) {
            mstTimeline.stop();
        }

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                gridState[r][c] = NodeType.EMPTY;
            }
        }

        stationLabels.clear();
        chargingStationCounter = 0;
        dropZoneCounter = 0;

        if (rightSidebar != null) {
            rightSidebar.clearCustomTasks();
        }

        // Remove robot visual assets and reset cell colors/borders
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                StackPane cell = cellGrid[r][c];
                // Keep only the first child (base Rectangle)
                while (cell.getChildren().size() > 1) {
                    cell.getChildren().remove(1);
                }
                Rectangle rect = (Rectangle) cell.getChildren().get(0);
                rect.setFill(Color.web("#0B132B"));
                rect.setStroke(Color.web("#00FF66", 0.15));
                rect.setStrokeWidth(1.0);
            }
        }

        statusBar.setStatus("Map cleared.");
    }

    public NodeType[][] getGridState() {
        return gridState;
    }

    public PlacementMode getCurrentPlacementMode() {
        return currentPlacementMode;
    }

    public void setCurrentPlacementMode(PlacementMode mode) {
        this.currentPlacementMode = mode;
    }

    public void startWorkflowSimulation(Robot robot, java.util.Queue<com.warehouse.model.domain.Task> taskQueue) {
        executeNextTask(robot, taskQueue);
    }

    private void executeNextTask(Robot robot, java.util.Queue<com.warehouse.model.domain.Task> taskQueue) {
        if (taskQueue.isEmpty()) {
            statusBar.setStatus("All scheduled tasks completed successfully!");
            return;
        }

        com.warehouse.model.domain.Task nextTask = taskQueue.poll();
        String targetId = nextTask.getTargetNodeId();
        
        // Rebuild full active graph using occupied nodes of other robots
        List<String> occupied = new java.util.ArrayList<>();
        for (Robot r : robotManagementService.getActiveFleet()) {
            if (!r.getId().equals(robot.getId()) && r.getCurrentNode() != null) {
                occupied.add(r.getCurrentNode().getId());
            }
        }
        routingService.generateGraphFromGrid(gridState, occupied);
        Graph graph = routingService.getWarehouseMap();
        
        Node destNode = null;
        for (Node n : graph.getNodes()) {
            if (n.getId().equals(targetId)) {
                destNode = n;
                break;
            }
        }
        
        if (destNode == null) {
            statusBar.setStatus("Skipping Task " + nextTask.getId() + ": Target location is blocked or invalid.");
            executeNextTask(robot, taskQueue);
            return;
        }

        Node startNode = robot.getCurrentNode();
        if (startNode == null) {
            statusBar.setStatus("Error: Robot has no current location.");
            return;
        }

        if (startNode.equals(destNode)) {
            statusBar.setStatus("Robot " + robot.getId() + " is already at " + destNode.getId() + " for " + nextTask.getName());
            nextTask.setActive(false);
            nextTask.setProgress(1.0);
            rightSidebar.refreshCreatedTasksListView();

            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.0));
            pause.setOnFinished(e -> executeNextTask(robot, taskQueue));
            pause.play();
            return;
        }

        List<Edge> path = routingService.calculateRoute(startNode, destNode);
        if (path == null || path.isEmpty()) {
            statusBar.setStatus("Error: No navigable path found for task " + nextTask.getId());
            executeNextTask(robot, taskQueue);
            return;
        }

        statusBar.setStatus("Executing Task: " + nextTask.getName() + " -> Routing to " + destNode.getId());
        
        // Set task active and draw path discovery
        nextTask.setActive(true);
        nextTask.setProgress(0.0);
        robot.setActiveTaskId(nextTask.getId());
        rightSidebar.refreshCreatedTasksListView();

        highlightCalculatedRoute(path);

        final Node finalDest = destNode;
        final com.warehouse.model.domain.Task finalTask = nextTask;

        // Animate movement step-by-step
        executeNextPathStep(robot, path, 0, finalTask, taskQueue, false, () -> {
            robot.setCurrentNode(finalDest); // update position upon arrival
            robot.setActiveTaskId("None");
            
            StackPane visual = (StackPane) this.lookup("#robot-visual-" + robot.getId());
            updateRobotTelemetryHUD(robot, visual);

            statusBar.setStatus("Task Completed: " + finalTask.getName() + " at " + finalDest.getId());
            
            finalTask.setActive(false);
            finalTask.setProgress(1.0);
            rightSidebar.refreshCreatedTasksListView();

            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.0));
            pause.setOnFinished(e -> executeNextTask(robot, taskQueue));
            pause.play();
        });
    }

    private void executeNextPathStep(Robot robot,
                                     List<Edge> path,
                                     int pathIndex,
                                     com.warehouse.model.domain.Task activeTask,
                                     java.util.Queue<com.warehouse.model.domain.Task> taskQueue,
                                     boolean isEmergencyCharging,
                                     Runnable onArrival) {
        if (pathIndex >= path.size()) {
            onArrival.run();
            return;
        }

        // Before stepping onto a new tile, check if batteryLevel < 20.0 (Smart Check)
        if (!isEmergencyCharging && robot.getBatteryLevel() < 20.0) {
            statusBar.setStatus("Warning: Robot " + robot.getId() + " battery low (" + String.format("%.1f", robot.getBatteryLevel()) + "%). Initiating Emergency Charging Override!");
            
            // Interrupt the active task and queue
            if (activeTask != null) {
                activeTask.setActive(false);
                if (taskQueue instanceof java.util.LinkedList) {
                    ((java.util.LinkedList<com.warehouse.model.domain.Task>) taskQueue).addFirst(activeTask);
                }
            }
            
            // Set active task ID to Charging
            robot.setActiveTaskId("CHARGING");
            StackPane robotVisual = (StackPane) this.lookup("#robot-visual-" + robot.getId());
            updateRobotTelemetryHUD(robot, robotVisual);
            rightSidebar.refreshCreatedTasksListView();
            
            // Find nearest charging station
            routingService.generateGraphFromGrid(gridState);
            Graph currentGraph = routingService.getWarehouseMap();
            List<Node> stations = new java.util.ArrayList<>();
            for (Node n : currentGraph.getNodes()) {
                if (n.getNodeType() == NodeType.CHARGING_STATION) {
                    stations.add(n);
                }
            }
            
            if (stations.isEmpty()) {
                statusBar.setStatus("Error: No charging station placed on grid! Continuing active task...");
                // Re-enable task if possible and continue
                if (activeTask != null) {
                    activeTask.setActive(true);
                    robot.setActiveTaskId(activeTask.getId());
                }
            } else {
                // Find shortest route to a station
                List<Edge> bestRoute = null;
                Node targetStation = null;
                for (Node station : stations) {
                    List<Edge> route = routingService.calculateRoute(robot.getCurrentNode(), station);
                    if (route != null && !route.isEmpty()) {
                        if (bestRoute == null || route.size() < bestRoute.size()) {
                            bestRoute = route;
                            targetStation = station;
                        }
                    }
                }
                
                if (bestRoute == null) {
                    statusBar.setStatus("Error: Charging stations are unreachable! Continuing active task...");
                    if (activeTask != null) {
                        activeTask.setActive(true);
                        robot.setActiveTaskId(activeTask.getId());
                    }
                } else {
                    final Node finalStation = targetStation;
                    final List<Edge> finalEmergencyRoute = bestRoute;
                    
                    // Route to charging station
                    executeNextPathStep(robot, finalEmergencyRoute, 0, null, taskQueue, true, () -> {
                        statusBar.setStatus("Robot " + robot.getId() + " arrived at Charging Station. Recharging...");
                        
                        // Recharge transition (1.5 seconds)
                        javafx.animation.PauseTransition rechargeTimer = new javafx.animation.PauseTransition(
                            javafx.util.Duration.seconds(1.5)
                        );
                        rechargeTimer.setOnFinished(evt -> {
                            robot.setBatteryLevel(100.0);
                            robot.setActiveTaskId("None");
                            statusBar.setStatus("Robot " + robot.getId() + " fully recharged (100.0%). Resuming task queue.");
                            
                            StackPane visual = (StackPane) this.lookup("#robot-visual-" + robot.getId());
                            updateRobotTelemetryHUD(robot, visual);
                            
                            // Resume queue
                            executeNextTask(robot, taskQueue);
                        });
                        rechargeTimer.play();
                    });
                    return;
                }
            }
        }

        // Normal path step execution
        Edge edge = path.get(pathIndex);
        Node srcNode = edge.getSource();
        Node destNode = edge.getDestination();

        // Deduct battery
        robot.setBatteryLevel(robot.getBatteryLevel() - 1.5);

        StackPane robotVisual = (StackPane) this.lookup("#robot-visual-" + robot.getId());
        if (robotVisual == null) {
            robotVisual = createRobotVisual(robot);
            cellGrid[srcNode.getY()][srcNode.getX()].getChildren().add(robotVisual);
        }

        // Update telemetry HUD
        updateRobotTelemetryHUD(robot, robotVisual);

        // Move visual using TranslateTransition
        int dx = destNode.getX() - srcNode.getX();
        int dy = destNode.getY() - srcNode.getY();
        double offset = CELL_SIZE + 1.0;
        double tx = dx * offset;
        double ty = dy * offset;

        final StackPane finalVisual = robotVisual;
        javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(
            javafx.util.Duration.millis(200), finalVisual
        );
        tt.setFromX(0.0);
        tt.setFromY(0.0);
        tt.setToX(tx);
        tt.setToY(ty);

        tt.setOnFinished(evt -> {
            // Reset translation first to prevent coordinate multiplication errors or visual jumps
            finalVisual.setTranslateX(0.0);
            finalVisual.setTranslateY(0.0);

            // Reparent visual node
            cellGrid[srcNode.getY()][srcNode.getX()].getChildren().remove(finalVisual);
            
            // Clean up destination cell of any old robot-visual of this robot
            cellGrid[destNode.getY()][destNode.getX()].getChildren().removeIf(
                n -> n.getId() != null && n.getId().equals("robot-visual-" + robot.getId())
            );
            cellGrid[destNode.getY()][destNode.getX()].getChildren().add(finalVisual);

            // Update model
            robot.setCurrentNode(destNode);

            // Update task progress if active
            if (activeTask != null) {
                double progress = (double) (pathIndex + 1) / path.size();
                activeTask.setProgress(progress);
                rightSidebar.refreshCreatedTasksListView();
            }

            // Next step
            executeNextPathStep(robot, path, pathIndex + 1, activeTask, taskQueue, isEmergencyCharging, onArrival);
        });

        tt.play();
    }
}
