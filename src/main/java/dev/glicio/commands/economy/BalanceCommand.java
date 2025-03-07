package dev.glicio.commands.economy;

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

public class BalanceCommand {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register both 'bal' and 'saldo' commands
        dispatcher.register(Commands.literal("bal")
                .requires(source -> source.hasPermission(0))
                .executes(BalanceCommand::execute));
                
        dispatcher.register(Commands.literal("saldo")
                .requires(source -> source.hasPermission(0))
                .executes(BalanceCommand::execute));
    }
    
    private static int execute(CommandContext<CommandSourceStack> context) {
        Player player = context.getSource().getPlayer();
        if (player == null) {
            LOGGER.error("This command can only be executed as a player");
            return 0;
        }
        
        GPlayer gPlayer = GCraftCore.getPlayer(player.getUUID().toString());
        
        if (gPlayer == null) {
            player.sendSystemMessage(Component.literal("Erro ao buscar seu saldo"));
            return 0;
        }
        
        String message = String.format("Seu saldo atual é: $%s", gPlayer.getFormattedBalance());
        player.sendSystemMessage(Component.literal(message));
        
        return 1;
    }
} 