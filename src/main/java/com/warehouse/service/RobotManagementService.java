package com.warehouse.service;

import com.warehouse.model.domain.Node;
import com.warehouse.model.domain.Robot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service managing robot fleet registration, tracking, and lifetime actions.
 */
public class RobotManagementService {
    private final Map<String, Robot> robotFleet = new HashMap<>();
    private int robotCounter = 1;

    /**
     * Instantiates a new Robot asset at a target node location and saves it to the fleet map.
     * @param placementNode The target Node where the robot is initialised.
     * @return The registered Robot.
     */
    public Robot registerRobot(Node placementNode) {
        String id = "R" + robotCounter;
        String name = "Robot " + robotCounter;
        Robot robot = new Robot(id, name, placementNode);
        robotFleet.put(id, robot);
        robotCounter++;
        System.out.println("Registered robot: " + robot);
        return robot;
    }

    /**
     * Clears all registered robots and resets the ID auto-increment counter.
     */
    public void clearFleet() {
        robotFleet.clear();
        robotCounter = 1;
        System.out.println("Cleared robot fleet tracking database.");
    }

    /**
     * Retrieves all current robot fleet tracking records.
     * @return A list of all active Robots.
     */
    public List<Robot> getActiveFleet() {
        return new ArrayList<>(robotFleet.values());
    }
}
