package dev.glicio.events;

import com.mojang.logging.LogUtils;
import dev.glicio.GCraftCore;
import dev.glicio.GPlayer;
import dev.glicio.database.DatabaseHelper;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static dev.glicio.GCraftCore.*;
import static dev.glicio.database.PlayerDb.*;

@EventBusSubscriber(modid = GCraftCore.MODID)
public class ChatEventHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        MinecraftServer server = player.getServer();

        // Send a welcome message to the player
        PlayerList playerList = Objects.requireNonNull(server).getPlayerList();
        String selfMsg = String.format("Bem vindo %s, ao servidor GCraft!", player.getName().getString());
        String broadcastMsg = String.format("%s entrou no servidor!", player.getName().getString());
        for (Player p : playerList.getPlayers()) {
            if (p.getUUID().equals(player.getUUID())) {
                p.sendSystemMessage(Component.literal(broadcastMsg));
                continue;
            }
            p.sendSystemMessage(Component.literal(selfMsg));
        }

        String uuid = player.getUUID().toString();
        String name = player.getName().getString();
        Timestamp now = Timestamp.from(Instant.now());
        Connection con = DatabaseHelper.getConnection();

        try (con) {
            if (con == null) {
                LOGGER.error("Failed to get connection");
                return;
            }
            GPlayer dbPlayer = getDbPlayer(uuid, con);
            if (dbPlayer == null) {
                dbPlayer = new GPlayer(name, uuid, null, now);
                saveDbPlayer(dbPlayer, con);
            }
            String lastLoginMsg = String.format("Bem vindo de volta, %s, seu último login foi em %s", player.getName().getString(), dbPlayer.getLastLogin());
            player.sendSystemMessage(Component.literal(lastLoginMsg));
            player.sendSystemMessage(Component.literal("Você está no chat local, use /g para entrar no chat global e falar com todos os jogadores"));
            updateLastLogin(uuid, con);
            addPlayer(uuid, dbPlayer);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                con.close();
            } catch (Exception e) {
                LOGGER.error("Failed to close connection");
                e.printStackTrace();
            }
        }
    }

    @SubscribeEvent
    public static void onLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        MinecraftServer server = player.getServer();

        PlayerList playerList = Objects.requireNonNull(server).getPlayerList();
        String broadcastMsg = String.format("%s saiu do servidor!", player.getName().getString());
        for (Player p : playerList.getPlayers()) {
            p.sendSystemMessage(Component.literal(broadcastMsg));
        }
        String uuid = player.getUUID().toString();
        removePlayer(uuid);
    }

    @SubscribeEvent
    public static void onMessage(ServerChatEvent event) {
        Player player = event.getPlayer();
        String prefix_name = player.getName().getString();
        String formatedMessage = "";
        User user = luckPermsApi.getUserManager().getUser(player.getUUID());
        assert user != null;
        //get user prefix
        String groupName = Objects.requireNonNull(user.getNodes().stream().filter(NodeType.INHERITANCE::matches).map(NodeType.INHERITANCE::cast).findFirst().orElse(null)).getGroupName();
        Group group = luckPermsApi.getGroupManager().getGroup(groupName);
        assert group != null;
        Node prefix = group.getNodes().stream().filter(NodeType.PREFIX::matches).map(NodeType.PREFIX::cast).findFirst().orElse(null);
        Node color = group.getNodes().stream().filter(node -> node.getKey().startsWith("color")).findFirst().orElse(null);
        String message = event.getMessage().getString();
        MinecraftServer server = player.getServer();
        event.setCanceled(true);
        if (prefix != null) {
            // node is in the format "prefix.{priority}.{prefix}"
            String prefixKey = prefix.getKey().split("\\.")[2];
            if(color != null) {
                // node is in the format "color.{color_number}"
                String colorKey = color.getKey().split("\\.")[1];
                prefix_name = String.format("[§%s%s§r] %s", colorKey, prefixKey, player.getName().getString());
            } else {
                prefix_name = String.format("[%s] %s", prefixKey, player.getName().getString());
            }
        }
        var now = LocalDateTime.now();
        String date = now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

        GPlayer gPlayer = getPlayer(player.getUUID().toString());

        if(gPlayer == null) {
            LOGGER.warn("Player {} not in hash player list", player.getName().getString());
            return;
        }

        PlayerList playerList = Objects.requireNonNull(server).getPlayerList();
        if(gPlayer.getCurrentChat() == 1) {
            formatedMessage = String.format("[G]%s: %s", prefix_name, message);
            LOGGER.info(formatedMessage);
            Component component = Component.literal(formatedMessage);
            for (Player p : playerList.getPlayers()) {
                p.sendSystemMessage(component);
            }
        } else {
            formatedMessage = String.format("[§eL§r]%s: §e%s", prefix_name, message);
            LOGGER.info(formatedMessage);
            Component component = Component.literal(formatedMessage);
            //send message to player within a radius of 100 blocks
            for (Player p : playerList.getPlayers()) {
                if(p.distanceTo(player) < 100) {
                    p.sendSystemMessage(component);
                }
            }
        }

    }
}
