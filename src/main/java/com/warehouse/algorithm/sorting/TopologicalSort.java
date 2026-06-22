package com.warehouse.algorithm.sorting;

import com.warehouse.model.domain.NodeColor;
import com.warehouse.model.domain.Task;
import java.util.*;

public final class TopologicalSort implements TopologicalSortStrategy {

    @Override
    public List<Task> sort(List<Task> tasks) {
        System.out.println("Executing Clean Direct DFS Topological Sort...");
        if (tasks == null || tasks.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Task, NodeColor> colors = new HashMap<>();
        Set<Task> discovered = new HashSet<>();
        LinkedList<Task> sortedTasks = new LinkedList<>();

        // Initialize all tasks as WHITE (unvisited)
        for (Task task : tasks) {
            colors.put(task, NodeColor.WHITE);
        }

        // Core DFS Loop
        for (Task task : tasks) {
            if (!discovered.contains(task)) {
                dfsVisit(task, tasks, colors, discovered, sortedTasks);
            }
        }

        return sortedTasks;
    }

    private void dfsVisit(Task task, List<Task> tasks, Map<Task, NodeColor> colors, Set<Task> discovered, LinkedList<Task> sortedTasks) {
        // Mark as discovered and change color to GRAY
        discovered.add(task);
        colors.put(task, NodeColor.GRAY);

        // Look at tasks that DEPEND on this task
        for (Task dependent : tasks) {
            if (dependent.getDependencies().contains(task)) {

                // Cycle Detection: If a neighbor is GRAY, we have a circular deadlock
                if (colors.get(dependent) == NodeColor.GRAY) {
                    throw new IllegalStateException("Cycle detected at task: " + dependent.getName());
                }

                if (!discovered.contains(dependent)) {
                    dfsVisit(dependent, tasks, colors, discovered, sortedTasks);
                }
            }
        }

        // Mark as fully processed
        colors.put(task, NodeColor.BLACK);

        // Push to the front of the list (Reverse finishing order)
        sortedTasks.addFirst(task);
    }
}