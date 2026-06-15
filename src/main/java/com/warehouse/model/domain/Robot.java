package com.warehouse.model.domain;

import java.util.Objects;

/**
 * Represents an autonomous robot operating within the warehouse environment.
 * Tracks its identity and its current physical location on the graph grid.
 */
public class Robot {
    private final String id;
    private final String name;
    private Node currentNode;

    public Robot(String id, String name, Node currentNode) {
        this.id = id;
        this.name = name;
        this.currentNode = currentNode;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Node getCurrentNode() {
        return currentNode;
    }

    public void setCurrentNode(Node currentNode) {
        this.currentNode = currentNode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Robot robot = (Robot) o;
        return Objects.equals(id, robot.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Robot %s: %s @ %s", id, name,
                currentNode != null ? currentNode.getId() : "Unplaced");
    }
}