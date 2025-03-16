package dev.glicio.events;

import com.mojang.logging.LogUtils;
import dev.glicio.GCraftCore;
import dev.glicio.regions.Region;
import dev.glicio.regions.RegionHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = GCraftCore.MODID)
public class RegionProtectionHandler {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    // Cache to store the last region a player was in to avoid unnecessary checks
    private static final Map<UUID, Integer> playerLastRegionCache = new HashMap<>();
    // Store last check time per player to throttle checks
    private static final Map<UUID, Long> lastCheckTime = new HashMap<>();
    private static final long CHECK_INTERVAL_MS = 500; // Check every 500ms
    
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        // Process when player logs in
        if (event.getEntity() instanceof Player player) {
            checkPlayerRegion(player);
        }
    }
    
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        // Process when player changes dimension
        if (event.getEntity() instanceof Player player) {
            checkPlayerRegion(player);
        }
    }
    
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        // Process when player respawns
        if (event.getEntity() instanceof Player player) {
            checkPlayerRegion(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        // Only check on server side
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        
        // Get the player
        Player player = event.getEntity();
        
        // Throttle checks (only check every 10 ticks = 0.5 seconds)
        if (player.tickCount % 10 != 0) {
            return;
        }
        
        // Check if the player is in a region
        checkPlayerRegion(player);
    }
    
    private static void checkPlayerRegion(Player player) {
        UUID playerUUID = player.getUUID();
        
        // Throttle checks to avoid excessive processing
        long currentTime = System.currentTimeMillis();
        Long lastCheck = lastCheckTime.get(playerUUID);
        if (lastCheck != null && currentTime - lastCheck < CHECK_INTERVAL_MS) {
            return;
        }
        lastCheckTime.put(playerUUID, currentTime);
        
        Vec2 playerPos = new Vec2((float)player.getX(), (float)player.getZ());
        
        // Get all regions
        Map<Integer, Region> regionsMap = RegionHandler.getAllRegions();
        if (regionsMap.isEmpty()) {
            return; // No regions to check
        }
        
        // Check if player is in any region
        boolean foundRegion = false;
        for (Map.Entry<Integer, Region> entry : regionsMap.entrySet()) {
            Region region = entry.getValue();
            if (region.checkCollision(playerPos)) {
                int regionId = entry.getKey();
                foundRegion = true;
                
                // Only trigger events if player entered a new region
                Integer lastRegionId = playerLastRegionCache.get(playerUUID);
                if (lastRegionId == null || !lastRegionId.equals(regionId)) {
                    playerLastRegionCache.put(playerUUID, regionId);
                    onPlayerEnterRegion(player, region);
                }
                
                break;
            }
        }
        
        // Player left all regions
        if (!foundRegion && playerLastRegionCache.containsKey(playerUUID)) {
            Region lastRegion = regionsMap.get(playerLastRegionCache.get(playerUUID));
            playerLastRegionCache.remove(playerUUID);
            if (lastRegion != null) {
                onPlayerExitRegion(player, lastRegion);
            }
        }
    }
    
    private static void onPlayerEnterRegion(Player player, Region region) {
        // Handle player entering a region
        String ownerName = region.getOwnerName() == null ? "ninguém" : region.getOwnerName();
        
        // Send to action bar instead of chat
        player.displayClientMessage(Component.literal("§a↓ §fVocê entrou na região: §e" + region.getName() + "§r de §e" + ownerName + "§r §a↓"), true);
        
        // Add your region protection logic here
    }
    
    private static void onPlayerExitRegion(Player player, Region region) {
        // Handle player exiting a region
        
        // Send to action bar instead of chat
        player.displayClientMessage(Component.literal("§c↑ §fVocê saiu da região: §e" + region.getName() + "§r §c↑"), true);
    }
} 