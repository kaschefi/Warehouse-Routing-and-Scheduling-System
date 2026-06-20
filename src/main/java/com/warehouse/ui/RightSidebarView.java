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
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * The right sidebar containing operations for pathfinding, MST, and scheduling.
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

    private final List<Task> customTaskList = new java.util.ArrayList<>();
    private TextField taskNameField;
    private ComboBox<String> locationComboBox;
    private ComboBox<String> dependencyComboBox;
    private ListView<Task> createdTasksListView;
    private VBox taskBuilderCard;

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
        this.setSpacing(15);
        this.setPrefWidth(260);

        setupPathfindingSection();
        setupNetworkPlanningSection();
        setupTaskSchedulingSection();
    }

    private void setupPathfindingSection() {
        VBox card = new VBox(10);
        card.getStyleClass().add("tactical-card");

        Label title = new Label("PATHFINDING");
        title.getStyleClass().add("card-title");

        startNodeField = new TextField();
        startNodeField.setPromptText("Start Node");

        endNodeField = new TextField();
        endNodeField.setPromptText("End Node");

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
                // Generate graph from active grid state to include/exclude obstacles and robots
                List<String> occupied = new java.util.ArrayList<>();
                for (Robot r : robotManagementService.getActiveFleet()) {
                    if (r.getCurrentNode() != null) {
                        occupied.add(r.getCurrentNode().getId());
                    }
                }
                routingService.generateGraphFromGrid(gridView.getGridState(), occupied);
                Graph graph = routingService.getWarehouseMap();
                
                // Locate start and end nodes from the graph
                Node startNode = null;
                Node endNode = null;
                for (Node node : graph.getNodes()) {
                    if (node.getId().equals(startId)) {
                        startNode = node;
                    }
                    if (node.getId().equals(endId)) {
                        endNode = node;
                    }
                }
                
                if (startNode == null) {
                    statusBar.setStatus("Error: Start Node (" + startId + ") is an obstacle or invalid!");
                    return;
                }
                if (endNode == null) {
                    statusBar.setStatus("Error: End Node (" + endId + ") is an obstacle or invalid!");
                    return;
                }
                
                // Calculate shortest path
                List<Edge> path = routingService.calculateRoute(startNode, endNode);
                
                if (path == null || path.isEmpty()) {
                    statusBar.setStatus("No path found from " + startId + " to " + endId);
                    System.out.println("No path found from " + startId + " to " + endId);
                    gridView.highlightCalculatedRoute(null);
                } else {
                    statusBar.setStatus("Shortest path calculated: " + path.size() + " steps");
                    System.out.println("--- Dijkstra Shortest Path Result ---");
                    for (int i = 0; i < path.size(); i++) {
                        System.out.println((i + 1) + ". " + path.get(i));
                    }
                    System.out.println("Total Path Size: " + path.size() + " steps");
                    gridView.highlightCalculatedRoute(path);
                }
            } catch (Exception ex) {
                statusBar.setStatus("Error calculating path: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        card.getChildren().addAll(title, startNodeField, endNodeField, btnFindPath);
        this.getChildren().add(card);
    }

    private void setupNetworkPlanningSection() {
        VBox card = new VBox(10);
        card.getStyleClass().add("tactical-card");

        Label title = new Label("NETWORK PLANNING");
        title.getStyleClass().add("card-title");

        Button btnMST = new Button("Generate MST");
        btnMST.setMaxWidth(Double.MAX_VALUE);
        btnMST.setOnAction(e -> {
            if (gridView == null) {
                statusBar.setStatus("Error: GridView is not initialized!");
                return;
            }
            
            try {
                // 1. Generate full active graph incorporating user obstacles
                routingService.generateGraphFromGrid(gridView.getGridState());
                Graph fullGraph = routingService.getWarehouseMap();
                
                // 2. Isolate special nodes (Charging Stations and Drop Zones)
                List<Node> specialNodes = new java.util.ArrayList<>();
                for (Node node : fullGraph.getNodes()) {
                    if (node.getNodeType() == com.warehouse.model.domain.NodeType.CHARGING_STATION ||
                        node.getNodeType() == com.warehouse.model.domain.NodeType.DROP_ZONE) {
                        specialNodes.add(node);
                    }
                }
                
                // 3. Graceful check if fewer than 2 special stations are placed
                if (specialNodes.size() < 2) {
                    statusBar.setStatus("Please place at least two stations to design a network.");
                    gridView.highlightMST(null);
                    return;
                }
                
                // 4. Compute Dijkstra bridges (shortest paths) between every pair of special stations
                Graph specialGraph = new com.warehouse.model.graph.AdjacencyListGraph();
                for (Node node : specialNodes) {
                    specialGraph.addNode(node);
                }
                
                java.util.Map<String, List<Edge>> virtualEdgePaths = new java.util.HashMap<>();
                for (int i = 0; i < specialNodes.size(); i++) {
                    Node u = specialNodes.get(i);
                    for (int j = i + 1; j < specialNodes.size(); j++) {
                        Node v = specialNodes.get(j);
                        
                        List<Edge> path = routingService.calculateRoute(u, v);
                        if (path != null && !path.isEmpty()) {
                            double pathWeight = path.stream().mapToDouble(Edge::getWeight).sum();
                            
                            Edge uv = new Edge(u, v, pathWeight);
                            Edge vu = new Edge(v, u, pathWeight);
                            
                            specialGraph.addEdge(uv);
                            specialGraph.addEdge(vu);
                            
                            virtualEdgePaths.put(u.getId() + "-" + v.getId(), path);
                            virtualEdgePaths.put(v.getId() + "-" + u.getId(), path);
                        }
                    }
                }
                
                // 5. Pass this specialized graph to NetworkDesignService
                networkDesignService.setWarehouseMap(specialGraph);
                
                // 6. Generate MST
                List<Edge> mstEdges = networkDesignService.generateOptimalNetwork();
                
                if (mstEdges == null || mstEdges.isEmpty()) {
                    statusBar.setStatus("No MST generated (Stations might be disconnected).");
                    System.out.println("No MST generated.");
                    gridView.highlightMST(null);
                } else {
                    // 7. Flatten virtual MST edges to their real grid paths
                    List<Edge> realMstEdges = new java.util.ArrayList<>();
                    for (Edge mstEdge : mstEdges) {
                        String key = mstEdge.getSource().getId() + "-" + mstEdge.getDestination().getId();
                        List<Edge> path = virtualEdgePaths.get(key);
                        if (path != null) {
                            realMstEdges.addAll(path);
                        }
                    }
                    
                    double totalCost = mstEdges.stream().mapToDouble(Edge::getWeight).sum();
                    statusBar.setStatus(String.format("Infrastructure grid established connecting %d stations. Total cost: %.2f units", 
                            specialNodes.size(), totalCost));
                    
                    System.out.println("--- Pairwise Virtual MST Result ---");
                    for (int i = 0; i < mstEdges.size(); i++) {
                        System.out.println((i + 1) + ". " + mstEdges.get(i));
                    }
                    System.out.printf("Total MST Infrastructure Cost: %.2f%n", totalCost);
                    
                    // Render paths visually
                    gridView.highlightMST(realMstEdges);
                }
            } catch (Exception ex) {
                statusBar.setStatus("Error generating MST: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        card.getChildren().addAll(title, btnMST);
        this.getChildren().add(card);
    }

    private void setupTaskSchedulingSection() {
        taskBuilderCard = new VBox(10);
        taskBuilderCard.getStyleClass().add("tactical-card");

        Label title = new Label("TASK BUILDER");
        title.getStyleClass().add("card-title");

        taskNameField = new TextField();
        taskNameField.setPromptText("Task Name (e.g. Sort Items)");

        locationComboBox = new ComboBox<>();
        locationComboBox.setPromptText("Select Location");
        locationComboBox.setMaxWidth(Double.MAX_VALUE);
        locationComboBox.setOnShowing(event -> {
            locationComboBox.getItems().clear();
            if (gridView != null) {
                locationComboBox.getItems().addAll(gridView.getStationLabels().values());
            }
        });

        dependencyComboBox = new ComboBox<>();
        dependencyComboBox.setPromptText("Select Dependency");
        dependencyComboBox.setMaxWidth(Double.MAX_VALUE);
        dependencyComboBox.setOnShowing(event -> {
            dependencyComboBox.getItems().clear();
            dependencyComboBox.getItems().add("None");
            for (Task t : customTaskList) {
                dependencyComboBox.getItems().add(t.getId() + " - " + t.getName());
            }
        });

        Button btnAddTask = new Button("Add Task");
        btnAddTask.setMaxWidth(Double.MAX_VALUE);
        btnAddTask.setOnAction(e -> {
            String name = taskNameField.getText().trim();
            if (name.isEmpty()) {
                statusBar.setStatus("Warning: Task Name cannot be empty!");
                return;
            }

            String loc = locationComboBox.getValue();
            if (loc == null || loc.isEmpty()) {
                statusBar.setStatus("Warning: Please select a target location!");
                return;
            }

            String cellId = null;
            if (gridView != null) {
                for (java.util.Map.Entry<String, String> entry : gridView.getStationLabels().entrySet()) {
                    if (entry.getValue().equals(loc)) {
                        cellId = entry.getKey();
                        break;
                    }
                }
            }

            if (cellId == null) {
                statusBar.setStatus("Error: Selected location is invalid.");
                return;
            }

            Task task = new Task("T" + (customTaskList.size() + 1), name);
            task.setTargetNodeId(cellId);

            String depValue = dependencyComboBox.getValue();
            if (depValue != null && !depValue.equals("None")) {
                String depId = depValue.split(" - ")[0];
                for (Task t : customTaskList) {
                    if (t.getId().equals(depId)) {
                        task.addDependency(t);
                        break;
                    }
                }
            }

            customTaskList.add(task);
            refreshCreatedTasksListView();

            taskNameField.clear();
            locationComboBox.setValue(null);
            dependencyComboBox.setValue(null);
            statusBar.setStatus("Task " + task.getId() + " added at " + loc);
        });

        Label listLabel = new Label("CREATED TASKS");

        createdTasksListView = new ListView<>();
        createdTasksListView.setPrefHeight(120);
        createdTasksListView.setCellFactory(lv -> new javafx.scene.control.ListCell<Task>() {
            private final javafx.scene.layout.HBox layout = new javafx.scene.layout.HBox(10);
            private final Label label = new Label();
            private final javafx.scene.control.ProgressBar pb = new javafx.scene.control.ProgressBar(0);
            
            {
                layout.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                pb.getStyleClass().add("task-progress-bar");
                layout.getChildren().addAll(label, pb);
                javafx.scene.layout.HBox.setHgrow(label, javafx.scene.layout.Priority.ALWAYS);
            }
            
            @Override
            protected void updateItem(Task task, boolean empty) {
                super.updateItem(task, empty);
                if (empty || task == null) {
                    setGraphic(null);
                } else {
                    StringBuilder depStr = new StringBuilder();
                    if (!task.getDependencies().isEmpty()) {
                        for (int i = 0; i < task.getDependencies().size(); i++) {
                            Task dep = task.getDependencies().get(i);
                            String depLoc = (gridView != null) ? gridView.getStationLabels().get(dep.getTargetNodeId()) : dep.getTargetNodeId();
                            if (depLoc == null) depLoc = dep.getTargetNodeId();
                            depStr.append(dep.getId()).append(" at ").append(depLoc);
                            if (i < task.getDependencies().size() - 1) {
                                depStr.append(", ");
                            }
                        }
                    } else {
                        depStr.append("None");
                    }
                    String displayString = String.format("%s (%s) [Deps: %s]", task.getName(), task.getId(), depStr.toString());
                    label.setText(displayString);
                    
                    if (task.isActive()) {
                        pb.setVisible(true);
                        pb.setProgress(task.getProgress());
                    } else {
                        pb.setVisible(false);
                    }
                    setGraphic(layout);
                }
            }
        });

        Button btnSchedule = new Button("Schedule Tasks");
        btnSchedule.setMaxWidth(Double.MAX_VALUE);
        btnSchedule.setOnAction(e -> {
            try {
                // Fetch the first active robot. Warn if none exist (Test 3c).
                List<Robot> fleet = robotManagementService.getActiveFleet();
                if (fleet.isEmpty()) {
                    statusBar.setStatus("Error: No active robot placed on the grid to assign tasks to.");
                    return;
                }
                Robot robot = fleet.get(0);

                if (customTaskList.isEmpty()) {
                    statusBar.setStatus("Error: Please add at least one task to schedule.");
                    return;
                }

                // Clear any existing tasks in the service
                taskSchedulingService.clearTasks();
                
                // Add dynamically built tasks to TaskSchedulingService
                for (Task t : customTaskList) {
                    taskSchedulingService.addTask(t);
                }
                
                // Call getExecutionOrder() with cycle exception handling (Test 3b)
                List<Task> executionOrder;
                try {
                    executionOrder = taskSchedulingService.getExecutionOrder();
                } catch (IllegalStateException isEx) {
                    statusBar.setStatus("Circular dependency loop detected! A valid execution schedule is impossible.");
                    
                    // Flash background warning-red twice using FadeTransition
                    taskBuilderCard.setStyle("-fx-background-color: #FF3333;");
                    javafx.animation.FadeTransition fadeCard = new javafx.animation.FadeTransition(javafx.util.Duration.millis(200), taskBuilderCard);
                    fadeCard.setFromValue(1.0);
                    fadeCard.setToValue(0.4);
                    fadeCard.setCycleCount(4);
                    fadeCard.setAutoReverse(true);
                    fadeCard.setOnFinished(evt -> {
                        taskBuilderCard.setStyle("");
                        taskBuilderCard.setOpacity(1.0);
                    });
                    fadeCard.play();

                    // Render flashing warning label
                    taskBuilderCard.getChildren().removeIf(node -> node.getStyleClass().contains("deadlock-warning-label"));
                    Label warningLabel = new Label("[!] CRITICAL DEADLOCK: SCHEDULING ABORTED [!]");
                    warningLabel.getStyleClass().add("deadlock-warning-label");
                    warningLabel.setMaxWidth(Double.MAX_VALUE);
                    
                    taskBuilderCard.getChildren().add(1, warningLabel);
                    
                    javafx.animation.FadeTransition fadeLabel = new javafx.animation.FadeTransition(javafx.util.Duration.millis(250), warningLabel);
                    fadeLabel.setFromValue(1.0);
                    fadeLabel.setToValue(0.1);
                    fadeLabel.setCycleCount(8);
                    fadeLabel.setAutoReverse(true);
                    fadeLabel.setOnFinished(evt -> taskBuilderCard.getChildren().remove(warningLabel));
                    fadeLabel.play();

                    return;
                }
                
                // Build chronological roadmap output
                StringBuilder orderStr = new StringBuilder();
                for (int i = 0; i < executionOrder.size(); i++) {
                    orderStr.append(executionOrder.get(i).getId());
                    if (i < executionOrder.size() - 1) {
                        orderStr.append(" -> ");
                    }
                }
                
                statusBar.setStatus("Task Schedule Order: " + orderStr.toString());
                
                System.out.println("--- Task Scheduling Roadmap ---");
                for (int i = 0; i < executionOrder.size(); i++) {
                    System.out.println((i + 1) + ". " + executionOrder.get(i));
                }
                System.out.println("Roadmap order string: " + orderStr.toString());
 
                // Trigger automated workflow simulation
                java.util.Queue<Task> taskQueue = new java.util.LinkedList<>(executionOrder);
                gridView.startWorkflowSimulation(robot, taskQueue);
                
            } catch (Exception ex) {
                statusBar.setStatus("Scheduling Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        taskBuilderCard.getChildren().addAll(
            title, 
            taskNameField, 
            locationComboBox, 
            dependencyComboBox, 
            btnAddTask, 
            listLabel, 
            createdTasksListView, 
            btnSchedule
        );
        this.getChildren().add(taskBuilderCard);
    }

    public void clearCustomTasks() {
        customTaskList.clear();
        if (createdTasksListView != null) {
            createdTasksListView.getItems().clear();
        }
        if (locationComboBox != null) {
            locationComboBox.setValue(null);
            locationComboBox.getItems().clear();
        }
        if (dependencyComboBox != null) {
            dependencyComboBox.setValue(null);
            dependencyComboBox.getItems().clear();
        }
        if (taskNameField != null) {
            taskNameField.clear();
        }
    }

    public void refreshCreatedTasksListView() {
        if (createdTasksListView == null) return;
        int selectedIndex = createdTasksListView.getSelectionModel().getSelectedIndex();
        createdTasksListView.getItems().clear();
        createdTasksListView.getItems().addAll(customTaskList);
        if (selectedIndex >= 0 && selectedIndex < customTaskList.size()) {
            createdTasksListView.getSelectionModel().select(selectedIndex);
        }
    }

    private Node findNodeByType(Graph graph, com.warehouse.model.domain.NodeType type, String defaultId) {
        for (Node node : graph.getNodes()) {
            if (node.getNodeType() == type) {
                return node;
            }
        }
        for (Node node : graph.getNodes()) {
            if (node.getId().equals(defaultId)) {
                return node;
            }
        }
        if (!graph.getNodes().isEmpty()) {
            return graph.getNodes().get(0);
        }
        return null;
    }

    public String getStartNodeText() {
        return startNodeField.getText().trim();
    }

    public void setStartNodeText(String text) {
        startNodeField.setText(text);
    }

    public String getEndNodeText() {
        return endNodeField.getText().trim();
    }

    public void setEndNodeText(String text) {
        endNodeField.setText(text);
    }

    public void setGridView(WarehouseGridView gridView) {
        this.gridView = gridView;
    }
}
