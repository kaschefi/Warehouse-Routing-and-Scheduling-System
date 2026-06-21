package com.warehouse.ui;

import com.warehouse.model.domain.Node;
import com.warehouse.model.domain.Robot;
import com.warehouse.model.domain.Task;
import com.warehouse.model.graph.Edge;
import com.warehouse.model.graph.Graph;
import com.warehouse.service.NetworkDesignService;
import com.warehouse.service.RoutingService;
import com.warehouse.service.TaskSchedulingService;
import com.warehouse.service.RobotManagementService;
import java.util.List;
import java.util.Map;
import java.util.LinkedList;
import java.util.Queue;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Priority;

/**
 * The right sidebar.
 * Contains: Algorithmic HUD, Pathfinding, Network Planning, and Task Builder.
 *
 * Task Builder supports assigning each task to a specific robot,
 * enabling concurrent independent multi-robot simulation.
 */
public class RightSidebarView extends VBox {

    private final RoutingService routingService;
    private final NetworkDesignService networkDesignService;
    private final TaskSchedulingService taskSchedulingService;
    private final RobotManagementService robotManagementService;
    private final StatusBarView statusBar;
    private WarehouseGridView gridView;

    private TextField startNodeField;
    private TextField endNodeField;

    private final List<Task> customTaskList = new ArrayList<>();
    private TextField taskNameField;
    private ComboBox<String> locationComboBox;
    private ComboBox<String> robotComboBox;      // NEW: robot assignment
    private ComboBox<String> dependencyComboBox;
    private ListView<Task> createdTasksListView;
    private VBox taskBuilderCard;
    private javafx.scene.control.Slider speedSlider;

    // Algorithmic HUD labels
    private Label lblDijkstraCost;
    private Label lblMstFootprint;
    private Label lblKahnStatus;

    public RightSidebarView(RoutingService routingService,
                            NetworkDesignService networkDesignService,
                            TaskSchedulingService taskSchedulingService,
                            StatusBarView statusBar,
                            RobotManagementService robotManagementService) {
        this.routingService = routingService;
        this.networkDesignService = networkDesignService;
        this.taskSchedulingService = taskSchedulingService;
        this.statusBar = statusBar;
        this.robotManagementService = robotManagementService;

        this.getStyleClass().add("right-sidebar");
        this.setSpacing(0);
        this.setPrefWidth(280);

        // Wrap content in a ScrollPane so it's usable on small screens
        VBox content = new VBox(12);
        content.setStyle("-fx-padding: 12px;");

        buildAlgorithmicsHUD(content);
        buildSimulationSpeedSection(content);
        buildPathfindingSection(content);
        buildNetworkPlanningSection(content);
        buildTaskSchedulingSection(content);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("sidebar-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        this.getChildren().add(scroll);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Algorithmic Performance HUD
    // ─────────────────────────────────────────────────────────────────────────

    private void buildAlgorithmicsHUD(VBox parent) {
        VBox card = new VBox(8);
        card.getStyleClass().add("tactical-card");

        Label title = new Label("ALGORITHMIC PERFORMANCE HUD");
        title.getStyleClass().add("card-title");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(6);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(115);
        col1.setPrefWidth(115);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        Label keyDijkstra = new Label("Dijkstra Cost:");
        keyDijkstra.getStyleClass().add("hud-key-label");
        lblDijkstraCost = new Label("—");
        lblDijkstraCost.getStyleClass().add("hud-value-label");
        grid.add(keyDijkstra, 0, 0);
        grid.add(lblDijkstraCost, 1, 0);

        Label keyMst = new Label("MST Footprint:");
        keyMst.getStyleClass().add("hud-key-label");
        lblMstFootprint = new Label("—");
        lblMstFootprint.getStyleClass().add("hud-value-label");
        grid.add(keyMst, 0, 1);
        grid.add(lblMstFootprint, 1, 1);

        Label keyKahn = new Label("Kahn Status:");
        keyKahn.getStyleClass().add("hud-key-label");
        lblKahnStatus = new Label("IDLE");
        lblKahnStatus.getStyleClass().addAll("hud-value-label", "hud-status-idle");
        grid.add(keyKahn, 0, 2);
        grid.add(lblKahnStatus, 1, 2);

        card.getChildren().addAll(title, grid);
        parent.getChildren().add(card);
    }

    private void buildSimulationSpeedSection(VBox parent) {
        VBox card = new VBox(8);
        card.getStyleClass().add("tactical-card");

        Label title = new Label("SIMULATION SPEED");
        title.getStyleClass().add("card-title");

        speedSlider = new javafx.scene.control.Slider(50.0, 600.0, 200.0);
        speedSlider.setShowTickMarks(true);
        speedSlider.setShowTickLabels(true);
        speedSlider.setMajorTickUnit(100.0);
        speedSlider.setMinorTickCount(4);
        speedSlider.setBlockIncrement(50.0);

        Label lblValue = new Label("Speed: 200.0 ms/tile");
        lblValue.getStyleClass().add("hud-value-label");

        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            lblValue.setText(String.format("Speed: %.1f ms/tile", newVal.doubleValue()));
        });

        card.getChildren().addAll(title, speedSlider, lblValue);
        parent.getChildren().add(card);
    }

