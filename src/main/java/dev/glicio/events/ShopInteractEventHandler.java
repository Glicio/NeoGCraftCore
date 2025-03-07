package dev.glicio.events;

import com.mojang.logging.LogUtils;
import dev.glicio.GCraftCore;
import dev.glicio.GPlayer;
import dev.glicio.blocks.ShopSign;
import dev.glicio.shop.ShopHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = GCraftCore.MODID)
public class ShopInteractEventHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onShopCreation(PlayerInteractEvent.RightClickBlock event) {
        var targetPos = event.getPos();
        var player = event.getEntity();
        var level = event.getEntity().level();
        var block = level.getBlockEntity(targetPos);
        Vec3 pos = new Vec3(targetPos.getX(), targetPos.getY(), targetPos.getZ());
        if (block != null) {
            if (block instanceof SignBlockEntity) {
                var shop = ShopHandler.getShop(pos);
                if (shop == null) {
                    try {
                        // Get the GPlayer to check if they're creating an admin shop
                        GPlayer gPlayer = GCraftCore.getPlayer(player.getUUID().toString());
                        boolean isAdminShop = gPlayer != null && gPlayer.isCreating_admin_shop();
                        
                        ShopSign shopSign = new ShopSign((SignBlockEntity) block, isAdminShop, player.getUUID().toString());
                        event.setCanceled(true);
                        ShopHandler.SaveShop(shopSign).thenRun(() -> {
                            String message = isAdminShop ? "Loja administrativa criada com sucesso" : "Loja criada com sucesso";
                            player.sendSystemMessage(Component.literal(message));
                        }).exceptionally(throwable -> {
                            LOGGER.error("Failed to create shop: {}", throwable.getMessage());
                            if (throwable instanceof IllegalArgumentException) {
                                if (throwable.getMessage().equals("NOT_SHOP")) {
                                    return null;
                                }
                                player.sendSystemMessage(Component.literal(throwable.getMessage()));
                                return null;
                            }
                            return null;
                        });
                    } catch (Exception e) {
                        LOGGER.error("Failed to create shop: {}", e.getMessage());
                        if (e instanceof IllegalArgumentException) {
                            if (e.getMessage().equals("NOT_SHOP")) {
                                return;
                            }
                            player.sendSystemMessage(Component.literal(e.getMessage()));
                            return;
                        }
                        return;
                    }
                } else {
                    event.setCanceled(true);
                    if (shop.getShopType() == ShopSign.ShopType.Comprar) {
                        ShopHandler.buyFromShop(shop, player).thenAccept(success -> {
                            if (!success) {
                                LOGGER.error("Failed to buy from shop");
                            }
                        }).exceptionally(throwable -> {
                            LOGGER.error("Failed to buy from shop: {}", throwable.getMessage());
                            return null;
                        });
                    } else if (shop.getShopType() == ShopSign.ShopType.Vender) {
                        ShopHandler.sellToShop(shop, player).thenAccept(success -> {
                            if (!success) {
                                LOGGER.error("Failed to sell to shop");
                            }
                        }).exceptionally(throwable -> {
                            LOGGER.error("Failed to sell to shop: {}", throwable.getMessage());
                            return null;
                        });
                    }
                }
            } else {
                LOGGER.info("Block entity is not a sign");
            }
        }
    }
}
