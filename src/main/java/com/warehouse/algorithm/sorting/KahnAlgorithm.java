package com.warehouse.algorithm.sorting;

import com.warehouse.model.domain.Task;
import java.util.*;

/**
 * An implementation of Topological Sort using Kahn's Algorithm (indegree-based).
 * Schedules warehouse tasks safely according to their prerequisites.
 */
public class KahnAlgorithm implements TopologicalSortStrategy {

    @Override
    public List<Task> sort(List<Task> tasks) {
        System.out.println("Executing Kahn's Topological Sort Algorithm...");

        List<Task> sortedOrder = new ArrayList<>();
        if (tasks == null || tasks.isEmpty()) {
            return sortedOrder;
        }

        // Map to track the in-degree (number of remaining dependencies) for each task
        Map<Task, Integer> inDegreeMap = new HashMap<>();

        // Initialize all tracked tasks with an in-degree of 0
        for (Task task : tasks) {
            inDegreeMap.put(task, 0);
        }

        // Calculate the in-degrees by counting how many prerequisites each task has
        for (Task task : tasks) {
            for (Task dependency : task.getDependencies()) {
                // If this task depends on something, its incoming dependency count increases
                inDegreeMap.put(task, inDegreeMap.get(task) + 1);
            }
        }

        //Create a queue and populate it with tasks that have 0 dependencies
        Queue<Task> zeroDependencyQueue = new LinkedList<>();
        for (Task task : tasks) {
            if (inDegreeMap.get(task) == 0) {
                zeroDependencyQueue.add(task);
            }
        }

        // Process the queue
        while (!zeroDependencyQueue.isEmpty()) {
            Task currentTask = zeroDependencyQueue.poll();
            sortedOrder.add(currentTask); // Safe to execute! Add to final timeline

            // Look at all remaining tasks to see if they were waiting for currentTask
            for (Task potentialDependent : tasks) {
                if (potentialDependent.getDependencies().contains(currentTask)) {
                    // Decrement its in-degree since its prerequisite is now finished
                    int reducedInDegree = inDegreeMap.get(potentialDependent) - 1;
                    inDegreeMap.put(potentialDependent, reducedInDegree);

                    // If its in-degree hits 0, it's fully unlocked!
                    if (reducedInDegree == 0) {
                        zeroDependencyQueue.add(potentialDependent);
                    }
                }
            }
        }

        // Check for circular dependency loops
        if (sortedOrder.size() != tasks.size()) {
            throw new IllegalStateException("Circular dependency loop detected! A valid execution schedule is impossible.");
        }

        return sortedOrder;
    }
}