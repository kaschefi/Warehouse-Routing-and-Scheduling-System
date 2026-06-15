package com.warehouse.algorithm.pathfinding;
import com.warehouse.model.graph.Edge;
import com.warehouse.model.graph.Graph;

import java.util.List;

public interface ShortestPathStrategy<T> {
    List<Edge<T>> findShortestPath(Graph<T> graph, Vertex<T> start, Vertex<T> end);
}
