package com.warehouse.service;
import com.warehouse.algorithm.spanningtree.MinimumSpanningTreeStrategy;
import com.warehouse.model.graph.Edge;
import com.warehouse.model.graph.Graph;
import java.util.List;

public class NetworkDesignService {
    private Graph<Location> warehouseMap;
    private MinimumSpanningTreeStrategy<Location> mstStrategy;
    
    public NetworkDesignService(Graph<Location> warehouseMap, MinimumSpanningTreeStrategy<Location> mstStrategy) {
        this.warehouseMap = warehouseMap;
        this.mstStrategy = mstStrategy;
    }
    
    public List<Edge<Location>> generateOptimalNetwork() {
        return mstStrategy.findMST(warehouseMap);
    }
}
