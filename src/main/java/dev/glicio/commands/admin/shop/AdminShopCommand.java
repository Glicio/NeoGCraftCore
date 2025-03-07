package dev.glicio.commands.admin.shop;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import dev.glicio.GCraftCore;
import dev.glicio.GPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.luckperms.api.model.user.User;
import org.slf4j.Logger;

import java.util.UUID;

public class AdminShopCommand {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register the command with permission check for gcraftcore.shop.admin
        dispatcher.register(Commands.literal("adminshop")
                .requires(source -> {
                    if (!(source.getEntity() instanceof Player player)) {
                        return false;
                    }
                    
                    // Check permission using LuckPerms
                    User user = GCraftCore.luckPermsApi.getUserManager().getUser(player.getUUID());
                    return user != null && user.getCachedData().getPermissionData().checkPermission("gcraftcore.shop.admin").asBoolean();
                })
                .executes(AdminShopCommand::execute));
    }
    
    private static int execute(CommandContext<CommandSourceStack> context) {
        Player player = context.getSource().getPlayer();
        if (player == null) {
            LOGGER.error("This command can only be executed as a player");
            return 0;
        }
        
        GPlayer gPlayer = GCraftCore.getPlayer(player.getUUID().toString());
        
        if (gPlayer == null) {
            gPlayer = new GPlayer(player.getName().getString(), player.getUUID().toString(), null, 0);
            GCraftCore.addPlayer(player.getUUID().toString(), gPlayer);
        }
        
        // Toggle the creating_admin_shop flag
        boolean newState = !gPlayer.isCreating_admin_shop();
        gPlayer.setCreating_admin_shop(newState);
        
        // Send feedback to the player
        String message = newState 
                ? "Modo de criação de loja administrativa ativado" 
                : "Modo de criação de loja administrativa desativado";
        Component component = Component.literal(message);
        player.sendSystemMessage(component);
        
        return 1;
    }
}
