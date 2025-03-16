package dev.glicio;

import java.sql.Timestamp;
import java.util.UUID;

import dev.glicio.regions.Region;
import net.luckperms.api.model.user.User;
import net.minecraft.world.phys.Vec2;

public class GPlayer {
    private final String name;
    private final String uuid;
    private final Timestamp lastLogin;
    private boolean creating_admin_shop = false;
    private int currentChat = 0;
    private int balance = 0;
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

    public String getName() {
        return name;
    }

    public String getUuid() {
        return uuid;
    }

    /**
     * Get the player's prefix from LuckPerms
     * @return The player's prefix or an empty string if not found
     */
    public String getPrefix() {
        if (GCraftCore.luckPermsApi == null) {
            return "";
        }
        
        User user = GCraftCore.luckPermsApi.getUserManager().getUser(UUID.fromString(uuid));
        if (user == null) {
            return "";
        }
        
        String prefix = user.getCachedData().getMetaData().getPrefix();
        return prefix != null ? prefix : "";
    }

    public int getCurrentChat() {
        return currentChat;
    }

    public void setCurrentChat(int currentChat) {
        this.currentChat = currentChat;
    }

    //formated last login in DD/MM/YYYY HH:mm:ss
    public String getLastLogin() {
        return new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(lastLogin);
    }

    public boolean isCreating_admin_shop() {
        return creating_admin_shop;
    }

    public void setCreating_admin_shop(boolean creating_admin_shop) {
        this.creating_admin_shop = creating_admin_shop;
    }
    
    public int getBalance() {
        return balance;
    }
    
    public void setBalance(int balance) {
        this.balance = balance;
    }
    
    /**
     * Get the formatted balance as a string with decimal point (e.g. "10.50")
     * @return Formatted balance string
     */
    public String getFormattedBalance() {
        return String.format("%.2f", balance / 100.0);
    }
    
    /**
     * Add amount to player's balance
     * @param amount Amount to add (can be negative to subtract)
     */
    public void addBalance(int amount) {
        this.balance += amount;
    }

    public void setSelectionTopLeft(int x, int y) {
        if(this.selection == null) {
            this.selection = new Region.Rect2D(new Vec2(x, y), new Vec2(x, y));
            return;
        }
        this.selection.setTopLeft(new Vec2(x, y));
    }


    public void setSelectionBottomRight(int x, int y) {
        if(this.selection == null) {
            this.selection = new Region.Rect2D(new Vec2(x, y), new Vec2(x, y));
            return;
        }
        this.selection.setBottomRight(new Vec2(x, y));
    }

    public void clearSelection() {
        this.selection = null;
    }

    public Region.Rect2D getSelection() {
        return this.selection;
    }

    public void setSelectingRegion(boolean selectingRegion) {
        this.selectingRegion = selectingRegion;
    }

    public boolean isSelectingRegion() {
        return this.selectingRegion;
    }

}
