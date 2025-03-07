package dev.glicio.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import dev.glicio.Config;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.EnumSet;
import java.util.Set;

public class SpawnCommand {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        //has permission 0 -> player
        dispatcher.register(Commands.literal("spawn").requires(source -> source.hasPermission(0)).executes(SpawnCommand::execute));
    }
    private static int execute(CommandContext<CommandSourceStack> context) {
        Player player = context.getSource().getPlayer();

        if (player == null) {
            LOGGER.error("This command can only be executed as a player");
            return 0;
        }

        // Retrieve position and world from the config
        Vec3 pos = Config.spawnCoord.getPos();

        String worldName = Config.spawnCoord.getWorld();

        // Get the server instance
        MinecraftServer server = player.getServer();

        if (server == null) {
            LOGGER.error("Failed to retrieve the server instance");
            return 0;
        }

        String wNamespace = worldName.split(":")[0];
        String wPath = worldName.split(":")[1];
        var resLocation = ResourceLocation.fromNamespaceAndPath(wNamespace, wPath);
        var locationType = ResourceLocation.fromNamespaceAndPath("minecraft", "dimension");
        ResourceKey<Level> worldKey = ResourceKey.create(ResourceKey.createRegistryKey(locationType), resLocation);


        // Fetch the dimension directly from the server's level map
        ServerLevel dimension = server.getLevel(worldKey);
        if (dimension == null) {
            LOGGER.error("Dimension not found: {}", worldName);
            return 0;
        }

        // Prepare movement set for teleportation
        Set<RelativeMovement> movementSet = EnumSet.noneOf(RelativeMovement.class);
        movementSet.add(RelativeMovement.X);
        movementSet.add(RelativeMovement.Y);
        movementSet.add(RelativeMovement.Z);

        // Teleport the player
        player.teleportTo(dimension, pos.x, pos.y, pos.z, movementSet, player.getYRot(), player.getXRot());

        player.sendSystemMessage(Component.literal("Teleportado para o spawn"));
        return 1;
    }


}
