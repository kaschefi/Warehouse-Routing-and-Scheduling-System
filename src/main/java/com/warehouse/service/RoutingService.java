package com.warehouse.service;

import com.warehouse.algorithm.pathfinding.ShortestPathStrategy;
import com.warehouse.model.domain.Node;
import com.warehouse.model.domain.NodeType;
import com.warehouse.model.graph.AdjacencyListGraph;
import com.warehouse.model.graph.Edge;
import com.warehouse.model.graph.Graph;
import java.util.List;

/**
 * Service managing the warehouse layout network generation and pathfinding requests.
 */
public class RoutingService {
    private Graph warehouseMap;
    private final ShortestPathStrategy pathFinder;

    public RoutingService(ShortestPathStrategy pathFinder) {
        this.warehouseMap = new AdjacencyListGraph();
        this.pathFinder = pathFinder;
    }

    /**
     * Converts a 2D matrix layout of node types into an active network graph.
     * Rules: Obstacles are skipped. All adjacent walkable nodes are linked orthogonally.
     * * @param gridMatrix A 2D array matching the rows/cols configuration of the UI grid.
     */
    public void generateGraphFromGrid(NodeType[][] gridMatrix) {
        // Reset to an empty graph
        this.warehouseMap = new AdjacencyListGraph();

        int rows = gridMatrix.length;
        int cols = gridMatrix[0].length;

        // Create and add all valid walkable Node objects to the graph framework
        Node[][] nodeTracker = new Node[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                NodeType type = gridMatrix[r][c];

                // Skip obstacles entirely
                if (type != NodeType.OBSTACLE) {
                    String standardId = "N_" + c + "_" + r;
                    Node node = new Node(standardId, c, r, type);
                    nodeTracker[r][c] = node;
                    warehouseMap.addNode(node);
                }
            }
        }

        // Loop through nodes to stitch adjacent paths together (Edges)
        // Orthogonal directions: Down (1,0), Up (-1,0), Right (0,1), Left (0,-1)
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Node currentNode = nodeTracker[r][c];
                if (currentNode == null) continue; // Skip spots where obstacles sit

                for (int[] dir : directions) {
                    int neighborRow = r + dir[0];
                    int neighborCol = c + dir[1];

                    // Boundary checking logic
                    if (neighborRow >= 0 && neighborRow < rows && neighborCol >= 0 && neighborCol < cols) {
                        Node neighborNode = nodeTracker[neighborRow][neighborCol];

                        // If the neighbor exists (is walkable), create a directed edge path
                        if (neighborNode != null) {
                            Edge stepEdge = new Edge(currentNode, neighborNode, 1.0);
                            warehouseMap.addEdge(stepEdge);
                        }
                    }
                }
            }
        }
        System.out.println("Successfully generated Graph! Total active nodes added: " + warehouseMap.getNodes().size());
    }

    public List<Edge> calculateRoute(Node start, Node end) {
        return pathFinder.findShortestPath(warehouseMap, start, end);
    }

    public Graph getWarehouseMap() {
        return warehouseMap;
    }
}