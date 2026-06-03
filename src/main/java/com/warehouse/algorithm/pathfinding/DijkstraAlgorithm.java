package com.warehouse.algorithm.pathfinding;
import com.warehouse.model.graph.Edge;
import com.warehouse.model.graph.Graph;
import com.warehouse.model.graph.Vertex;
import java.util.List;
import java.util.ArrayList;

public class DijkstraAlgorithm<T> implements ShortestPathStrategy<T> {
    
    @Override
    public List<Edge<T>> findShortestPath(Graph<T> graph, Vertex<T> start, Vertex<T> end) {
        // TODO: Implement Dijkstra's algorithm here!
        // Requirement: Must implement algorithm yourself. Do not use built-in library pathfinding.
        System.out.println("Executing Dijkstra's Algorithm...");
        return new ArrayList<>(); // Return dummy list for now
    }
}
