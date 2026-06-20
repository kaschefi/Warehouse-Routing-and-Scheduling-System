package com.warehouse.service;

import com.warehouse.algorithm.spanningtree.MinimumSpanningTreeStrategy;
import com.warehouse.model.graph.Edge;
import com.warehouse.model.graph.Graph;
import java.util.List;

/**
 * Service managing infrastructure optimizations and network planning tasks.
 */
public class NetworkDesignService {
    private final Graph warehouseMap;
    private final MinimumSpanningTreeStrategy mstStrategy;

    public NetworkDesignService(Graph warehouseMap, MinimumSpanningTreeStrategy mstStrategy) {
        this.warehouseMap = warehouseMap;
        this.mstStrategy = mstStrategy;
    }

    public List<Edge> generateOptimalNetwork() {
        return mstStrategy.findMST(warehouseMap);
    }
}