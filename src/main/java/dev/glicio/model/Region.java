package dev.glicio.model;

import net.minecraft.world.phys.Vec2;

public class Region {
    private String name;
    private Rect2D bounds;
    private int id;
    private String ownerId;
    private String ownerName;

    public static class Rect2D {
        private Vec2 topLeft;
        private Vec2 bottomRight;

        public Rect2D(Vec2 topLeft, Vec2 bottomRight) {
            this.topLeft = topLeft;
            this.bottomRight = bottomRight;
        }

        public Rect2D(Vec2 vector) {
            this.topLeft = vector;
            this.bottomRight = vector;
        }

        public boolean checkCollision(Vec2 point) {
            float minX = Math.min(topLeft.x, bottomRight.x);
            float maxX = Math.max(topLeft.x, bottomRight.x);
            float minY = Math.min(topLeft.y, bottomRight.y);
            float maxY = Math.max(topLeft.y, bottomRight.y);
            return point.x >= minX && point.x <= maxX && point.y >= minY && point.y <= maxY;
        }

        public boolean checkCollision(Rect2D other) {
            float minX1 = Math.min(topLeft.x, bottomRight.x);
            float maxX1 = Math.max(topLeft.x, bottomRight.x);
            float minY1 = Math.min(topLeft.y, bottomRight.y);
            float maxY1 = Math.max(topLeft.y, bottomRight.y);
            float minX2 = Math.min(other.topLeft.x, other.bottomRight.x);
            float maxX2 = Math.max(other.topLeft.x, other.bottomRight.x);
            float minY2 = Math.min(other.topLeft.y, other.bottomRight.y);
            float maxY2 = Math.max(other.topLeft.y, other.bottomRight.y);
            return maxX1 >= minX2 && maxX2 >= minX1 && maxY1 >= minY2 && maxY2 >= minY1;
        }

        public int getArea() {
            float width = Math.abs(topLeft.x - bottomRight.x) + 1;
            float height = Math.abs(topLeft.y - bottomRight.y) + 1;
            return (int) (width * height);
        }

        public Vec2 getTopLeft() { return topLeft; }
        public Vec2 getBottomRight() { return bottomRight; }
        public void setTopLeft(Vec2 topLeft) { this.topLeft = topLeft; }
        public void setBottomRight(Vec2 bottomRight) { this.bottomRight = bottomRight; }
    }

    public Region(String name, Vec2 topLeft, Vec2 bottomRight, String ownerId, String ownerName) {
        this.name = name;
        this.bounds = new Rect2D(topLeft, bottomRight);
        this.ownerId = ownerId;
        this.ownerName = ownerName;
    }

    public Region(String name, Vec2 topLeft, Vec2 bottomRight, String ownerId) {
        this(name, topLeft, bottomRight, ownerId, null);
    }

    public boolean checkCollision(Vec2 point) { return bounds.checkCollision(point); }
    public boolean checkCollision(Region other) { return this.bounds.checkCollision(other.bounds); }
    public int getArea() { return bounds.getArea(); }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Rect2D getBounds() { return bounds; }
    public void setBounds(Rect2D bounds) { this.bounds = bounds; }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
}
