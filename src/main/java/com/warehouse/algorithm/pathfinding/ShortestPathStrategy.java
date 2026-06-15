package com.warehouse.algorithm.pathfinding;
import com.warehouse.model.domain.Node;
import com.warehouse.model.graph.Edge;
import com.warehouse.model.graph.Graph;

import java.util.List;

public interface ShortestPathStrategy {
    List<Edge> findShortestPath(Graph graph, Node start, Node end);
}
