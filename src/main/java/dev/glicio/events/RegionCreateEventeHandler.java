package dev.glicio.events;


import net.minecraft.world.entity.EquipmentSlot;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.glicio.GCraftCore;
import dev.glicio.GPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@EventBusSubscriber(modid = GCraftCore.MODID)
public class RegionCreateEventeHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onRegionCreate(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        BlockPos pos = event.getPos();

        GPlayer playerData = GCraftCore.getPlayer(player.getUUID().toString());

        if (!playerData.isSelectingRegion()) {
            return;
        }

        // Get the item in the player's main hand
        var itemInHand = player.getMainHandItem();

        // Check if the item is a golden shovel
        if (!itemInHand.isEmpty() && itemInHand.is(net.minecraft.world.item.Items.GOLDEN_SHOVEL)) {
            // The player is holding a golden shovel
            // Add your region creation logic here

            if (playerData.getSelection() == null || playerData.getSelection().getTopLeft() == null) {
                playerData.setSelectionTopLeft(pos.getX(), pos.getZ());
                String confirmMessage = String.format("Primeiro ponto selecionado (X: %d, Z: %d)", pos.getX(), pos.getZ());
                player.sendSystemMessage(Component.literal(confirmMessage));
            } else {
                if (pos.getX() == playerData.getSelection().getTopLeft().x && pos.getZ() == playerData.getSelection().getTopLeft().y) {
                    player.sendSystemMessage(Component.literal("Selecione a outra extremidade da região"));
                    return;
                }
                playerData.setSelectionBottomRight(pos.getX(), pos.getZ());
                String confirmMessage = String.format("Segundo ponto selecionado (X: %d, Z: %d)", pos.getX(), pos.getZ());
                player.sendSystemMessage(Component.literal(confirmMessage));
                String commandMessage = "Use o comando /r criar [nome] para criar a região";
                player.sendSystemMessage(Component.literal(commandMessage));
            }
        } else {
            var item = itemInHand.getItem();
            if (item == null) {
                LOGGER.error("Item in hand is null");
                return;
            }
            String itemName = item.getDescriptionId();
            LOGGER.info("Item in hand is: {}", itemName);

        }
        return;

    }

    @SubscribeEvent
    public static void onItemChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof Player player) {
            GPlayer playerData = GCraftCore.getPlayer(player.getUUID().toString());
            if (!playerData.isSelectingRegion()) {
                return;
            }
            if (event.getSlot().getType() == EquipmentSlot.Type.HAND) {
                // Item in hand has changed
                // You can check if it's a golden shovel here
                if (event.getTo().getItem() != Items.GOLDEN_SHOVEL) {
                    if (playerData.getSelection() != null) {
                        player.sendSystemMessage(Component.literal("Seleção de região abortada"));
                        playerData.clearSelection();
                    }
                } else {
                    if(playerData.getSelection() == null) {
                        player.sendSystemMessage(Component.literal("Use a §6pá de ouro§r para selecionar a região"));
                    }
                }
            }
        }
    }
}