    public double getSimulationSpeed() {
        if (speedSlider != null) {
            return speedSlider.getValue();
        }
        return 200.0;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public metric setters
    // ─────────────────────────────────────────────────────────────────────────

    public void updateDijkstraCost(double cost) {
        lblDijkstraCost.setText(String.format("%.2f units", cost));
    }

    public void updateMstFootprint(double weight) {
        lblMstFootprint.setText(String.format("%.2f units", weight));
    }

    public void updateKahnStatus(String status) {
        lblKahnStatus.getStyleClass().removeAll("hud-status-idle", "hud-status-simulating", "hud-status-deadlock");
        switch (status.toUpperCase()) {
            case "SIMULATING":
                lblKahnStatus.getStyleClass().add("hud-status-simulating");
                break;
            case "DEADLOCK ABORT":
                lblKahnStatus.getStyleClass().add("hud-status-deadlock");
                break;
            default:
                lblKahnStatus.getStyleClass().add("hud-status-idle");
                break;
        }
        lblKahnStatus.setText(status.toUpperCase());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pathfinding Section
    // ─────────────────────────────────────────────────────────────────────────

    private void buildPathfindingSection(VBox parent) {
        VBox card = new VBox(10);
        card.getStyleClass().add("tactical-card");

        Label title = new Label("PATHFINDING");
        title.getStyleClass().add("card-title");

        startNodeField = new TextField();
        startNodeField.setPromptText("Start Node");
        startNodeField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.trim().isEmpty() && gridView != null) {
                gridView.highlightCalculatedRoute(null);
                updateDijkstraCost(0.0);
            }
        });

