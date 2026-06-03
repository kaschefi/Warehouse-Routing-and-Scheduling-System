package com.warehouse.service;
import com.warehouse.algorithm.pathfinding.ShortestPathStrategy;
import com.warehouse.model.domain.Location;
import com.warehouse.model.graph.Edge;
import com.warehouse.model.graph.Graph;
import com.warehouse.model.graph.Vertex;
import java.util.List;

public class RoutingService {
    private Graph<Location> warehouseMap;
    private ShortestPathStrategy<Location> pathFinder;
    
    public RoutingService(Graph<Location> warehouseMap, ShortestPathStrategy<Location> pathFinder) {
        this.warehouseMap = warehouseMap;
        this.pathFinder = pathFinder;
    }
    
    public List<Edge<Location>> calculateRoute(Vertex<Location> start, Vertex<Location> end) {
        return pathFinder.findShortestPath(warehouseMap, start, end);
    }
}
