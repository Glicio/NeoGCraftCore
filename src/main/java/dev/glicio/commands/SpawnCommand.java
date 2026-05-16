package dev.glicio.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import dev.glicio.model.WarpCoord;
import dev.glicio.service.WarpService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import java.util.EnumSet;
import java.util.Set;

public class SpawnCommand {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spawn")
            .requires(source -> source.hasPermission(0))
            .executes(SpawnCommand::execute));
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        Player player = context.getSource().getPlayer();
        if (player == null) {
            LOGGER.error("This command can only be executed as a player");
            return 0;
        }

        WarpCoord spawn = WarpService.getSpawn();
        if (spawn == null) {
            player.sendSystemMessage(Component.literal("§cSpawn não configurado"));
            return 0;
        }

        MinecraftServer server = player.getServer();
        if (server == null) return 0;

        String[] parts = spawn.getWorld().split(":");
        var resLocation = ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
        ResourceKey<Level> worldKey = ResourceKey.create(
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath("minecraft", "dimension")),
            resLocation
        );

        ServerLevel dimension = server.getLevel(worldKey);
        if (dimension == null) {
            LOGGER.error("Dimension not found: {}", spawn.getWorld());
            return 0;
        }

        Set<RelativeMovement> relativeMovement = EnumSet.noneOf(RelativeMovement.class);
        player.teleportTo(dimension, spawn.getPos().x, spawn.getPos().y, spawn.getPos().z,
            relativeMovement, player.getYRot(), player.getXRot());
        player.sendSystemMessage(Component.literal("Teleportado para o spawn"));
        return 1;
    }
}
