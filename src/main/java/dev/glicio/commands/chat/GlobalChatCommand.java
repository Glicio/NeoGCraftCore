package dev.glicio.commands.chat;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import dev.glicio.model.GPlayer;
import dev.glicio.service.PlayerService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

public class GlobalChatCommand {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("g")
            .requires(source -> source.hasPermission(0))
            .executes(GlobalChatCommand::execute));
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        Player player = context.getSource().getPlayer();
        if (player == null) {
            LOGGER.error("This command can only be executed as a player");
            return 0;
        }

        GPlayer gPlayer = PlayerService.get(player.getUUID().toString());
        if (gPlayer == null) return 0;

        gPlayer.setCurrentChat(1);
        player.sendSystemMessage(Component.literal("Você entrou no chat global"));
        return 1;
    }
}