        endNodeField = new TextField();
        endNodeField.setPromptText("End Node");
        endNodeField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.trim().isEmpty() && gridView != null) {
                gridView.highlightCalculatedRoute(null);
                updateDijkstraCost(0.0);
            }
        });

        Button btnFindPath = new Button("Find Shortest Path");
        btnFindPath.setMaxWidth(Double.MAX_VALUE);
        btnFindPath.setOnAction(e -> {
            String startId = getStartNodeText();
            String endId = getEndNodeText();

            if (startId.isEmpty() || endId.isEmpty()) {
                statusBar.setStatus("Error: Start Node and End Node must both be selected!");
                return;
            }
            if (gridView == null) {
                statusBar.setStatus("Error: GridView is not initialized!");
                return;
            }

            try {
                List<String> occupied = new ArrayList<>();
                for (Robot r : robotManagementService.getActiveFleet()) {
                    if (r.getCurrentNode() != null) occupied.add(r.getCurrentNode().getId());
                }
                routingService.generateGraphFromGrid(gridView.getGridState(), occupied);
                Graph graph = routingService.getWarehouseMap();

                Node startNode = null, endNode = null;
                for (Node node : graph.getNodes()) {
                    if (node.getId().equals(startId)) startNode = node;
                    if (node.getId().equals(endId)) endNode = node;
                }

                if (startNode == null) { statusBar.setStatus("Error: Start Node (" + startId + ") is an obstacle or invalid!"); return; }
                if (endNode == null)   { statusBar.setStatus("Error: End Node (" + endId + ") is an obstacle or invalid!"); return; }

                List<Edge> path = routingService.calculateRoute(startNode, endNode);
                if (path == null || path.isEmpty()) {
                    statusBar.setStatus("No path found from " + startId + " to " + endId);
                    gridView.highlightCalculatedRoute(null);
                    updateDijkstraCost(0.0);
                } else {
                    double totalCost = path.stream().mapToDouble(Edge::getWeight).sum();
                    updateDijkstraCost(totalCost);
                    statusBar.setStatus("Shortest path: " + path.size() + " steps | cost " + String.format("%.2f", totalCost));
                    gridView.highlightCalculatedRoute(path);
                }
            } catch (Exception ex) {
                statusBar.setStatus("Error calculating path: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        card.getChildren().addAll(title, startNodeField, endNodeField, btnFindPath);
        parent.getChildren().add(card);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Network Planning Section
    // ─────────────────────────────────────────────────────────────────────────

    private void buildNetworkPlanningSection(VBox parent) {
        VBox card = new VBox(10);
        card.getStyleClass().add("tactical-card");

        Label title = new Label("NETWORK PLANNING");
        title.getStyleClass().add("card-title");

        Button btnMST = new Button("Generate MST");
        btnMST.setMaxWidth(Double.MAX_VALUE);
        btnMST.setOnAction(e -> {
            if (gridView == null) { statusBar.setStatus("Error: GridView is not initialized!"); return; }

            try {
                routingService.generateGraphFromGrid(gridView.getGridState());
                Graph fullGraph = routingService.getWarehouseMap();

                List<Node> specialNodes = new ArrayList<>();
                for (Node node : fullGraph.getNodes()) {
                    if (node.getNodeType() == com.warehouse.model.domain.NodeType.CHARGING_STATION ||
                        node.getNodeType() == com.warehouse.model.domain.NodeType.DROP_ZONE) {
                        specialNodes.add(node);
                    }
                }

                if (specialNodes.size() < 2) {
                    statusBar.setStatus("Please place at least two stations to design a network.");
                    gridView.highlightMST(null);
                    return;
                }

                Graph specialGraph = new com.warehouse.model.graph.AdjacencyListGraph();
                for (Node node : specialNodes) specialGraph.addNode(node);

                Map<String, List<Edge>> virtualEdgePaths = new java.util.HashMap<>();
                for (int i = 0; i < specialNodes.size(); i++) {
                    Node u = specialNodes.get(i);
                    for (int j = i + 1; j < specialNodes.size(); j++) {
                        Node v = specialNodes.get(j);
                        List<Edge> path = routingService.calculateRoute(u, v);
                        if (path != null && !path.isEmpty()) {
                            double w = path.stream().mapToDouble(Edge::getWeight).sum();
                            specialGraph.addEdge(new Edge(u, v, w));
                            specialGraph.addEdge(new Edge(v, u, w));
                            virtualEdgePaths.put(u.getId() + "-" + v.getId(), path);
                            virtualEdgePaths.put(v.getId() + "-" + u.getId(), path);
                        }
                    }
                }

                networkDesignService.setWarehouseMap(specialGraph);
                List<Edge> mstEdges = networkDesignService.generateOptimalNetwork();

                if (mstEdges == null || mstEdges.isEmpty()) {
                    statusBar.setStatus("No MST generated (stations may be disconnected).");
                    gridView.highlightMST(null);
                } else {
                    List<Edge> realMstEdges = new ArrayList<>();
                    for (Edge mstEdge : mstEdges) {
                        List<Edge> path = virtualEdgePaths.get(mstEdge.getSource().getId() + "-" + mstEdge.getDestination().getId());
                        if (path != null) realMstEdges.addAll(path);
                    }
                    double totalCost = mstEdges.stream().mapToDouble(Edge::getWeight).sum();
                    updateMstFootprint(totalCost);
                    statusBar.setStatus(String.format("MST: %d stations linked, cost %.2f units", specialNodes.size(), totalCost));
                    gridView.highlightMST(realMstEdges);
                }
            } catch (Exception ex) {
                statusBar.setStatus("Error generating MST: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        card.getChildren().addAll(title, btnMST);
        parent.getChildren().add(card);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Task Scheduling Section  (multi-robot aware)
    // ─────────────────────────────────────────────────────────────────────────

    private void buildTaskSchedulingSection(VBox parent) {
        taskBuilderCard = new VBox(10);
        taskBuilderCard.getStyleClass().add("tactical-card");

        Label title = new Label("TASK BUILDER");
        title.getStyleClass().add("card-title");

        // Task name
        taskNameField = new TextField();
        taskNameField.setPromptText("Task Name (e.g. Pick Shelf A)");

        // Target location
        locationComboBox = new ComboBox<>();
        locationComboBox.setPromptText("Target Station / Zone");
        locationComboBox.setMaxWidth(Double.MAX_VALUE);
        locationComboBox.setOnShowing(ev -> {
            locationComboBox.getItems().clear();
            if (gridView != null) locationComboBox.getItems().addAll(gridView.getStationLabels().values());
        });

        // ── NEW: Robot assignment combo ────────────────────────────────────
        robotComboBox = new ComboBox<>();
        robotComboBox.setPromptText("Assign to Robot");
        robotComboBox.setMaxWidth(Double.MAX_VALUE);
        robotComboBox.setOnShowing(ev -> {
            robotComboBox.getItems().clear();
            for (Robot r : robotManagementService.getActiveFleet()) {
                robotComboBox.getItems().add(r.getId() + " — " + r.getName());
            }
        });

        // Dependency
        dependencyComboBox = new ComboBox<>();
        dependencyComboBox.setPromptText("Depends on Task (optional)");
        dependencyComboBox.setMaxWidth(Double.MAX_VALUE);
        dependencyComboBox.setOnShowing(ev -> {
            dependencyComboBox.getItems().clear();
            dependencyComboBox.getItems().add("None");
            for (Task t : customTaskList) {
                dependencyComboBox.getItems().add(t.getId() + " — " + t.getName());
            }
        });

        // Small helper labels
        Label lblLocation   = makeFieldLabel("Station / Zone");
        Label lblRobot      = makeFieldLabel("Assigned Robot");
        Label lblDependency = makeFieldLabel("Dependency");

        Button btnAddTask = new Button("+ Add Task");
        btnAddTask.setMaxWidth(Double.MAX_VALUE);
        btnAddTask.getStyleClass().add("btn-add-task");
        btnAddTask.setOnAction(e -> {
            // Validate name
            String name = taskNameField.getText().trim();
            if (name.isEmpty()) { statusBar.setStatus("Warning: Task Name cannot be empty!"); return; }

            // Validate location
            String loc = locationComboBox.getValue();
            if (loc == null || loc.isEmpty()) { statusBar.setStatus("Warning: Please select a target location!"); return; }

            // Validate robot
            String robotEntry = robotComboBox.getValue();
            if (robotEntry == null || robotEntry.isEmpty()) { statusBar.setStatus("Warning: Please assign this task to a robot!"); return; }
            String robotId = robotEntry.split(" — ")[0].trim();

            // Resolve cell id from label
            String cellId = null;
            if (gridView != null) {
                for (Map.Entry<String, String> entry : gridView.getStationLabels().entrySet()) {
                    if (entry.getValue().equals(loc)) { cellId = entry.getKey(); break; }
                }
            }
            if (cellId == null) { statusBar.setStatus("Error: Selected location is invalid."); return; }

            // Build the task
            Task task = new Task("T" + (customTaskList.size() + 1), name);
            task.setTargetNodeId(cellId);
            task.setAssignedRobotId(robotId);

            // Resolve dependency
            String depValue = dependencyComboBox.getValue();
            if (depValue != null && !depValue.equals("None")) {
                String depId = depValue.split(" — ")[0].trim();
                for (Task t : customTaskList) {
                    if (t.getId().equals(depId)) { task.addDependency(t); break; }
                }
            }

            customTaskList.add(task);
            refreshCreatedTasksListView();

            taskNameField.clear();
            locationComboBox.setValue(null);
            robotComboBox.setValue(null);
            dependencyComboBox.setValue(null);
            statusBar.setStatus("Task " + task.getId() + " (" + name + ") assigned to " + robotId + " @ " + loc);
        });

        Label listLabel = new Label("CREATED TASKS");
        listLabel.getStyleClass().add("hud-key-label");

        createdTasksListView = new ListView<>();
        createdTasksListView.setPrefHeight(150);
        createdTasksListView.setCellFactory(lv -> new javafx.scene.control.ListCell<Task>() {
            private final HBox layout   = new HBox(8);
            private final Label lbText  = new Label();
            private final javafx.scene.control.ProgressBar pb = new javafx.scene.control.ProgressBar(0);
            private final Label lbRobot = new Label();
            private final Button btnDelete = new Button("🗑");
            {
                layout.setAlignment(Pos.CENTER_LEFT);
                pb.getStyleClass().add("task-progress-bar");
                lbRobot.getStyleClass().add("task-robot-badge");
                
                // Style delete button
                btnDelete.setStyle("-fx-text-fill: #FF3333; -fx-background-color: transparent; -fx-padding: 2px 6px; -fx-cursor: hand; -fx-font-size: 12px;");
                btnDelete.hoverProperty().addListener((obs, wasHovered, isNowHovered) -> {
                    if (isNowHovered) {
                        btnDelete.setStyle("-fx-text-fill: #FF6666; -fx-background-color: rgba(255, 51, 51, 0.15); -fx-padding: 2px 6px; -fx-cursor: hand; -fx-font-size: 12px;");
                    } else {
                        btnDelete.setStyle("-fx-text-fill: #FF3333; -fx-background-color: transparent; -fx-padding: 2px 6px; -fx-cursor: hand; -fx-font-size: 12px;");
                    }
                });
                
                btnDelete.setOnAction(e -> {
                    Task item = getItem();
                    if (item != null) {
                        customTaskList.remove(item);
                        // Remove from dependencies
                        for (Task t : customTaskList) {
                            t.getDependencies().remove(item);
                        }
                        refreshCreatedTasksListView();
                        statusBar.setStatus("Deleted task: " + item.getId());
                    }
                });

                layout.getChildren().addAll(lbRobot, lbText, pb, btnDelete);
                HBox.setHgrow(lbText, Priority.ALWAYS);
            }
            @Override
            protected void updateItem(Task task, boolean empty) {
                super.updateItem(task, empty);
                if (empty || task == null) { setGraphic(null); return; }

                // Robot badge
                String rid = task.getAssignedRobotId();
                lbRobot.setText(rid != null ? rid : "?");
                if (rid != null) {
                    String baseHex = getRobotColorHex(rid);
                    lbRobot.setStyle(
                        "-fx-text-fill: " + baseHex + "; " +
                        "-fx-border-color: " + baseHex + "59; " + // 35% opacity
                        "-fx-background-color: " + baseHex + "1E;"  // 12% opacity
                    );
                } else {
                    lbRobot.setStyle("");
                }

                // Dep string
                StringBuilder depStr = new StringBuilder();
                if (!task.getDependencies().isEmpty()) {
                    for (int i = 0; i < task.getDependencies().size(); i++) {
                        Task dep = task.getDependencies().get(i);
                        depStr.append(dep.getId());
                        if (i < task.getDependencies().size() - 1) depStr.append(",");
                    }
                } else {
                    depStr.append("—");
                }

                String loc = gridView != null ? gridView.getStationLabels().get(task.getTargetNodeId()) : task.getTargetNodeId();
                if (loc == null) loc = task.getTargetNodeId();
                lbText.setText(String.format("%s · %s · dep:%s", task.getId(), loc, depStr));

                pb.setVisible(task.isActive());
                if (task.isActive()) pb.setProgress(task.getProgress());
                setGraphic(layout);
            }
        });

        Button btnSchedule = new Button("▶  Launch Simulation");
        btnSchedule.setMaxWidth(Double.MAX_VALUE);
        btnSchedule.getStyleClass().add("btn-launch");
        btnSchedule.setOnAction(e -> handleScheduleAndLaunch());

        HBox bottomRow = new HBox(8, btnSchedule);
        HBox.setHgrow(btnSchedule, Priority.ALWAYS);

        taskBuilderCard.getChildren().addAll(
            title,
            taskNameField,
            lblLocation, locationComboBox,
            lblRobot,    robotComboBox,
            lblDependency, dependencyComboBox,
            btnAddTask,
            listLabel,
            createdTasksListView,
            bottomRow
        );
        parent.getChildren().add(taskBuilderCard);
    }

    /**
     * Resolves the task list topologically, groups tasks by assigned robot,
     * then launches a concurrent independent simulation queue per robot.
     */
    private void handleScheduleAndLaunch() {
        try {
            List<Robot> fleet = robotManagementService.getActiveFleet();
            if (fleet.isEmpty()) {
                statusBar.setStatus("Error: No robots on the grid. Place robots first.");
                return;
            }
            if (customTaskList.isEmpty()) {
                statusBar.setStatus("Error: No tasks to schedule. Add tasks first.");
                return;
            }

            // Validate all tasks have an assigned robot that exists in the fleet
            Map<String, Robot> robotById = new LinkedHashMap<>();
            for (Robot r : fleet) robotById.put(r.getId(), r);

            for (Task t : customTaskList) {
                if (t.getAssignedRobotId() == null || !robotById.containsKey(t.getAssignedRobotId())) {
                    statusBar.setStatus("Error: Task " + t.getId() + " has no valid robot assignment.");
                    return;
                }
            }

            // Topological sort across all tasks (respects cross-robot dependencies)
            taskSchedulingService.clearTasks();
            for (Task t : customTaskList) taskSchedulingService.addTask(t);

            updateKahnStatus("SIMULATING");

            List<Task> executionOrder;
            try {
                executionOrder = taskSchedulingService.getExecutionOrder();
            } catch (IllegalStateException isEx) {
                updateKahnStatus("DEADLOCK ABORT");
                statusBar.setStatus("Circular dependency detected! Scheduling aborted.");

                taskBuilderCard.setStyle("-fx-background-color: #FF3333;");
                javafx.animation.FadeTransition fadeCard = new javafx.animation.FadeTransition(javafx.util.Duration.millis(200), taskBuilderCard);
                fadeCard.setFromValue(1.0); fadeCard.setToValue(0.4);
                fadeCard.setCycleCount(4); fadeCard.setAutoReverse(true);
                fadeCard.setOnFinished(evt -> { taskBuilderCard.setStyle(""); taskBuilderCard.setOpacity(1.0); });
                fadeCard.play();

                taskBuilderCard.getChildren().removeIf(n -> n.getStyleClass().contains("deadlock-warning-label"));
                Label warn = new Label("[!] CRITICAL DEADLOCK: SCHEDULING ABORTED [!]");
                warn.getStyleClass().add("deadlock-warning-label");
                warn.setMaxWidth(Double.MAX_VALUE);
                taskBuilderCard.getChildren().add(1, warn);

                javafx.animation.FadeTransition fadeWarn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(250), warn);
                fadeWarn.setFromValue(1.0); fadeWarn.setToValue(0.1);
                fadeWarn.setCycleCount(8); fadeWarn.setAutoReverse(true);
                fadeWarn.setOnFinished(evt -> taskBuilderCard.getChildren().remove(warn));
                fadeWarn.play();
                return;
            }

            // Partition ordered tasks into per-robot queues (preserving topological order within each robot)
            Map<String, Queue<Task>> robotQueues = new LinkedHashMap<>();
            for (Robot r : fleet) robotQueues.put(r.getId(), new LinkedList<>());
            for (Task t : executionOrder) {
                robotQueues.get(t.getAssignedRobotId()).add(t);
            }

            // Build display string
            StringBuilder summary = new StringBuilder("Launching: ");
            for (Map.Entry<String, Queue<Task>> entry : robotQueues.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    summary.append(entry.getKey()).append("(").append(entry.getValue().size()).append(" tasks) ");
                }
            }
            statusBar.setStatus(summary.toString().trim());

            // Fire each robot's queue independently — they run concurrently via async animations
            gridView.startMultiRobotSimulation(robotById, robotQueues, this);

        } catch (Exception ex) {
            updateKahnStatus("IDLE");
            statusBar.setStatus("Scheduling Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    private Label makeFieldLabel(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("hud-key-label");
        lbl.setPadding(new Insets(4, 0, 0, 0));
        return lbl;
    }

    public void clearCustomTasks() {
        customTaskList.clear();
        if (createdTasksListView != null) createdTasksListView.getItems().clear();
        if (locationComboBox != null)   { locationComboBox.setValue(null); locationComboBox.getItems().clear(); }
        if (robotComboBox != null)      { robotComboBox.setValue(null);    robotComboBox.getItems().clear(); }
        if (dependencyComboBox != null) { dependencyComboBox.setValue(null); dependencyComboBox.getItems().clear(); }
        if (taskNameField != null) taskNameField.clear();
        updateKahnStatus("IDLE");
    }

    public void refreshCreatedTasksListView() {
        if (createdTasksListView == null) return;
        int sel = createdTasksListView.getSelectionModel().getSelectedIndex();
        createdTasksListView.getItems().setAll(customTaskList);
        if (sel >= 0 && sel < customTaskList.size())
            createdTasksListView.getSelectionModel().select(sel);
    }

    public void loadDemoScenarioTasks(java.util.Map<String, String> cellIdToLabel) {
        customTaskList.clear();

        String dz1 = findCellIdByLabel(cellIdToLabel, "DZ1");
        String dz2 = findCellIdByLabel(cellIdToLabel, "DZ2");
        String dz3 = findCellIdByLabel(cellIdToLabel, "DZ3");
        String dz4 = findCellIdByLabel(cellIdToLabel, "DZ4");
        String dz5 = findCellIdByLabel(cellIdToLabel, "DZ5");
        String dz6 = findCellIdByLabel(cellIdToLabel, "DZ6");
        String dz7 = findCellIdByLabel(cellIdToLabel, "DZ7");
        String dz8 = findCellIdByLabel(cellIdToLabel, "DZ8");
        String dz9 = findCellIdByLabel(cellIdToLabel, "DZ9");
        String dz10 = findCellIdByLabel(cellIdToLabel, "DZ10");
        String dz11 = findCellIdByLabel(cellIdToLabel, "DZ11");
        String dz12 = findCellIdByLabel(cellIdToLabel, "DZ12");

        Task t1 = new Task("T1", "Aisle Pick Alpha");
        t1.setTargetNodeId(dz1);
        t1.setAssignedRobotId("R1");

        Task t2 = new Task("T2", "Aisle Pick Beta");
        t2.setTargetNodeId(dz2);
        t2.setAssignedRobotId("R1");

        Task t3 = new Task("T3", "Aisle Pick Gamma");
        t3.setTargetNodeId(dz3);
        t3.setAssignedRobotId("R1");
        t3.addDependency(t2);

        Task t4 = new Task("T4", "Shelf Retrieval X");
        t4.setTargetNodeId(dz4);
        t4.setAssignedRobotId("R1");
        t4.addDependency(t3);

        Task t5 = new Task("T5", "Consolidation Y");
        t5.setTargetNodeId(dz5);
        t5.setAssignedRobotId("R2");

        Task t6 = new Task("T6", "Aisle Sort Delta");
        t6.setTargetNodeId(dz6);
        t6.setAssignedRobotId("R2");

        Task t7 = new Task("T7", "Express Delivery E");
        t7.setTargetNodeId(dz7);
        t7.setAssignedRobotId("R2");
        t7.addDependency(t4);
        t7.addDependency(t6);

        Task t8 = new Task("T8", "Express Delivery F");
        t8.setTargetNodeId(dz8);
        t8.setAssignedRobotId("R2");

        Task t9 = new Task("T9", "Express Delivery G");
        t9.setTargetNodeId(dz9);
        t9.setAssignedRobotId("R3");

        Task t10 = new Task("T10", "Final Sorting H");
        t10.setTargetNodeId(dz10);
        t10.setAssignedRobotId("R3");
        t10.addDependency(t9);

        Task t11 = new Task("T11", "Buffer Putaway I");
        t11.setTargetNodeId(dz11);
        t11.setAssignedRobotId("R3");
        t11.addDependency(t10);

        Task t12 = new Task("T12", "Staging Load J");
        t12.setTargetNodeId(dz12);
        t12.setAssignedRobotId("R3");

        customTaskList.add(t1);
        customTaskList.add(t2);
        customTaskList.add(t3);
        customTaskList.add(t4);
        customTaskList.add(t5);
        customTaskList.add(t6);
        customTaskList.add(t7);
        customTaskList.add(t8);
        customTaskList.add(t9);
        customTaskList.add(t10);
        customTaskList.add(t11);
        customTaskList.add(t12);

        refreshCreatedTasksListView();
    }

    private String findCellIdByLabel(java.util.Map<String, String> cellIdToLabel, String label) {
        for (java.util.Map.Entry<String, String> entry : cellIdToLabel.entrySet()) {
            if (entry.getValue().equals(label)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private String getRobotColorHex(String robotId) {
        if (robotId == null) {
            return "#00E5FF";
        }
        String[] neonColors = {
            "#00E5FF", // Neon Cyan/Blue (First robot / Default)
            "#BD00FF", // Neon Purple/Violet
            "#FF007F", // Neon Pink/Magenta
            "#00FF66", // Neon Green
            "#FF6C00", // Neon Orange/Amber
            "#FFE600", // Neon Yellow
            "#7B2CBF"  // Neon Deep Purple
        };
        int index = 0;
        if (robotId.startsWith("R")) {
            try {
                int num = Integer.parseInt(robotId.substring(1));
                index = (num - 1) % neonColors.length;
                if (index < 0) index = 0;
            } catch (NumberFormatException e) {
                index = Math.abs(robotId.hashCode()) % neonColors.length;
            }
        } else {
            index = Math.abs(robotId.hashCode()) % neonColors.length;
        }
        return neonColors[index];
    }

    public String getStartNodeText()        { return startNodeField.getText().trim(); }
    public void setStartNodeText(String t)  { startNodeField.setText(t); }
    public String getEndNodeText()          { return endNodeField.getText().trim(); }
    public void setEndNodeText(String t)    { endNodeField.setText(t); }
    public void setGridView(WarehouseGridView gv) { this.gridView = gv; }
}
