package com.warehouse.algorithm.spanningtree;

import com.warehouse.model.domain.Node;
import com.warehouse.model.graph.Edge;
import com.warehouse.model.graph.Graph;

import java.util.*;

/**
 * An implementation of the Minimum Spanning Tree Strategy using Prim's Algorithm.
 */
public class PrimAlgorithm implements MinimumSpanningTreeStrategy {

    @Override
    public List<Edge> findMST(Graph graph) {
        System.out.println("Executing Prim's Minimum Spanning Tree Algorithm...");

        List<Edge> mst = new ArrayList<>();
        if (graph == null || graph.getNodes().isEmpty()) {
            return mst;
        }

        // Pick an arbitrary starting node from the graph to seed the algorithm
        Node start = graph.getNodes().get(0);

        Map<Node, Double> key = new HashMap<>();
        Map<Node, Node> parent = new HashMap<>();

        // Priority Queue sorts nodes by their lowest known edge cost ("key")
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingDouble(key::get));

        // Initialize tracking maps
        for (Node node : graph.getNodes()) {
            key.put(node, Double.MAX_VALUE);
            parent.put(node, null);
        }

        key.put(start, 0.0);

        // Populate priority queue
        queue.addAll(graph.getNodes());

        // Core Prim's Algorithm Loop
        while (!queue.isEmpty()) {
            Node u = queue.poll();

            // If u has a parent, find the concrete Edge that linked them and add it to our MST
            if (parent.get(u) != null) {
                for (Edge edge : graph.getNeighbors(parent.get(u))) {
                    if (edge.getDestination().equals(u)) {
                        mst.add(edge);
                        break;
                    }
                }
            }

            // Evaluate all paths branching out from node u
            for (Edge edge : graph.getNeighbors(u)) {
                Node v = edge.getDestination();
                double weight = edge.getWeight();

                // If neighbor v is unvisited and this layout path is cheaper than v's current cost
                if (queue.contains(v) && weight < key.get(v)) {
                    queue.remove(v); // Remove to trigger re-sorting on re-insertion
                    parent.put(v, u);
                    key.put(v, weight);
                    queue.add(v);
                }
            }
        }

        // Print total infrastructure cost to console to match assignment specifications
        double totalCost = mst.stream().mapToDouble(Edge::getWeight).sum();
        System.out.printf("Total MST Infrastructure Cost: %.2f%n", totalCost);

        return mst;
    }
}