package com.warehouse.model.domain;
import java.util.Objects;

public class Node {
    private final String id;
    private final int x;
    private final int y;
    private NodeType nodeType;

    public Node(String id, int x, int y, NodeType nodeType) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.nodeType = nodeType;
    }
    public Node(String id, int x, int y) {
        this(id, x, y, NodeType.EMPTY);
    }
    //Getter methods
    public String getId() { return id; }
    public int getX() { return x; }
    public int getY() { return y; }
    public NodeType getNodeType() { return nodeType; }
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
