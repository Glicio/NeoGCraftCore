package dev.glicio.regions;

import net.minecraft.world.phys.Vec2;

public class Region {

    private String name;
    private Rect2D bounds;
    private int id;
    private String ownerId; // UUID of the player who created the region
    private String ownerName; // Name of the player who created the region

    public static class Rect2D {
        private Vec2 TOP_LEFT;
        private Vec2 BOTTOM_RIGHT;

        public Rect2D(Vec2 topLeft, Vec2 bottomRight) {
            this.TOP_LEFT = topLeft;
            this.BOTTOM_RIGHT = bottomRight;
        }

        public Rect2D(Vec2 vector) {
            this.TOP_LEFT = vector;
            this.BOTTOM_RIGHT = vector;
        }

        public boolean checkCollision(Vec2 point) {
            // Find the actual mins and maxes regardless of what the fields are named
            float minX = Math.min(TOP_LEFT.x, BOTTOM_RIGHT.x);
            float maxX = Math.max(TOP_LEFT.x, BOTTOM_RIGHT.x);
            float minY = Math.min(TOP_LEFT.y, BOTTOM_RIGHT.y);
            float maxY = Math.max(TOP_LEFT.y, BOTTOM_RIGHT.y);
            
            // Check if the point is within the bounds
            // In Minecraft, blocks are represented by their corner coordinates
            // So we need to do an inclusive check to properly include border blocks
            return point.x >= minX && point.x <= maxX && point.y >= minY && point.y <= maxY;
        }

        /**
         * Checks if this rectangle collides with another rectangle
         * @param other The other rectangle to check collision with
         * @return true if the rectangles overlap, false otherwise
         */
        public boolean checkCollision(Rect2D other) {
            // Find mins and maxes for this rectangle
            float minX1 = Math.min(TOP_LEFT.x, BOTTOM_RIGHT.x);
            float maxX1 = Math.max(TOP_LEFT.x, BOTTOM_RIGHT.x);
            float minY1 = Math.min(TOP_LEFT.y, BOTTOM_RIGHT.y);
            float maxY1 = Math.max(TOP_LEFT.y, BOTTOM_RIGHT.y);
            
            // Find mins and maxes for the other rectangle
            float minX2 = Math.min(other.TOP_LEFT.x, other.BOTTOM_RIGHT.x);
            float maxX2 = Math.max(other.TOP_LEFT.x, other.BOTTOM_RIGHT.x);
            float minY2 = Math.min(other.TOP_LEFT.y, other.BOTTOM_RIGHT.y);
            float maxY2 = Math.max(other.TOP_LEFT.y, other.BOTTOM_RIGHT.y);
            
            // Check for intersection - if the rectangles overlap on both axes
            // Using inclusive comparison (<=, >=) to properly include border blocks
            boolean overlapX = maxX1 >= minX2 && maxX2 >= minX1;
            boolean overlapY = maxY1 >= minY2 && maxY2 >= minY1;
            
            return overlapX && overlapY;
        }

        public Vec2 getTopLeft() {
            return TOP_LEFT;
        }

        public Vec2 getBottomRight() {
            return BOTTOM_RIGHT;
        }

        public void setTopLeft(Vec2 topLeft) {
            TOP_LEFT = topLeft;
        }

        public void setBottomRight(Vec2 bottomRight) {
            BOTTOM_RIGHT = bottomRight;
        }
        
        /**
         * Calculates the area of this rectangle in square blocks
         * @return The area in blocks (square units)
         */
        public int getArea() {
            // Use min/max to ensure correct calculation regardless of corner positions
            float minX = Math.min(TOP_LEFT.x, BOTTOM_RIGHT.x);
            float maxX = Math.max(TOP_LEFT.x, BOTTOM_RIGHT.x);
            float minY = Math.min(TOP_LEFT.y, BOTTOM_RIGHT.y);
            float maxY = Math.max(TOP_LEFT.y, BOTTOM_RIGHT.y);
            
            // Calculate width and height
            float width = maxX - minX + 1; // +1 because blocks are inclusive
            float height = maxY - minY + 1; // +1 because blocks are inclusive
            
            // Return the area (cast to int since we're dealing with blocks)
            return (int)(width * height);
        }
    }

    /**
     * Create a new region with the given name and bounds
     * @param name The name of the region
     * @param topLeft The top-left corner of the region
     * @param bottomRight The bottom-right corner of the region
     * @param ownerId The UUID of the player who created the region
     * @param ownerName The name of the player who created the region
     */
    public Region(String name, Vec2 topLeft, Vec2 bottomRight, String ownerId, String ownerName) {
        this.name = name;
        this.bounds = new Rect2D(topLeft, bottomRight);
        this.ownerId = ownerId;
        this.ownerName = ownerName;
    }
    
    /**
     * Create a new region with the given name and bounds (with owner uuid only)
     * @param name The name of the region
     * @param topLeft The top-left corner of the region
     * @param bottomRight The bottom-right corner of the region
     * @param ownerId The UUID of the player who created the region
     */
    public Region(String name, Vec2 topLeft, Vec2 bottomRight, String ownerId) {
        this(name, topLeft, bottomRight, ownerId, null);
    }
    
    /**
     * Create a new region with the given name and bounds (legacy constructor)
     * @param name The name of the region
     * @param topLeft The top-left corner of the region
     * @param bottomRight The bottom-right corner of the region
     */
    public Region(String name, Vec2 topLeft, Vec2 bottomRight) {
        this(name, topLeft, bottomRight, null, null);
    }

    public boolean checkCollision(Vec2 point) {
        return bounds.checkCollision(point);
    }

    /**
     * Checks if this region collides with another region
     * @param other The other region to check collision with
     * @return true if the regions overlap, false otherwise
     */
    public boolean checkCollision(Region other) {
        return this.bounds.checkCollision(other.bounds);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Rect2D getBounds() {
        return this.bounds;
    }

    public void setBounds(Rect2D bounds) {
        this.bounds = bounds;
    }


    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
    
    /**
     * Get the UUID of the player who created this region
     * @return The player's UUID as a string
     */
    public String getOwnerId() {
        return ownerId;
    }
    
    /**
     * Set the UUID of the player who created this region
     * @param ownerId The player's UUID as a string
     */
    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }
    
    /**
     * Get the name of the player who created this region
     * @return The player's name
     */
    public String getOwnerName() {
        return ownerName;
    }
    
    /**
     * Set the name of the player who created this region
     * @param ownerName The player's name
     */
    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }
    
    /**
     * Calculates the area of this region in square blocks
     * @return The area in blocks (square units)
     */
    public int getArea() {
        return this.bounds.getArea();
    }

}
