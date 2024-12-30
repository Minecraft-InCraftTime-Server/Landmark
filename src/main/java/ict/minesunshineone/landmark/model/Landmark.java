package ict.minesunshineone.landmark.model;

import java.util.UUID;

import org.bukkit.Location;

public class Landmark {

    private String name;
    private Location location;
    private String description;
    private UUID displayEntityId;
    private UUID interactionEntityId;
    private int order;

    public Landmark(String name, Location location, String description) {
        this.name = name;
        this.location = location;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getDisplayEntityId() {
        return displayEntityId;
    }

    public void setDisplayEntityId(UUID displayEntityId) {
        this.displayEntityId = displayEntityId;
    }

    public UUID getInteractionEntityId() {
        return interactionEntityId;
    }

    public void setInteractionEntityId(UUID interactionEntityId) {
        this.interactionEntityId = interactionEntityId;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
