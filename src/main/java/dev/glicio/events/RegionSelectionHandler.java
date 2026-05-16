package dev.glicio.events;

import com.mojang.logging.LogUtils;
import dev.glicio.GCraftCore;
import dev.glicio.model.GPlayer;
import dev.glicio.service.PlayerService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.slf4j.Logger;

@EventBusSubscriber(modid = GCraftCore.MODID)
public class RegionSelectionHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        GPlayer playerData = PlayerService.get(player.getUUID().toString());

        if (playerData == null || !playerData.isSelectingRegion()) return;
        if (!player.getMainHandItem().is(Items.GOLDEN_SHOVEL)) return;

        BlockPos pos = event.getPos();

        if (playerData.getSelection() == null || playerData.getSelection().getTopLeft() == null) {
            playerData.setSelectionTopLeft(pos.getX(), pos.getZ());
            player.sendSystemMessage(Component.literal(
                String.format("Primeiro ponto selecionado (X: %d, Z: %d)", pos.getX(), pos.getZ())));
        } else {
            if (pos.getX() == (int) playerData.getSelection().getTopLeft().x &&
                pos.getZ() == (int) playerData.getSelection().getTopLeft().y) {
                player.sendSystemMessage(Component.literal("Selecione a outra extremidade da região"));
                return;
            }
            playerData.setSelectionBottomRight(pos.getX(), pos.getZ());
            player.sendSystemMessage(Component.literal(
                String.format("Segundo ponto selecionado (X: %d, Z: %d)", pos.getX(), pos.getZ())));
            player.sendSystemMessage(Component.literal("Use /r create [nome] para criar a região"));
        }
    }

    @SubscribeEvent
    public static void onItemChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getSlot().getType() != EquipmentSlot.Type.HAND) return;

        GPlayer playerData = PlayerService.get(player.getUUID().toString());
        if (playerData == null || !playerData.isSelectingRegion()) return;

        if (event.getTo().getItem() != Items.GOLDEN_SHOVEL) {
            if (playerData.getSelection() != null) {
                player.sendSystemMessage(Component.literal("Seleção de região abortada"));
                playerData.clearSelection();
            }
        } else if (playerData.getSelection() == null) {
            player.sendSystemMessage(Component.literal("Use a §6pá de ouro§r para selecionar a região"));
        }
    }
}
