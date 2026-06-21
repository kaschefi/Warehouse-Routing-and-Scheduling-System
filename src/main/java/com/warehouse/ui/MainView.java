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
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;

/**
 * The main layout container for the RoboFlow application.
 *
 * Architecture:
 *   BorderPane (root)
 *     ├── TOP:    MenuBar
 *     ├── CENTER: horizontal SplitPane
 *     │              ├── LeftSidebarView  (fixed)
 *     │              ├── WarehouseGridView (grows)
 *     │              └── RightSidebarView (fixed, scrollable)
 *     └── BOTTOM: StatusBarView
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
        // Instantiate algorithms
        DijkstraAlgorithm dijkstra = new DijkstraAlgorithm();
        PrimAlgorithm prim = new PrimAlgorithm();
        KahnAlgorithm kahn = new KahnAlgorithm();

        // Instantiate services
        this.routingService = new RoutingService(dijkstra);
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

        // Build right sidebar
        rightSidebar = new RightSidebarView(
                routingService,
                networkDesignService,
                taskSchedulingService,
                statusBar,
                robotManagementService
        );

        // Build grid
        gridView = new WarehouseGridView(statusBar, routingService, rightSidebar, robotManagementService);
        rightSidebar.setGridView(gridView);

        leftSidebar = new LeftSidebarView(gridView, statusBar, robotManagementService);

        // ── Horizontal SplitPane: left | center grid | right ─────────────────
        SplitPane horizontalSplit = new SplitPane();
        horizontalSplit.getStyleClass().add("main-split-pane");
        horizontalSplit.getItems().addAll(leftSidebar, gridView, rightSidebar);
        horizontalSplit.setDividerPositions(0.13, 0.78);
        SplitPane.setResizableWithParent(leftSidebar, Boolean.FALSE);
        SplitPane.setResizableWithParent(rightSidebar, Boolean.FALSE);

        this.setCenter(horizontalSplit);
        this.setBottom(statusBar);
    }
}
