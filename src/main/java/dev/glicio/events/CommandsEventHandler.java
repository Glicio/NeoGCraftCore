package dev.glicio.events;

import dev.glicio.GCraftCore;
import dev.glicio.commands.SpawnCommand;
import dev.glicio.commands.admin.SetSpawnCommand;
import dev.glicio.commands.admin.shop.AdminShopCommand;
import dev.glicio.commands.debug.DebugCommand;
import dev.glicio.commands.chat.GlobalChatCommand;
import dev.glicio.commands.chat.LocalChatCommand;
import dev.glicio.commands.economy.BalanceCommand;
import dev.glicio.commands.economy.BalanceTopCommand;
import dev.glicio.commands.economy.PayCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = GCraftCore.MODID)
public class CommandsEventHandler {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        DebugCommand.register(event.getDispatcher());
        GlobalChatCommand.register(event.getDispatcher());
        LocalChatCommand.register(event.getDispatcher());
        SpawnCommand.register(event.getDispatcher());
        SetSpawnCommand.register(event.getDispatcher());
        AdminShopCommand.register(event.getDispatcher());
        BalanceCommand.register(event.getDispatcher());
        BalanceTopCommand.register(event.getDispatcher());
        PayCommand.register(event.getDispatcher());
    }

}
