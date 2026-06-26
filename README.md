# RoboFlow: Warehouse Routing and Scheduling System

RoboFlow is a graph-based warehouse management and simulation system built in Java and JavaFX. It simulates autonomous robots operating in a warehouse grid, planning optimal paths, connecting infrastructure, and scheduling tasks with dependencies.

The project models the warehouse floor as a graph where walkable cells are nodes and adjacent cells are connected by weighted edges. It visualizes navigation paths, infrastructure layout (Minimum Spanning Trees), and task dependency orders in real time.

## Core Features and Algorithms

The system integrates three primary graph-based algorithms:

### 1. Route Planning (Dijkstra's Algorithm)
Calculates the shortest grid distance for autonomous robots navigating from a start station to a destination station.
* **Data Structure:** Graph modeled via an adjacency list.
* **Algorithm:** Dijkstra's Algorithm (with priority queue optimization).
* **Behavior:** Automatically avoids obstacles placed on the warehouse floor and updates path visualization.

### 2. Infrastructure Design (Prim's Algorithm)
Connects critical warehouse facilities (such as charging stations and drop zones) with the minimum possible cabling or communication line costs.
* **Algorithm:** Prim's Minimum Spanning Tree (MST) Algorithm.
* **Behavior:** Computes the overall MST linking all active stations and highlights the optimal connections on the grid, outputting the total infrastructure cost.

### 3. Task Scheduling (Topological Sort)
Schedules order-fulfillment and manufacturing tasks while respecting sequential dependencies.
* **Data Structure:** Directed Acyclic Graph (DAG).
* **Algorithm:** Depth-First Search (DFS) based Topological Sort.
* **Cycle Detection:** Automatically detects circular dependencies (deadlocks) using node-coloring (White-Gray-Black DFS classification) and raises alert messages to the user if cycles are found.

## Project Architecture

The codebase follows clean software engineering principles, separating domain models, algorithmic strategies, and JavaFX views.

* **Domain Model:** Defines core elements like nodes, edges, graph structure, robots, and tasks.
* **Strategy Pattern:** Decouples algorithms from application logic using abstract strategies (ShortestPathStrategy, MinimumSpanningTreeStrategy, and TopologicalSortStrategy), allowing easy replacement of algorithms.
* **Service Layer:** Manages states and delegates business logic (RoutingService, NetworkDesignService, TaskSchedulingService, and RobotManagementService).
* **UI Layer:** A rich JavaFX interface featuring:
  * A 20x20 Warehouse Grid for visual interaction.
  * Sidebars for adding/removing obstacles, robots, charging stations, and drop zones.
  * Sidebars for planning paths, generating MSTs, and viewing task schedules.
  * A Terminal Log console for system logs.
  * A Bottom Status Bar for runtime feedback.

## Requirements

* Java Development Kit (JDK) 17 or higher
* Apache Maven 3.6 or higher

## Getting Started

### 1. Build the Project
Compile the Java classes and download dependencies using Maven:
```bash
mvn clean compile
```

### 2. Run the Application
Start the JavaFX GUI via the JavaFX Maven plugin:
```bash
mvn javafx:run
```

### 3. Run the Tests
Verify the implementation using JUnit tests:
```bash
mvn test
```

## System Layout

```
src/
└── main/
    ├── java/
    │   └── com/
    │       └── warehouse/
    │           ├── algorithm/
    │           │   ├── pathfinding/    (Dijkstra Pathfinding)
    │           │   ├── sorting/        (Topological Sort & Cycle Detection)
    │           │   └── spanningtree/   (Prim's MST)
    │           ├── model/
    │           │   ├── domain/         (Node, Robot, Task models)
    │           │   └── graph/          (Graph & Adjacency List)
    │           ├── service/            (Routing, Management, & Scheduling Services)
    │           └── ui/                 (JavaFX Views & Main Launcher)
    └── resources/
        ├── style.css                   (UI Theme styling)
        └── *.obj                       (Robot and Drone 3D models)
```
