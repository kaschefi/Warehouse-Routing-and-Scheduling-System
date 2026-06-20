package com.warehouse.ui;

import com.warehouse.algorithm.pathfinding.DijkstraAlgorithm;
import com.warehouse.algorithm.spanningtree.PrimAlgorithm;
import com.warehouse.algorithm.sorting.KahnAlgorithm;
import com.warehouse.service.NetworkDesignService;
import com.warehouse.service.RoutingService;
import com.warehouse.service.TaskSchedulingService;
import com.warehouse.service.RobotManagementService;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;

/**
 * The main layout container (BorderPane) for the application.
 * Manages the top menu bar, sidebars, central grid, and bottom status bar.
 */
public class MainView extends BorderPane {

    private final RoutingService routingService;
    private final NetworkDesignService networkDesignService;
    private final TaskSchedulingService taskSchedulingService;
    private final RobotManagementService robotManagementService;

    private StatusBarView statusBar;
    private WarehouseGridView gridView;
    private LeftSidebarView leftSidebar;
    private RightSidebarView rightSidebar;

    public MainView() {
        // Instantiate the backend algorithms
        DijkstraAlgorithm dijkstra = new DijkstraAlgorithm();
        PrimAlgorithm prim = new PrimAlgorithm();
        KahnAlgorithm kahn = new KahnAlgorithm();

        // Instantiate the services with their respective strategies
        this.routingService = new RoutingService(dijkstra);
        // Start NetworkDesignService with the initial Graph from RoutingService
        this.networkDesignService = new NetworkDesignService(routingService.getWarehouseMap(), prim);
        this.taskSchedulingService = new TaskSchedulingService(kahn);
        this.robotManagementService = new RobotManagementService();

        setupMenu();
        setupComponents();
    }

    private void setupMenu() {
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");
        Menu viewMenu = new Menu("View");
        Menu helpMenu = new Menu("Help");

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> System.exit(0));
        fileMenu.getItems().add(exitItem);

        menuBar.getMenus().addAll(fileMenu, viewMenu, helpMenu);
        this.setTop(menuBar);
    }

    private void setupComponents() {
        statusBar = new StatusBarView();
        
        // Pass shared services and UI references down via constructor injection
        rightSidebar = new RightSidebarView(routingService, networkDesignService, taskSchedulingService, statusBar, robotManagementService);
        gridView = new WarehouseGridView(statusBar, routingService, rightSidebar, robotManagementService);
        rightSidebar.setGridView(gridView);
        
        leftSidebar = new LeftSidebarView(gridView, statusBar, robotManagementService);

        this.setCenter(gridView);
        this.setLeft(leftSidebar);
        this.setRight(rightSidebar);
        this.setBottom(statusBar);
    }
}
