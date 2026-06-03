package com.warehouse.algorithm.spanningtree;
import com.warehouse.model.graph.Edge;
import com.warehouse.model.graph.Graph;
import java.util.List;

public interface MinimumSpanningTreeStrategy<T> {
    List<Edge<T>> findMST(Graph<T> graph);
}
