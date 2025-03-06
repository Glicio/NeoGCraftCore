package dev.glicio;

import net.minecraft.world.phys.Vec3;

public class WarpCoord {
    private final String id;
    private final int x;
    private final int y;
    private final int z;
    private final String world;
    private final String friendlyName;

    public WarpCoord(String id, int x, int y, int z, String world, String friendlyName) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.world = world;
        this.friendlyName = friendlyName;
    }

    public String getId() {
        return id;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public String getWorld() {
        return world;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public Vec3 getPos() {
        return new Vec3(x, y, z);
    }
}
