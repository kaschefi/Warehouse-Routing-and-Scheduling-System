package com.warehouse.algorithm.pathfinding;
import com.warehouse.model.domain.Node;
import com.warehouse.model.graph.Edge;
import com.warehouse.model.graph.Graph;

import java.util.List;
import java.util.ArrayList;

public class DijkstraAlgorithm implements ShortestPathStrategy {

    @Override
    public List<Edge> findShortestPath(Graph graph, Node start, Node end) {
        // TODO: Complete actual algorithm internals in later step
        System.out.println("Executing Dijkstra's Algorithm from " + start + " to " + end);
        return new ArrayList<>();
    }
}