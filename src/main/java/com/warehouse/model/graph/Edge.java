package com.warehouse.model.graph;

import com.warehouse.model.domain.Node;

public class Edge<T> {
    private Node source;
    private Node destination;
    private double weight;
    
    public Edge(Node source, Node destination, double weight) {
        this.source = source;
        this.destination = destination;
        this.weight = weight;
    }
    
    public Node getSource() { return source; }
    public Node getDestination() { return destination; }
    public double getWeight() { return weight; }
    @Override
    public String toString() {
        return String.format("Edge: %s -> %s (Weight: %.1f)", source.getId(), destination.getId(), weight);
    }
}
