package com.warehouse.algorithm.pathfinding;

import com.warehouse.model.domain.Node;
import com.warehouse.model.graph.Edge;
import com.warehouse.model.graph.Graph;

import java.util.*;

/**
 * An implementation of ShortestPathStrategy using Dijkstra's Algorithm.
 * Computes individual tile-to-tile navigation paths across the warehouse graph.
 */
public class DijkstraAlgorithm implements ShortestPathStrategy {

    @Override
    public List<Edge> findShortestPath(Graph graph, Node start, Node end) {
        System.out.println("Executing Dijkstra's Algorithm from " + start.getId() + " to " + end.getId());

        // Edge case checks
        if (graph == null || start == null || end == null) {
            return new ArrayList<>();
        }
        if (start.equals(end)) {
            return new ArrayList<>();
        }

        Map<Node, Double> distances = new HashMap<>();
        // Tracks the edge used to reach a node
        Map<Node, Edge> parentEdges = new HashMap<>();
        Set<Node> settledNodes = new HashSet<>();

        // Set all initial distances to infinity
        for (Node node : graph.getNodes()) {
            distances.put(node, Double.MAX_VALUE);
        }
        distances.put(start, 0.0);

        // Setup PriorityQueue to prioritize the node with the lowest known tentative distance
        PriorityQueue<Node> priorityQueue = new PriorityQueue<>(
                (node1, node2) -> Double.compare(distances.get(node1), distances.get(node2))
        );
        priorityQueue.add(start);

        boolean pathFound = false;

        // Main evaluation loop
        while (!priorityQueue.isEmpty()) {
            Node current = priorityQueue.poll();

            // If we pulled out a node with infinity distance, remaining nodes are unreachable
            if (distances.get(current) == Double.MAX_VALUE) {
                break;
            }

            if (current.equals(end)) {
                pathFound = true;
                break;
            }

            if (settledNodes.contains(current)) {
                continue;
            }
            settledNodes.add(current);

            // Scan all outgoing neighbors
            for (Edge edge : graph.getNeighbors(current)) {
                Node neighbor = edge.getDestination();

                if (settledNodes.contains(neighbor)) {
                    continue;
                }

                // Relaxation calculation
                double newDistance = distances.get(current) + edge.getWeight();
                if (newDistance < distances.get(neighbor)) {
                    distances.put(neighbor, newDistance);
                    parentEdges.put(neighbor, edge);

                    // update queue position
                    priorityQueue.remove(neighbor);
                    priorityQueue.add(neighbor);
                }
            }
        }

        // Backtrack route assembly
        List<Edge> shortestPath = new ArrayList<>();
        if (!pathFound) {
            System.out.println("No viable path exists between selected nodes.");
            return shortestPath;
        }

        // Trace backward from end node to start node using our parent edges
        Node traceNode = end;
        while (parentEdges.containsKey(traceNode)) {
            Edge routeEdge = parentEdges.get(traceNode);
            shortestPath.add(0, routeEdge);
            traceNode = routeEdge.getSource();
        }

        System.out.println("Shortest path calculated successfully! Total moves: " + shortestPath.size());
        return shortestPath;
    }
}