package com.warehouse.algorithm.sorting;
import com.warehouse.model.graph.Graph;
import com.warehouse.model.graph.Vertex;
import java.util.List;

public interface TopologicalSortStrategy<T> {
    List<Vertex<T>> sort(Graph<T> graph);
}
