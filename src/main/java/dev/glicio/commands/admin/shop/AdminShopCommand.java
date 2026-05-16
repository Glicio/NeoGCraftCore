package dev.glicio.commands.admin.shop;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import dev.glicio.GCraftCore;
import dev.glicio.model.GPlayer;
import dev.glicio.service.PlayerService;
import net.luckperms.api.model.user.User;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

public class AdminShopCommand {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("adminshop")
            .requires(source -> {
                if (!(source.getEntity() instanceof Player player)) return false;
                User user = GCraftCore.luckPermsApi.getUserManager().getUser(player.getUUID());
                return user != null && user.getCachedData().getPermissionData()
                    .checkPermission("gcraftcore.shop.admin").asBoolean();
            })
            .executes(AdminShopCommand::execute));
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        Player player = context.getSource().getPlayer();
        if (player == null) return 0;

        GPlayer gPlayer = PlayerService.get(player.getUUID().toString());
        if (gPlayer == null) return 0;

        boolean newState = !gPlayer.isCreatingAdminShop();
        gPlayer.setCreatingAdminShop(newState);

        player.sendSystemMessage(Component.literal(
            newState ? "Modo de criação de loja administrativa ativado"
                     : "Modo de criação de loja administrativa desativado"));
        return 1;
    }
}
