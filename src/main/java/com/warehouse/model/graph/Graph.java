package com.warehouse.model.graph;

import com.warehouse.model.domain.Node;
import java.util.List;

/**
 * Core interface defining graph operations specifically tailored for the warehouse layout.
 */
public interface Graph {
    void addNode(Node node);
    void addEdge(Edge edge);
    void removeNode(Node node);
    List<Node> getNodes();
    List<Edge> getNeighbors(Node node);
    void resetTraversalState();
}