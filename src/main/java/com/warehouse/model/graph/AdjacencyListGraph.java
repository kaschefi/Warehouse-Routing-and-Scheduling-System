package com.warehouse.model.graph;

import com.warehouse.model.domain.Node;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An adjacency list implementation tracking warehouse layout paths using internal hash maps.
 */
public class AdjacencyListGraph implements Graph {
    // Maps each Node to its list of outgoing Edges
    private final Map<Node, List<Edge>> adjList;

    public AdjacencyListGraph() {
        this.adjList = new HashMap<>();
    }

    @Override
    public void addNode(Node node) {
        if (node != null) {
            adjList.putIfAbsent(node, new ArrayList<>());
        }
    }

    @Override
    public void addEdge(Edge edge) {
        if (edge == null) return;

        // Ensure both endpoints exist in the graph structure first
        addNode(edge.getSource());
        addNode(edge.getDestination());

        adjList.get(edge.getSource()).add(edge);
    }

    @Override
    public void removeNode(Node node) {
        if (node == null || !adjList.containsKey(node)) return;

        // 1. Remove the node and all of its outgoing edges
        adjList.remove(node);

        // 2. Scan remaining nodes and remove any incoming edges pointing to the deleted node
        for (Node currentKey : adjList.keySet()) {
            List<Edge> edges = adjList.get(currentKey);
            edges.removeIf(edge -> edge.getDestination().equals(node));
        }
    }

    @Override
    public List<Node> getNodes() {
        return new ArrayList<>(adjList.keySet());
    }

    @Override
    public List<Edge> getNeighbors(Node node) {
        return adjList.getOrDefault(node, new ArrayList<>());
    }
}