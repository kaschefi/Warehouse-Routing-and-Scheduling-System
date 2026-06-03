package com.warehouse.model.graph;

import java.util.Objects;

public class Vertex<T> {
    private T payload;
    
    public Vertex(T payload) {
        this.payload = payload;
    }
    
    public T getPayload() {
        return payload;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vertex<?> vertex = (Vertex<?>) o;
        return Objects.equals(payload, vertex.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(payload);
    }

    @Override
    public String toString() {
        return "Vertex{" + "payload=" + payload + '}';
    }
}
