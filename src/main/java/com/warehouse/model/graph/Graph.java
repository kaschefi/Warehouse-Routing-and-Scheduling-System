package com.warehouse.model.graph;
import java.util.List;

public interface Graph<T> {
    void addVertex(Vertex<T> v);
    void addEdge(Edge<T> e);
    List<Vertex<T>> getVertices();
    List<Edge<T>> getNeighbors(Vertex<T> v);
}
