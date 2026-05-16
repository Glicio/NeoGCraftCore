package dev.glicio;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = GCraftCore.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.ConfigValue<String> DATABASE_URL = BUILDER
            .comment("Connection string to a postgres database")
            .define("databaseUrl", "jdbc:postgresql://localhost:5432/gcraft");
    private static final ModConfigSpec.ConfigValue<String> DATABASE_USER = BUILDER
            .comment("Database user")
            .define("databaseUser", "gserver");
    private static final ModConfigSpec.ConfigValue<String> DATABASE_PASSWORD = BUILDER
            .comment("Database password")
            .define("databasePassword", "gserver");

    static final ModConfigSpec SPEC = BUILDER.build();

    public static String databaseUrl;
    public static String databaseUser;
    public static String databasePassword;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        databaseUrl = DATABASE_URL.get();
        databaseUser = DATABASE_USER.get();
        databasePassword = DATABASE_PASSWORD.get();
    }
}
