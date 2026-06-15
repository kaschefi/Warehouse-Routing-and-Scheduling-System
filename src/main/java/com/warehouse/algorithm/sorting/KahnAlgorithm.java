package com.warehouse.algorithm.sorting;
import com.warehouse.model.domain.Task;
import com.warehouse.model.graph.Graph;

import java.util.List;
import java.util.ArrayList;

public class KahnAlgorithm implements TopologicalSortStrategy {

    @Override
    public List<Task> sort(List<Task> tasks) {
        // TODO: Implement Topological Sort (e.g. Kahn's or DFS) here!
        System.out.println("Executing Topological Sort...");
        return new ArrayList<>(); // Return dummy list for now
    }
}
