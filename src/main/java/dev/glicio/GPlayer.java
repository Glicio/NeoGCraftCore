package dev.glicio;

import java.sql.Timestamp;

public class GPlayer {
    private String name;
    private String uuid;
    private String prefix;
    private Timestamp lastLogin;
    private int currentChat = 0;

    public GPlayer(String name, String uuid, String prefix, Timestamp lastLogin) {
        this.name = name;
        this.uuid = uuid;
        this.prefix = prefix;
        this.lastLogin = lastLogin;
    }

    public String getName() {
        return name;
    }

    public String getUuid() {
        return uuid;
    }

    public String getPrefix() {
        return prefix;
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
}
