package dev.glicio.commands.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import dev.glicio.GPlayer;
import dev.glicio.database.DatabaseHelper;
import net.minecraft.commands.CommandSourceStack;

import net.minecraft.commands.Commands;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.sql.Connection;

public class DebugCommand {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        //has permission 2 -> operator
        dispatcher.register(Commands.literal("debug").requires(source -> source.hasPermission(2)).executes(DebugCommand::execute));
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        Player player = context.getSource().getPlayer();
        if (player == null) {
            return 0;
        }

        DatabaseHelper.Init();

        return 1;
    }
}
