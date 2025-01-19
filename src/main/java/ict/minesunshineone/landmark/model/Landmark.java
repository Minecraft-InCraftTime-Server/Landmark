package ict.minesunshineone.landmark.model;

import java.util.UUID;

import org.bukkit.Location;

public class Landmark {

    private String name;
    private Location location;
    private String description;
    private UUID displayEntityId;
    private int menuRow;    // 在菜单中的行位置
    private int menuColumn; // 在菜单中的列位置

    public Landmark(String name, Location location, String description) {
        this.name = name;
        this.location = location;
        this.description = description;
        this.menuRow = 1;    // 默认从第二行开始（索引1）
        this.menuColumn = 1; // 默认从第一列开始（索引1）
    }

    public Landmark(String name, Location location, String description, int menuRow, int menuColumn) {
        this.name = name;
        this.location = location;
        this.description = description;
        this.menuRow = menuRow;
        this.menuColumn = menuColumn;
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

    public int getMenuRow() {
        return menuRow;
    }

    public void setMenuRow(int menuRow) {
        this.menuRow = menuRow;
    }

    public int getMenuColumn() {
        return menuColumn;
    }

    public void setMenuColumn(int menuColumn) {
        this.menuColumn = menuColumn;
    }
}
