package dev.glicio.commands.admin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import dev.glicio.model.WarpCoord;
import dev.glicio.service.WarpService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import static dev.glicio.GCraftCore.luckPermsApi;

public class SetSpawnCommand {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("setspawn")
            .requires(source -> {
                if (!(source.getEntity() instanceof Player player)) return false;
                var user = luckPermsApi.getUserManager().getUser(player.getUUID());
                return user != null && user.getCachedData().getPermissionData()
                    .checkPermission("gcraftcore.admin.setspawn").asBoolean();
            })
            .executes(SetSpawnCommand::execute));
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        Player player = context.getSource().getPlayer();
        if (player == null) return 0;

        Vec3 pos = new Vec3(player.getX(), player.getY(), player.getZ());
        ResourceKey<Level> dimension = player.level().dimension();
        String world = dimension.location().toString();

        WarpCoord newSpawn = new WarpCoord("spawn", (int) pos.x, (int) pos.y, (int) pos.z, world, "Spawn");
        WarpService.setSpawn(newSpawn);

        player.sendSystemMessage(Component.literal("Spawn atualizado com sucesso"));
        LOGGER.info("Spawn updated by {} to {} at ({}, {}, {})",
            player.getName().getString(), world, (int) pos.x, (int) pos.y, (int) pos.z);
        return 1;
    }
}
