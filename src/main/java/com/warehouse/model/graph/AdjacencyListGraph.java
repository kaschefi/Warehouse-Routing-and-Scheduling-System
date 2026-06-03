package com.warehouse.model.graph;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdjacencyListGraph<T> implements Graph<T> {
    private Map<Vertex<T>, List<Edge<T>>> adjList;
    
    public AdjacencyListGraph() {
        this.adjList = new HashMap<>();
    }
    
    @Override
    public void addVertex(Vertex<T> v) {
        adjList.putIfAbsent(v, new ArrayList<>());
    }
    
    @Override
    public void addEdge(Edge<T> e) {
        adjList.get(e.getSource()).add(e);
        // Note: For an undirected graph, you may want to add the reverse edge here 
        // or handle it in your service layer when constructing the graph.
    }
    
    @Override
    public List<Vertex<T>> getVertices() {
        return new ArrayList<>(adjList.keySet());
    }
    
    @Override
    public List<Edge<T>> getNeighbors(Vertex<T> v) {
        return adjList.getOrDefault(v, new ArrayList<>());
    }
}
