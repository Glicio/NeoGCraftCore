package dev.glicio.events;

import com.mojang.logging.LogUtils;
import dev.glicio.GCraftCore;
import dev.glicio.model.GPlayer;
import dev.glicio.model.ShopSign;
import dev.glicio.service.PlayerService;
import dev.glicio.service.ShopService;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.slf4j.Logger;

@EventBusSubscriber(modid = GCraftCore.MODID)
public class ShopInteractEventHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        var pos = event.getPos();
        var player = event.getEntity();
        var block = player.level().getBlockEntity(pos);

        if (!(block instanceof SignBlockEntity)) return;

        Vec3 signPos = new Vec3(pos.getX(), pos.getY(), pos.getZ());
        ShopSign shop = ShopService.get(signPos);

        if (shop == null) {
            GPlayer gPlayer = PlayerService.get(player.getUUID().toString());
            boolean isAdminShop = gPlayer != null && gPlayer.isCreatingAdminShop();
            try {
                ShopSign newShop = new ShopSign((SignBlockEntity) block, isAdminShop, player.getUUID().toString());
                event.setCanceled(true);
                ShopService.save(newShop).thenRun(() ->
                    player.sendSystemMessage(Component.literal(isAdminShop ? "Loja administrativa criada com sucesso" : "Loja criada com sucesso"))
                ).exceptionally(e -> {
                    if (!(e.getCause() instanceof IllegalArgumentException iae) || !iae.getMessage().equals("NOT_SHOP")) {
                        player.sendSystemMessage(Component.literal(e.getCause() != null ? e.getCause().getMessage() : "Erro ao criar loja"));
                    }
                    return null;
                });
            } catch (IllegalArgumentException e) {
                if (!e.getMessage().equals("NOT_SHOP")) {
                    player.sendSystemMessage(Component.literal(e.getMessage()));
                }
            }
        } else {
            event.setCanceled(true);
            if (shop.getShopType() == ShopSign.ShopType.Comprar) {
                ShopService.buyFromShop(shop, player).exceptionally(e -> {
                    LOGGER.error("Buy from shop failed: {}", e.getMessage());
                    return false;
                });
            } else {
                ShopService.sellToShop(shop, player).exceptionally(e -> {
                    LOGGER.error("Sell to shop failed: {}", e.getMessage());
                    return false;
                });
            }
        }
    }
}
