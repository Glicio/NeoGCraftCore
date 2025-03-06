package dev.glicio.commands.admin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import dev.glicio.Config;
import dev.glicio.WarpCoord;
import dev.glicio.database.DatabaseHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.sql.Connection;

public class SetSpawnCommand {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        //has permission 2 -> operator
        dispatcher.register(Commands.literal("setspawn").requires(source -> source.hasPermission(2)).executes(SetSpawnCommand::execute));
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        //TODO: implement luck perms permission

        Player player = context.getSource().getPlayer();

        assert player != null;
        Vec3 pos = new Vec3(player.getX(), player.getY(), player.getZ());

        ResourceKey<Level> currentWorld = player.level().dimension();
        String currentWorldName = currentWorld.location().toString();

        Connection con = DatabaseHelper.getConnection();

        try {
            assert con != null;
            String updateSpawnSQL = "UPDATE coords SET world = ?, x = ?, y = ?, z = ? WHERE id = 'spawn'";
            var updateSpawn = con.prepareStatement(updateSpawnSQL);
            updateSpawn.setString(1, currentWorldName);
            updateSpawn.setInt(2, (int) pos.x);
            updateSpawn.setInt(3, (int) pos.y);
            updateSpawn.setInt(4, (int) pos.z);
            updateSpawn.execute();
            Config.spawnCoord = new WarpCoord("spawn", (int) pos.x, (int) pos.y, (int) pos.z, currentWorldName, "Spawn");
            player.sendSystemMessage(Component.literal("Spawn setado com sucesso"));
            LOGGER.info("Spawn alterado por {} para {} nas coordenadas {}, {}, {}", player.getName().getString(), currentWorldName, (int) pos.x, (int) pos.y, (int) pos.z);
            con.close();
            return 1;
        } catch (Exception e) {
            LOGGER.error("Failed to update spawn coordinates in database: {}", e.getMessage());
            return 0;
        }
    }
}
