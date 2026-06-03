package com.warehouse.ui;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;

/**
 * The main layout container (BorderPane) for the application.
 * Manages the top menu bar, sidebars, central grid, and bottom status bar.
 */
public class MainView extends BorderPane {

    private StatusBarView statusBar;
    private WarehouseGridView gridView;

    public MainView() {
        setupMenu();
        setupComponents();
    }

    private void setupMenu() {
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");
        Menu viewMenu = new Menu("View");
        Menu helpMenu = new Menu("Help");

        // TODO: Add menu items and functionality
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> System.exit(0));
        fileMenu.getItems().add(exitItem);

        menuBar.getMenus().addAll(fileMenu, viewMenu, helpMenu);
        this.setTop(menuBar);
    }

    private void setupComponents() {
        statusBar = new StatusBarView();
        gridView = new WarehouseGridView(statusBar);
        LeftSidebarView leftSidebar = new LeftSidebarView();
        RightSidebarView rightSidebar = new RightSidebarView();

        this.setCenter(gridView);
        this.setLeft(leftSidebar);
        this.setRight(rightSidebar);
        this.setBottom(statusBar);
    }
}
