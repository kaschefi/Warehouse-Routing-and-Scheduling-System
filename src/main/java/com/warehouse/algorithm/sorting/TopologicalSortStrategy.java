package com.warehouse.algorithm.sorting;

import com.warehouse.model.domain.Task;
import java.util.List;

/**
 * Strategy interface for sorting execution orders of dependent warehouse tasks.
 */
public interface TopologicalSortStrategy {
    /**
     * Sorts tasks chronologically based on their prerequisite dependencies.
     * @param tasks The unsorted list of tasks.
     * @return A sorted list of tasks in safe execution order.
     */
    List<Task> sort(List<Task> tasks);
}