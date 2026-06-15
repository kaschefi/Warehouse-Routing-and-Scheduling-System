package com.warehouse.service;

import com.warehouse.algorithm.sorting.TopologicalSortStrategy;
import com.warehouse.model.domain.Task;
import java.util.ArrayList;
import java.util.List;

/**
 * Service handling workflow management and scheduling dependencies for warehouse operations.
 */
public class TaskSchedulingService {
    private final List<Task> tasks;
    private final TopologicalSortStrategy sorter;

    public TaskSchedulingService(TopologicalSortStrategy sorter) {
        this.tasks = new ArrayList<>();
        this.sorter = sorter;
    }

    public void addTask(Task task) {
        if (task != null && !tasks.contains(task)) {
            tasks.add(task);
        }
    }

    /**
     * Computes the chronological roadmap order for execution.
     * @return Sorted list of tasks.
     */
    public List<Task> getExecutionOrder() {
        return sorter.sort(tasks);
    }
}