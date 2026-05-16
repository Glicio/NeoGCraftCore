package dev.glicio.model;

import dev.glicio.GCraftCore;
import net.luckperms.api.model.user.User;

import java.sql.Timestamp;
import java.util.UUID;

public class GPlayer {
    private final String name;
    private final String uuid;
    private final Timestamp lastLogin;
    private boolean creatingAdminShop = false;
    private int currentChat = 0;
    private int balance = 0;
    private volatile boolean dirty = false;
    private boolean selectingRegion = false;
    private Region.Rect2D selection;

    public GPlayer(String name, String uuid, Timestamp lastLogin) {
        this.name = name;
        this.uuid = uuid;
        this.lastLogin = lastLogin;
    }

    public GPlayer(String name, String uuid, Timestamp lastLogin, int balance) {
        this.name = name;
        this.uuid = uuid;
        this.lastLogin = lastLogin;
        this.balance = balance;
    }

    public String getName() { return name; }
    public String getUuid() { return uuid; }

    public String getPrefix() {
        if (GCraftCore.luckPermsApi == null) return "";
        User user = GCraftCore.luckPermsApi.getUserManager().getUser(UUID.fromString(uuid));
        if (user == null) return "";
        String prefix = user.getCachedData().getMetaData().getPrefix();
        return prefix != null ? prefix : "";
    }

    public int getCurrentChat() { return currentChat; }
    public void setCurrentChat(int currentChat) { this.currentChat = currentChat; }

    public String getLastLogin() {
        return new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(lastLogin);
    }

    public boolean isCreatingAdminShop() { return creatingAdminShop; }
    public void setCreatingAdminShop(boolean creatingAdminShop) { this.creatingAdminShop = creatingAdminShop; }

    public synchronized int getBalance() { return balance; }

    public synchronized void setBalance(int balance) {
        this.balance = balance;
        this.dirty = true;
    }

    public synchronized void addBalance(int amount) {
        this.balance += amount;
        this.dirty = true;
    }

    public String getFormattedBalance() {
        return String.format("%.2f", getBalance() / 100.0);
    }

    public boolean isDirty() { return dirty; }
    public void clearDirty() { this.dirty = false; }

    public void setSelectionTopLeft(int x, int y) {
        if (this.selection == null) {
            this.selection = new Region.Rect2D(new net.minecraft.world.phys.Vec2(x, y), new net.minecraft.world.phys.Vec2(x, y));
            return;
        }
        this.selection.setTopLeft(new net.minecraft.world.phys.Vec2(x, y));
    }

    public void setSelectionBottomRight(int x, int y) {
        if (this.selection == null) {
            this.selection = new Region.Rect2D(new net.minecraft.world.phys.Vec2(x, y), new net.minecraft.world.phys.Vec2(x, y));
            return;
        }
        this.selection.setBottomRight(new net.minecraft.world.phys.Vec2(x, y));
    }

    public void clearSelection() { this.selection = null; }
    public Region.Rect2D getSelection() { return this.selection; }
    public void setSelectingRegion(boolean selectingRegion) { this.selectingRegion = selectingRegion; }
    public boolean isSelectingRegion() { return this.selectingRegion; }
}
