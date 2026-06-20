package com.warehouse.ui;

import com.warehouse.model.domain.Node;
import com.warehouse.model.domain.Task;
import com.warehouse.model.graph.Edge;
import com.warehouse.model.graph.Graph;
import com.warehouse.service.NetworkDesignService;
import com.warehouse.service.RoutingService;
import com.warehouse.service.TaskSchedulingService;
import java.util.List;
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

    private final RoutingService routingService;
    private final NetworkDesignService networkDesignService;
    private final TaskSchedulingService taskSchedulingService;
    private final StatusBarView statusBar;
    private WarehouseGridView gridView;

    private TextField startNodeField;
    private TextField endNodeField;

    public RightSidebarView(RoutingService routingService,
                            NetworkDesignService networkDesignService,
                            TaskSchedulingService taskSchedulingService,
                            StatusBarView statusBar) {
        this.routingService = routingService;
        this.networkDesignService = networkDesignService;
        this.taskSchedulingService = taskSchedulingService;
        this.statusBar = statusBar;

        this.setPadding(new Insets(15));
        this.setSpacing(10);
        this.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 0 1;");
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
        title.setStyle("-fx-text-fill: #2d3748; -fx-padding: 0 0 5 0;");

        startNodeField = new TextField();
        startNodeField.setPromptText("Start Node");
        startNodeField.setStyle("-fx-background-color: #ffffff; -fx-border-color: #cbd5e0; -fx-border-radius: 4; -fx-background-radius: 4;");

        endNodeField = new TextField();
        endNodeField.setPromptText("End Node");
        endNodeField.setStyle("-fx-background-color: #ffffff; -fx-border-color: #cbd5e0; -fx-border-radius: 4; -fx-background-radius: 4;");

        Button btnFindPath = new Button("Find Shortest Path");
        btnFindPath.setMaxWidth(Double.MAX_VALUE);
        btnFindPath.setStyle("-fx-background-color: #3182ce; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-border-radius: 4; -fx-background-radius: 4;");
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
                // Generate graph from active grid state to include/exclude obstacles
                routingService.generateGraphFromGrid(gridView.getGridState());
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
                } else {
                    statusBar.setStatus("Shortest path calculated: " + path.size() + " steps");
                    System.out.println("--- Dijkstra Shortest Path Result ---");
                    for (int i = 0; i < path.size(); i++) {
                        System.out.println((i + 1) + ". " + path.get(i));
                    }
                    System.out.println("Total Path Size: " + path.size() + " steps");
                }
            } catch (Exception ex) {
                statusBar.setStatus("Error calculating path: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        this.getChildren().addAll(title, startNodeField, endNodeField, btnFindPath);
    }

    private void setupNetworkPlanningSection() {
        Label title = new Label("Network Planning");
        title.setFont(Font.font("System", FontWeight.BOLD, 14));
        title.setStyle("-fx-text-fill: #2d3748; -fx-padding: 0 0 5 0;");

        Button btnMST = new Button("Generate MST");
        btnMST.setMaxWidth(Double.MAX_VALUE);
        btnMST.setStyle("-fx-background-color: #3182ce; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-border-radius: 4; -fx-background-radius: 4;");
        btnMST.setOnAction(e -> {
            if (gridView == null) {
                statusBar.setStatus("Error: GridView is not initialized!");
                return;
            }
            
            try {
                // Generate graph from active grid state
                routingService.generateGraphFromGrid(gridView.getGridState());
                Graph activeGraph = routingService.getWarehouseMap();
                
                // Pass active graph to network design service
                networkDesignService.setWarehouseMap(activeGraph);
                
                // Generate MST
                List<Edge> mstEdges = networkDesignService.generateOptimalNetwork();
                
                if (mstEdges == null || mstEdges.isEmpty()) {
                    statusBar.setStatus("No MST generated (Graph might be empty or disconnected).");
                    System.out.println("No MST generated.");
                } else {
                    double totalCost = mstEdges.stream().mapToDouble(Edge::getWeight).sum();
                    statusBar.setStatus(String.format("MST generated: %d connections, Cost: %.2f", mstEdges.size(), totalCost));
                    
                    System.out.println("--- Minimum Spanning Tree (MST) Result ---");
                    for (int i = 0; i < mstEdges.size(); i++) {
                        System.out.println((i + 1) + ". " + mstEdges.get(i));
                    }
                    System.out.printf("Total MST Infrastructure Cost: %.2f%n", totalCost);
                }
            } catch (Exception ex) {
                statusBar.setStatus("Error generating MST: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        this.getChildren().addAll(title, btnMST);
    }

    private void setupTaskSchedulingSection() {
        Label title = new Label("Task Scheduling");
        title.setFont(Font.font("System", FontWeight.BOLD, 14));
        title.setStyle("-fx-text-fill: #2d3748; -fx-padding: 0 0 5 0;");

        Button btnSchedule = new Button("Schedule Tasks");
        btnSchedule.setMaxWidth(Double.MAX_VALUE);
        btnSchedule.setStyle("-fx-background-color: #3182ce; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-border-radius: 4; -fx-background-radius: 4;");
        btnSchedule.setOnAction(e -> {
            try {
                // Clear any existing tasks in the service
                taskSchedulingService.clearTasks();
                
                // Instantiate 4-5 dummy Task objects
                Task t1 = new Task("T1", "Pick Items");
                Task t2 = new Task("T2", "Pack Box");
                Task t3 = new Task("T3", "Attach Label");
                Task t4 = new Task("T4", "Ship Order");
                
                // Set explicit dependencies
                // T2 depends on T1
                t2.addDependency(t1);
                // T4 depends on T3 and T2
                t4.addDependency(t3);
                t4.addDependency(t2);
                
                // Add these tasks to TaskSchedulingService
                taskSchedulingService.addTask(t1);
                taskSchedulingService.addTask(t2);
                taskSchedulingService.addTask(t3);
                taskSchedulingService.addTask(t4);
                
                // Call getExecutionOrder()
                List<Task> executionOrder = taskSchedulingService.getExecutionOrder();
                
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
                
            } catch (Exception ex) {
                statusBar.setStatus("Scheduling Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        this.getChildren().addAll(title, btnSchedule);
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
