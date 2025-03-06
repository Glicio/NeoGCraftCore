package dev.glicio.events;

import com.mojang.logging.LogUtils;
import dev.glicio.GCraftCore;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.slf4j.Logger;

@EventBusSubscriber(modid = GCraftCore.MODID)
public class ShopCreationHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onShopCreation(PlayerInteractEvent.RightClickBlock event) {
        var targetPos = event.getPos();
        var level = event.getEntity().level();
        var block = level.getBlockEntity(targetPos);
        if (block != null) {
            if(block instanceof SignBlockEntity) {
                var frontText = ((SignBlockEntity) block).getFrontText();
                var backText = ((SignBlockEntity) block).getBackText();
                LOGGER.info("Front text: {} {} {} {}", frontText.getMessage(0, false), frontText.getMessage(1, false), frontText.getMessage(2, false), frontText.getMessage(3, false));
                LOGGER.info("Back text: {} {} {} {}", backText.getMessage(0, false), backText.getMessage(1, false), backText.getMessage(2, false), backText.getMessage(3, false));
                //red text
                Component[] messages = new Component[4];
                messages[0] = Component.literal("§c[Loja]");
                messages[1] = Component.literal("Vender");
                messages[2] = Component.literal("ITEM");
                messages[3] = Component.literal("VALOR");
                ((SignBlockEntity) block).setText(new SignText(messages, messages, DyeColor.BLACK, false), true);
                LOGGER.info("Block entity is a sign");
                event.setCanceled(true);
            } else {
                LOGGER.info("Block entity is not a sign");
            }
            return;
        }
        LOGGER.info("No block entity found at {}", targetPos);


    }


}
