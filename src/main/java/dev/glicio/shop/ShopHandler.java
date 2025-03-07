package dev.glicio.shop;

import com.mojang.logging.LogUtils;
import dev.glicio.blocks.ShopSign;
import dev.glicio.database.DatabaseHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ShopHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ExecutorService executorService = Executors.newFixedThreadPool(4);
    public static Map<Vec3, ShopSign> shops = new HashMap<>();

    /**
     * Load all shops from the database
     */
    public static CompletableFuture<Void> LoadShops() {
        return CompletableFuture.runAsync(() -> {
            try (Connection con = DatabaseHelper.getConnection()) {
                PreparedStatement shopStatement = con.prepareStatement("SELECT * FROM shop");
                shopStatement.execute();
                ResultSet resultSet = shopStatement.getResultSet();
                while (resultSet.next()) {
                    ResourceLocation item = ResourceLocation.parse(resultSet.getString("item"));
                    ShopSign shop = new ShopSign(
                            resultSet.getInt("id"),
                            new Vec3(resultSet.getInt("posX"), resultSet.getInt("posY"), resultSet.getInt("posZ")),
                            resultSet.getString("owner"),
                            resultSet.getInt("value"),
                            item,
                            resultSet.getBoolean("is_admin"),
                            resultSet.getString("shop_type").equalsIgnoreCase("vender") ? ShopSign.ShopType.Vender
                                    : ShopSign.ShopType.Comprar);
                    shop.setQuantity(resultSet.getInt("quantity"));
                    shops.put(shop.getPos(), shop);
                }
                LOGGER.info("Loaded {} shops", shops.size());
            } catch (Exception e) {
                LOGGER.error("Failed to load shops: {}", e.getMessage());
            }
        }, executorService);
    }

    public static CompletableFuture<Void> SaveShop(ShopSign shop) {
        return CompletableFuture.runAsync(() -> {
            try (Connection con = DatabaseHelper.getConnection()) {
                SignBlockEntity sign = shop.getSign();
                PreparedStatement shopStatement = con.prepareStatement(
                        "INSERT INTO shop (posX, posY, posZ, owner, value, item, is_admin, quantity, shop_type) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) returning id");
                shopStatement.setInt(1, (int) shop.getPos().x);
                shopStatement.setInt(2, (int) shop.getPos().y);
                shopStatement.setInt(3, (int) shop.getPos().z);
                shopStatement.setString(4, shop.getOwner());
                shopStatement.setInt(5, shop.getValue());
                shopStatement.setString(6, shop.getItem().toString());
                shopStatement.setBoolean(7, shop.isAdmin());
                shopStatement.setInt(8, shop.getQuantity());
                shopStatement.setString(9, shop.getShopType().name());
                shopStatement.execute();
                ResultSet resultSet = shopStatement.getResultSet();
                if (resultSet.next()) {
                    shop.setId(resultSet.getInt(1));
                    shops.put(shop.getPos(), shop);
                    LOGGER.info("Saved shop: {}", shop);
                }
                if (sign != null) {
                    SignText text = getSignFormatedText(shop);
                    sign.setText(text, true);
                    sign.setText(text, false);
                } else {
                    LOGGER.error("Sign contents not updated: sign is null");
                }
            } catch (Exception e) {
                LOGGER.error("Failed to save shop: {}", e.getMessage());
            }
        }, executorService);
    }

    private static @NotNull SignText getSignFormatedText(ShopSign shop) {
        String title = shop.isAdmin() ? "§l[§4Loja§r§l]" : "§l[§eLoja§r§l]";
        String type = shop.getShopType() == ShopSign.ShopType.Vender ? "§bVender" : "§2Comprar";
        type += ":§r " + shop.getQuantity();
        Double value = shop.getValue() / 100.0;
        String valueString = String.format("%.2f", value);
        
        // Get actual item name from registry
        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(shop.getItem());
        String itemName = item.getDescription().getString();

        Component[] messages = new Component[] {
                Component.literal(title),
                Component.literal(type),
                Component.literal(valueString),
                Component.literal(itemName),
        };
        return new SignText(messages, messages, DyeColor.BLACK, false);
    }

    public static ShopSign getShop(Vec3 pos) {
        return shops.get(pos);
    }

    public static CompletableFuture<Void> deleteShop(ShopSign shop) {
        return CompletableFuture.runAsync(() -> {
            try (Connection con = DatabaseHelper.getConnection()) {
                if (shop.getPos() == null) {
                    throw new IllegalArgumentException("Shop position is null");
                }
                PreparedStatement shopStatement = con.prepareStatement("DELETE FROM shop WHERE id = ?");
                shopStatement.setInt(1, shop.getId());
                shopStatement.execute();
                shops.remove(shop.getPos());
                LOGGER.info("Deleted shop: {}", shop);
            } catch (Exception e) {
                LOGGER.error("Failed to delete shop: {}", e.getMessage());
            }
        }, executorService);
    }

    private static boolean hasChestBelow(ShopSign shop, net.minecraft.world.level.Level level) {
        net.minecraft.core.BlockPos signPos = new net.minecraft.core.BlockPos((int) shop.getPos().x,
                (int) shop.getPos().y, (int) shop.getPos().z);
        net.minecraft.core.BlockPos chestPos = signPos.below();

        var blockState = level.getBlockState(chestPos);
        if (!(blockState.getBlock() instanceof net.minecraft.world.level.block.ChestBlock)) {
            return false;
        }

        return true;
    }

    private static boolean hasEnoughItems(ShopSign shop, net.minecraft.world.level.Level level) {
        if (shop.isAdmin()) {
            return true;
        }

        net.minecraft.core.BlockPos signPos = new net.minecraft.core.BlockPos((int) shop.getPos().x,
                (int) shop.getPos().y, (int) shop.getPos().z);
        net.minecraft.core.BlockPos chestPos = signPos.below();

        // Schedule block entity check on main thread
        try {
            var server = level.getServer();
            if (server == null) {
                LOGGER.error("Server is null while checking chest contents");
                return false;
            }

            return CompletableFuture.supplyAsync(() -> {
                var blockEntity = level.getBlockEntity(chestPos);
                if (!(blockEntity instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest)) {
                    LOGGER.error("Invalid chest block entity");
                    return false;
                }

                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(shop.getItem());
                int count = 0;

                // Count items in chest
                for (int i = 0; i < chest.getContainerSize(); i++) {
                    var stack = chest.getItem(i);
                    if (stack.getItem() == item) {
                        count += stack.getCount();
                    }
                }

                return count >= shop.getQuantity();
            }, server).get(); // Execute on server thread and wait for result
        } catch (Exception e) {
            LOGGER.error("Error checking chest contents: {}", e.getMessage());
            return false;
        }
    }

    private static boolean hasSpaceInChest(ShopSign shop, net.minecraft.world.level.Level level) {
        if (shop.isAdmin()) {
            return true; // Admin shops always have space
        }

        net.minecraft.core.BlockPos signPos = new net.minecraft.core.BlockPos((int) shop.getPos().x,
                (int) shop.getPos().y, (int) shop.getPos().z);
        net.minecraft.core.BlockPos chestPos = signPos.below();


        // Schedule block entity check on main thread
        try {
            var server = level.getServer();
            if (server == null) {
                LOGGER.error("Server is null while checking chest space");
                return false;
            }

            return CompletableFuture.supplyAsync(() -> {
                var blockEntity = level.getBlockEntity(chestPos);
                if (!(blockEntity instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest)) {
                    LOGGER.error("Invalid chest block entity while checking chest space");
                    return false;
                }

                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(shop.getItem());
                int emptySlots = 0;
                int partialSlots = 0;

                // Count available space in chest
                for (int i = 0; i < chest.getContainerSize(); i++) {
                    var stack = chest.getItem(i);
                    if (stack.isEmpty()) {
                        emptySlots++;
                    } else if (stack.getItem() == item && stack.getCount() < stack.getMaxStackSize()) {
                        partialSlots += stack.getMaxStackSize() - stack.getCount();
                    }
                }

                return (emptySlots * 64 + partialSlots) >= shop.getQuantity();
            }, server).get(); // Execute on server thread and wait for result
        } catch (Exception e) {
            LOGGER.error("Error checking chest space: {}", e.getMessage());
            return false;
        }
    }

    private static CompletableFuture<Boolean> hasEnoughBalance(String playerUuid, int cost) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection con = DatabaseHelper.getConnection()) {
                int balance = dev.glicio.database.PlayerDb.getBalance(playerUuid, con);
                return balance >= cost;
            } catch (Exception e) {
                LOGGER.error("Failed to check player balance: {}", e.getMessage());
                return false;
            }
        }, executorService);
    }

    private static CompletableFuture<Void> updateBalance(String playerUuid, int amount) {
        return CompletableFuture.runAsync(() -> {
            try (Connection con = DatabaseHelper.getConnection()) {
                dev.glicio.database.PlayerDb.updateBalance(playerUuid, amount, con);

                // Update in-memory player object if available
                dev.glicio.GPlayer player = dev.glicio.GCraftCore.getPlayer(playerUuid);
                if (player != null) {
                    player.addBalance(amount);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to update player balance: {}", e.getMessage());
            }
        }, executorService);
    }

    private static void transferItems(ShopSign shop, net.minecraft.world.level.Level level,
            net.minecraft.world.entity.player.Player player) {
        if (shop.isAdmin()) {
            // Give items directly to player for admin shops
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(shop.getItem());
            var itemStack = new net.minecraft.world.item.ItemStack(item, shop.getQuantity());
            player.getInventory().add(itemStack);
            return;
        }

        net.minecraft.core.BlockPos signPos = new net.minecraft.core.BlockPos((int) shop.getPos().x,
                (int) shop.getPos().y, (int) shop.getPos().z);
        net.minecraft.core.BlockPos chestPos = signPos.below();

        try {
            var server = level.getServer();
            if (server == null) {
                LOGGER.error("Server is null while transferring items");
                return;
            }

            CompletableFuture.runAsync(() -> {
                var blockEntity = level.getBlockEntity(chestPos);
                if (!(blockEntity instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest)) {
                    LOGGER.error("Invalid chest block entity while transferring items");
                    return;
                }

                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(shop.getItem());
                int remaining = shop.getQuantity();

                // Remove items from chest and give to player
                for (int i = 0; i < chest.getContainerSize() && remaining > 0; i++) {
                    var stack = chest.getItem(i);
                    if (stack.getItem() == item) {
                        int toRemove = Math.min(remaining, stack.getCount());
                        stack.shrink(toRemove);
                        chest.setItem(i, stack);

                        var newStack = new net.minecraft.world.item.ItemStack(item, toRemove);
                        player.getInventory().add(newStack);

                        remaining -= toRemove;
                    }
                }
            }, server).get(); // Execute on server thread and wait for result
        } catch (Exception e) {
            LOGGER.error("Error transferring items: {}", e.getMessage());
        }
    }

    public static CompletableFuture<Boolean> buyFromShop(ShopSign shop, net.minecraft.world.entity.player.Player player) {
        return CompletableFuture.supplyAsync(() -> {
            if (!shop.isAdmin()) {
                if (!hasChestBelow(shop, player.level())) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Esta loja não tem um baú"));
                    return false;
                }

                if (!hasEnoughItems(shop, player.level())) {
                    player.sendSystemMessage(
                            net.minecraft.network.chat.Component.literal("Esta loja não tem itens suficientes"));
                    return false;
                }
            }

            int totalCost = shop.getValue();
            String playerUuid = player.getUUID().toString();

            try {
                if (!hasEnoughBalance(playerUuid, totalCost).get()) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Você não tem dinheiro suficiente"));
                    return false;
                }

                // Process the transaction
                updateBalance(playerUuid, -totalCost).get(); // Subtract from buyer
                if (!shop.isAdmin()) {
                    updateBalance(shop.getOwner(), totalCost).get(); // Add to seller
                }

                // Transfer the items
                transferItems(shop, player.level(), player);

                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Compra realizada com sucesso"));
                return true;
            } catch (Exception e) {
                LOGGER.error("Failed to process buy transaction: {}", e.getMessage());
                return false;
            }
        }, executorService);
    }

    private static boolean hasEnoughItemsInInventory(net.minecraft.world.entity.player.Player player, ShopSign shop) {
        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(shop.getItem());
        int count = 0;

        // Count items in player inventory
        for (var stack : player.getInventory().items) {
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }

        return count >= shop.getQuantity();
    }

    private static void transferItemsToChest(ShopSign shop, net.minecraft.world.level.Level level,
            net.minecraft.world.entity.player.Player player) {
        if (shop.isAdmin()) {
            // Just remove items from player inventory for admin shops
            removeItemsFromPlayer(player, shop);
            return;
        }

        net.minecraft.core.BlockPos signPos = new net.minecraft.core.BlockPos((int) shop.getPos().x,
                (int) shop.getPos().y, (int) shop.getPos().z);
        net.minecraft.core.BlockPos chestPos = signPos.below();

        try {
            var server = level.getServer();
            if (server == null) {
                LOGGER.error("Server is null while transferring items to chest");
                return;
            }

            CompletableFuture.runAsync(() -> {
                var blockEntity = level.getBlockEntity(chestPos);
                if (!(blockEntity instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest)) {
                    LOGGER.error("Invalid chest block entity while transferring items to chest");
                    return;
                }

                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(shop.getItem());
                int remaining = shop.getQuantity();

                // Remove items from player inventory
                for (int i = 0; i < player.getInventory().items.size() && remaining > 0; i++) {
                    var stack = player.getInventory().items.get(i);
                    if (stack.getItem() == item) {
                        int toRemove = Math.min(remaining, stack.getCount());
                        stack.shrink(toRemove);

                        // Add to chest
                        addItemsToChest(chest, item, toRemove);

                        remaining -= toRemove;
                    }
                }
            }, server).get(); // Execute on server thread and wait for result
        } catch (Exception e) {
            LOGGER.error("Error transferring items to chest: {}", e.getMessage());
        }
    }

    private static void removeItemsFromPlayer(net.minecraft.world.entity.player.Player player, ShopSign shop) {
        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(shop.getItem());
        int remaining = shop.getQuantity();

        for (int i = 0; i < player.getInventory().items.size() && remaining > 0; i++) {
            var stack = player.getInventory().items.get(i);
            if (stack.getItem() == item) {
                int toRemove = Math.min(remaining, stack.getCount());
                stack.shrink(toRemove);
                remaining -= toRemove;
            }
        }
    }

    private static void addItemsToChest(net.minecraft.world.level.block.entity.ChestBlockEntity chest,
            net.minecraft.world.item.Item item, int amount) {
        int remaining = amount;

        // First try to fill existing stacks
        for (int i = 0; i < chest.getContainerSize() && remaining > 0; i++) {
            var stack = chest.getItem(i);
            if (stack.getItem() == item && stack.getCount() < stack.getMaxStackSize()) {
                int canAdd = Math.min(remaining, stack.getMaxStackSize() - stack.getCount());
                stack.grow(canAdd);
                chest.setItem(i, stack);
                remaining -= canAdd;
            }
        }

        // Then use empty slots if needed
        for (int i = 0; i < chest.getContainerSize() && remaining > 0; i++) {
            var stack = chest.getItem(i);
            if (stack.isEmpty()) {
                int toAdd = Math.min(remaining, 64);
                chest.setItem(i, new net.minecraft.world.item.ItemStack(item, toAdd));
                remaining -= toAdd;
            }
        }
    }

    public static CompletableFuture<Boolean> sellToShop(ShopSign shop, net.minecraft.world.entity.player.Player player) {
        return CompletableFuture.supplyAsync(() -> {
            if (!shop.isAdmin()) {
                if (!hasChestBelow(shop, player.level())) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Esta loja não tem um baú"));
                    return false;
                }

                if (!hasSpaceInChest(shop, player.level())) {
                    player.sendSystemMessage(
                            net.minecraft.network.chat.Component.literal("Esta loja não tem espaço suficiente"));
                    return false;
                }

                try {
                    // Check if shop owner has enough money
                    if (!hasEnoughBalance(shop.getOwner(), shop.getValue()).get()) {
                        player.sendSystemMessage(
                                net.minecraft.network.chat.Component.literal("O dono da loja não tem dinheiro suficiente"));
                        return false;
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to check shop owner balance: {}", e.getMessage());
                    return false;
                }
            }

            // Check if player has enough items
            if (!hasEnoughItemsInInventory(player, shop)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Você não tem itens suficientes"));
                return false;
            }

            int totalValue = shop.getValue();
            String playerUuid = player.getUUID().toString();

            try {
                // Process the transaction
                if (!shop.isAdmin()) {
                    updateBalance(shop.getOwner(), -totalValue).get(); // Subtract from shop owner
                }
                updateBalance(playerUuid, totalValue).get(); // Add to seller

                // Transfer the items
                transferItemsToChest(shop, player.level(), player);

                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Venda realizada com sucesso"));
                return true;
            } catch (Exception e) {
                LOGGER.error("Failed to process sell transaction: {}", e.getMessage());
                return false;
            }
        }, executorService);
    }

    public static void shutdown() {
        executorService.shutdown();
    }
}
