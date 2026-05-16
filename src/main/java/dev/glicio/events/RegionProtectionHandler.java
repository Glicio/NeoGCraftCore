package dev.glicio.events;

import com.mojang.logging.LogUtils;
import dev.glicio.GCraftCore;
import dev.glicio.model.Region;
import dev.glicio.service.RegionService;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = GCraftCore.MODID)
public class RegionProtectionHandler {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, Integer> playerRegionCache = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof Player player) checkRegion(player);
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof Player player) checkRegion(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof Player player) checkRegion(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        if (event.getEntity().level().isClientSide()) return;
        if (event.getEntity().tickCount % 10 != 0) return;
        checkRegion(event.getEntity());
    }

    private static void checkRegion(Player player) {
        if (player.level().isClientSide()) return;

        Vec2 pos = new Vec2((float) player.getX(), (float) player.getZ());
        Map<Integer, Region> allRegions = RegionService.getAll();
        UUID uuid = player.getUUID();

        boolean found = false;
        for (Map.Entry<Integer, Region> entry : allRegions.entrySet()) {
            if (entry.getValue().checkCollision(pos)) {
                found = true;
                int regionId = entry.getKey();
                Integer last = playerRegionCache.get(uuid);
                if (last == null || !last.equals(regionId)) {
                    playerRegionCache.put(uuid, regionId);
                    onEnterRegion(player, entry.getValue());
                }
                break;
            }
        }

        if (!found && playerRegionCache.containsKey(uuid)) {
            Region last = allRegions.get(playerRegionCache.remove(uuid));
            if (last != null) onExitRegion(player, last);
        }
    }

    private static void onEnterRegion(Player player, Region region) {
        String owner = region.getOwnerName() != null ? region.getOwnerName() : "ninguém";
        player.displayClientMessage(Component.literal(
            "§a↓ §fVocê entrou na região: §e" + region.getName() + "§r de §e" + owner + "§r §a↓"), true);
    }

    private static void onExitRegion(Player player, Region region) {
        player.displayClientMessage(Component.literal(
            "§c↑ §fVocê saiu da região: §e" + region.getName() + "§r §c↑"), true);
    }
}
