package dev.glicio;

import dev.glicio.database.DatabaseHelper;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.HashMap;
import java.util.Map;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(GCraftCore.MODID)
public class GCraftCore {

    private static Map<String, GPlayer> players = new HashMap<>();
    public static LuckPerms luckPermsApi;

    public static void addPlayer(String uuid, GPlayer player) {
        players.put(uuid, player);
    }

    public static void removePlayer(String uuid) {
        players.remove(uuid);
    }

    public static GPlayer getPlayer(String uuid) {
        return players.get(uuid);
    }


    // Define mod id in a common place for everything to reference
    public static final String MODID = "gcraftcore";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();


    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public GCraftCore(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // LOAD DRIVER
        try {
            Class.forName("org.postgresql.Driver");
            LOGGER.info("PostgreSQL JDBC driver loaded");
        } catch (ClassNotFoundException e) {
            LOGGER.error("Failed to load PostgreSQL JDBC driver");
            return;
        }


        // init database
        LOGGER.info("Initializing database");
        DatabaseHelper.Init();



    }

    @SubscribeEvent
    private void serverStarting(ServerStartingEvent event) {
        try {
            luckPermsApi = LuckPermsProvider.get();
            LOGGER.info("LuckPerms api loaded");
        } catch (Exception e) {
            LOGGER.error("LuckPerms api not found");
        }
    }


}
