package com.warehouse.model.domain;
import java.util.Objects;

public class Node {
    private final String id;
    private final int x;
    private final int y;
    private NodeType nodeType;
    private String name;
    private NodeColor color = NodeColor.WHITE;
    private int discoveryTime = 0;
    private int finishTime = 0;
    private final java.util.List<Node> neighbors = new java.util.ArrayList<>();

    public Node(String id, int x, int y, NodeType nodeType) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.nodeType = nodeType;
        this.name = id;
    }
    public Node(String id, int x, int y) {
        this(id, x, y, NodeType.EMPTY);
    }
    //Getter methods
    public String getId() { return id; }
    public int getX() { return x; }
    public int getY() { return y; }
    public NodeType getNodeType() { return nodeType; }
    
    public String getName() {
        return name != null ? name : id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public NodeColor getColor() {
        return color;
    }

    public void setColor(NodeColor color) {
        this.color = color;
    }

    public int getDiscoveryTime() {
        return discoveryTime;
    }

    public void setDiscoveryTime(int discoveryTime) {
        this.discoveryTime = discoveryTime;
    }

    public int getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(int finishTime) {
        this.finishTime = finishTime;
    }

    public java.util.List<Node> getNeighbors() {
        return neighbors;
    }

    public void addNeighbor(Node neighbor) {
        if (neighbor != null && !neighbors.contains(neighbor)) {
            neighbors.add(neighbor);
        }
    }

    public void clearNeighbors() {
        neighbors.clear();
    }

    // Setter methods
    public void setNodeType(NodeType nodeType) { this.nodeType = nodeType; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return x == node.x && y == node.y && Objects.equals(id, node.id);
    }
    @Override
    public int hashCode() {
        return Objects.hash(id, x, y);
    }

    @Override
    public String toString() {
        return String.format("%s (%d,%d) [%s]", id, x, y, nodeType);
    }
}
