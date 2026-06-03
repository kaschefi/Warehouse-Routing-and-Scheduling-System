package com.warehouse.service;
import com.warehouse.algorithm.sorting.TopologicalSortStrategy;
import com.warehouse.model.domain.Task;
import com.warehouse.model.graph.Graph;
import com.warehouse.model.graph.Vertex;
import java.util.List;

public class TaskSchedulingService {
    private Graph<Task> taskDependencies;
    private TopologicalSortStrategy<Task> sorter;
    
    public TaskSchedulingService(Graph<Task> taskDependencies, TopologicalSortStrategy<Task> sorter) {
        this.taskDependencies = taskDependencies;
        this.sorter = sorter;
    }
    
    public List<Vertex<Task>> getExecutionOrder() {
        return sorter.sort(taskDependencies);
    }
}
