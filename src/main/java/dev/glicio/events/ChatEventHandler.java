package dev.glicio.events;

import com.mojang.logging.LogUtils;
import dev.glicio.GCraftCore;
import dev.glicio.model.GPlayer;
import dev.glicio.service.PlayerService;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@EventBusSubscriber(modid = GCraftCore.MODID)
public class ChatEventHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onMessage(ServerChatEvent event) {
        Player player = event.getPlayer();
        GPlayer gPlayer = PlayerService.get(player.getUUID().toString());

        if (gPlayer == null) {
            LOGGER.warn("Player {} not found in player map", player.getName().getString());
            return;
        }

        User user = GCraftCore.luckPermsApi.getUserManager().getUser(player.getUUID());
        if (user == null) return;

        String prefixedName = player.getName().getString();
        String groupName = Objects.requireNonNull(
            user.getNodes().stream().filter(NodeType.INHERITANCE::matches)
                .map(NodeType.INHERITANCE::cast).findFirst().orElse(null)
        ).getGroupName();

        Group group = GCraftCore.luckPermsApi.getGroupManager().getGroup(groupName);
        if (group != null) {
            Node prefix = group.getNodes().stream().filter(NodeType.PREFIX::matches).map(NodeType.PREFIX::cast).findFirst().orElse(null);
            Node color = group.getNodes().stream().filter(n -> n.getKey().startsWith("color")).findFirst().orElse(null);
            if (prefix != null) {
                String prefixKey = prefix.getKey().split("\\.")[2];
                prefixedName = color != null
                    ? String.format("[§%s%s§r] %s", color.getKey().split("\\.")[1], prefixKey, player.getName().getString())
                    : String.format("[%s] %s", prefixKey, player.getName().getString());
            }
        }

        event.setCanceled(true);
        String message = event.getMessage().getString();
        MinecraftServer server = player.getServer();
        if (server == null) return;

        String formatted;
        if (gPlayer.getCurrentChat() == 1) {
            formatted = String.format("[G]%s: %s", prefixedName, message);
            LOGGER.info(formatted);
            Component component = Component.literal(formatted);
            server.getPlayerList().getPlayers().forEach(p -> p.sendSystemMessage(component));
        } else {
            formatted = String.format("[§eL§r]%s: §e%s", prefixedName, message);
            LOGGER.info(formatted);
            Component component = Component.literal(formatted);
            server.getPlayerList().getPlayers().stream()
                .filter(p -> p.distanceTo(player) < 100)
                .forEach(p -> p.sendSystemMessage(component));
        }
    }
}
