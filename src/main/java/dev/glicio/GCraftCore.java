package dev.glicio;

import com.mojang.logging.LogUtils;
import dev.glicio.database.DatabaseHelper;
import dev.glicio.service.PlayerService;
import dev.glicio.service.RegionService;
import dev.glicio.service.ShopService;
import dev.glicio.service.WarpService;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

@Mod(GCraftCore.MODID)
public class GCraftCore {

    public static final String MODID = "gcraftcore";
    private static final Logger LOGGER = LogUtils.getLogger();
    public static LuckPerms luckPermsApi;

    public GCraftCore(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        try {
            Class.forName("org.postgresql.Driver");
            LOGGER.info("PostgreSQL JDBC driver loaded");
        } catch (ClassNotFoundException e) {
            LOGGER.error("Failed to load PostgreSQL JDBC driver");
            return;
        }

        DatabaseHelper.init();
        DatabaseHelper.initSchema();
        WarpService.loadSpawn();
        ShopService.loadShops();
        RegionService.loadRegions();
        PlayerService.startFlushScheduler();
    }

    @SubscribeEvent
    private void serverStarting(ServerStartingEvent event) {
        try {
            luckPermsApi = LuckPermsProvider.get();
            LOGGER.info("LuckPerms API loaded");
        } catch (Exception e) {
            LOGGER.error("LuckPerms API not found");
        }
    }

    @SubscribeEvent
    private void serverStopping(ServerStoppingEvent event) {
        PlayerService.shutdown();
        ShopService.shutdown();
        DatabaseHelper.closeDataSource();
    }
}
