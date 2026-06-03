package com.warehouse.model.domain;

public class Location {
    private String id;
    private String name;
    
    public Location(String id, String name) {
        this.id = id;
        this.name = name;
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    
    @Override
    public String toString() { return name; }
}
