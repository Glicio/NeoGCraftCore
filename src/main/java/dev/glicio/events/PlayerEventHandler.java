package dev.glicio.events;

import com.mojang.logging.LogUtils;
import dev.glicio.GCraftCore;
import dev.glicio.database.DatabaseHelper;
import dev.glicio.database.PlayerDao;
import dev.glicio.model.GPlayer;
import dev.glicio.service.PlayerService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

@EventBusSubscriber(modid = GCraftCore.MODID)
public class PlayerEventHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        String uuid = player.getUUID().toString();
        String name = player.getName().getString();

        try (Connection con = DatabaseHelper.getConnection()) {
            GPlayer gPlayer = PlayerDao.getDbPlayer(uuid, con);
            if (gPlayer == null) {
                gPlayer = new GPlayer(name, uuid, Timestamp.from(Instant.now()), 0);
                PlayerDao.saveDbPlayer(gPlayer, con);
            }
            PlayerDao.updateLastLogin(uuid, con);
            PlayerService.add(uuid, gPlayer);

            MinecraftServer server = player.getServer();
            if (server != null) {
                String joinMsg = name + " entrou no servidor!";
                server.getPlayerList().getPlayers().forEach(p -> p.sendSystemMessage(Component.literal(joinMsg)));
            }

            player.sendSystemMessage(Component.literal("Bem vindo de volta, " + name + "! Último login: " + gPlayer.getLastLogin()));
            player.sendSystemMessage(Component.literal("Você está no chat local. Use /g para o chat global."));
            player.sendSystemMessage(Component.literal("Seu saldo: $" + gPlayer.getFormattedBalance()));
        } catch (SQLException e) {
            LOGGER.error("Database error on player join {}: {}", name, e.getMessage());
            player.sendSystemMessage(Component.literal("§cErro ao carregar seus dados. Tente novamente mais tarde."));
        }
    }

    @SubscribeEvent
    public static void onLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        MinecraftServer server = player.getServer();
        if (server != null) {
            String leaveMsg = player.getName().getString() + " saiu do servidor!";
            server.getPlayerList().getPlayers().forEach(p -> p.sendSystemMessage(Component.literal(leaveMsg)));
        }
        PlayerService.remove(player.getUUID().toString());
    }
}
