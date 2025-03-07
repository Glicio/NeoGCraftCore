package dev.glicio.commands.chat;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import dev.glicio.GCraftCore;
import dev.glicio.GPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

public class GlobalChatCommand {

    private static Logger LOGGER = LogUtils.getLogger();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        //has permission 0 -> player
        dispatcher.register(Commands.literal("g").requires(source -> source.hasPermission(0)).executes(GlobalChatCommand::execute));

    }


    private static int execute(CommandContext<CommandSourceStack> context) {
        Player player = context.getSource().getPlayer();
        if (player == null) {
            LOGGER.error("This command can only be executed as a player");
            return 0;
        }

        GPlayer gPlayer = GCraftCore.getPlayer(player.getUUID().toString());

        if(gPlayer == null) {
            gPlayer = new GPlayer(player.getName().getString(), player.getUUID().toString(), null, 0);
            GCraftCore.addPlayer(player.getUUID().toString(), gPlayer);
        }

        gPlayer.setCurrentChat(1);
        Component component = Component.literal("Você entrou no chat global");
        player.sendSystemMessage(component);

        return 1;
    }
}
