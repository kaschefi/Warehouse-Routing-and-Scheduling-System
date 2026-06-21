package com.warehouse.model.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents an operational task within the warehouse scheduling engine.
 * Maintains an internal list of prerequisites to support Directed Acyclic Graph (DAG) sorting.
 */
public class Task {
    private final String id;
    private final String name;
    private final List<Task> dependencies;
    private String targetNodeId;
    private String assignedRobotId; // which robot is responsible for this task
    private boolean active;
    private double progress;

    public Task(String id, String name) {
        this.id = id;
        this.name = name;
        this.dependencies = new ArrayList<>();
        this.active = false;
        this.progress = 0.0;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTargetNodeId() {
        return targetNodeId;
    }

    public void setTargetNodeId(String targetNodeId) {
        this.targetNodeId = targetNodeId;
    }

    public String getAssignedRobotId() {
        return assignedRobotId;
    }

    public void setAssignedRobotId(String assignedRobotId) {
        this.assignedRobotId = assignedRobotId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    /**
     * @return A list of tasks that must be executed BEFORE this task can begin.
     */
    public List<Task> getDependencies() {
        return dependencies;
    }

    /**
     * Adds a prerequisite dependency to this task.
     * @param task The task that must be completed first.
     */
    public void addDependency(Task task) {
        if (task != null && !task.equals(this) && !dependencies.contains(task)) {
            this.dependencies.add(task);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return Objects.equals(id, task.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", name, id);
    }
}