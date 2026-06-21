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
    private double batteryLevel = 100.0;
    private String activeTaskId = "None";

    public Robot(String id, String name, Node currentNode) {
        this.id = id;
        this.name = name;
        this.currentNode = currentNode;
        this.batteryLevel = 100.0;
        this.activeTaskId = "None";
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
        if (currentNode != null && currentNode.getNodeType() == NodeType.CHARGING_STATION) {
            this.batteryLevel = 100.0;
        }
    }

    public double getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(double batteryLevel) {
        this.batteryLevel = Math.max(0.0, Math.min(100.0, batteryLevel));
    }

    public String getActiveTaskId() {
        return activeTaskId;
    }

    public void setActiveTaskId(String activeTaskId) {
        this.activeTaskId = activeTaskId;
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