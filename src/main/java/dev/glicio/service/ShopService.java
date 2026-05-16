package dev.glicio.service;

import com.mojang.logging.LogUtils;
import dev.glicio.database.DatabaseHelper;
import dev.glicio.database.ShopDao;
import dev.glicio.model.GPlayer;
import dev.glicio.model.ShopSign;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ShopService {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<Vec3, ShopSign> shops = new ConcurrentHashMap<>();
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);

    public static void loadShops() {
        CompletableFuture.runAsync(() -> {
            try (Connection con = DatabaseHelper.getConnection()) {
                for (ShopSign shop : ShopDao.loadAll(con)) {
                    shops.put(shop.getPos(), shop);
                }
                LOGGER.info("Loaded {} shops", shops.size());
            } catch (Exception e) {
                LOGGER.error("Failed to load shops: {}", e.getMessage());
            }
        }, executor);
    }

    public static ShopSign get(Vec3 pos) {
        return shops.get(pos);
    }

    public static void shutdown() {
        executor.shutdown();
    }

    public static CompletableFuture<Void> save(ShopSign shop) {
        return CompletableFuture.runAsync(() -> {
            try (Connection con = DatabaseHelper.getConnection()) {
                int id = ShopDao.save(shop, con);
                if (id == -1) {
                    LOGGER.error("Failed to save shop — DB returned no ID");
                    return;
                }
                shop.setId(id);
                shops.put(shop.getPos(), shop);
                SignBlockEntity sign = shop.getSign();
                if (sign != null) {
                    SignText text = buildSignText(shop);
                    sign.setText(text, true);
                    sign.setText(text, false);
                }
                LOGGER.info("Saved shop: {}", shop);
            } catch (Exception e) {
                LOGGER.error("Failed to save shop: {}", e.getMessage());
            }
        }, executor);
    }

    public static CompletableFuture<Void> delete(ShopSign shop) {
        return CompletableFuture.runAsync(() -> {
            try (Connection con = DatabaseHelper.getConnection()) {
                ShopDao.delete(shop.getId(), con);
                shops.remove(shop.getPos());
                LOGGER.info("Deleted shop: {}", shop);
            } catch (Exception e) {
                LOGGER.error("Failed to delete shop: {}", e.getMessage());
            }
        }, executor);
    }

    public static CompletableFuture<Boolean> buyFromShop(ShopSign shop, Player player) {
        return CompletableFuture.supplyAsync(() -> {
            if (shop.getOwner().equals(player.getUUID().toString())) {
                player.displayClientMessage(Component.literal("§cVocê não pode comprar da sua própria loja"), true);
                return true;
            }

            if (!shop.isAdmin()) {
                if (!hasChestBelow(shop, player.level())) {
                    player.displayClientMessage(Component.literal("§cEsta loja não tem um baú"), true);
                    return false;
                }
                if (!hasEnoughItems(shop, player.level())) {
                    player.displayClientMessage(Component.literal("§cEsta loja não tem itens suficientes"), true);
                    return false;
                }
            }

            int totalCost = shop.getValue();
            String playerUuid = player.getUUID().toString();

            GPlayer buyer = PlayerService.get(playerUuid);
            if (buyer == null) {
                player.displayClientMessage(Component.literal("§cErro ao processar transação"), true);
                return false;
            }

            synchronized (buyer) {
                if (buyer.getBalance() < totalCost) {
                    player.displayClientMessage(Component.literal("§cVocê não tem dinheiro suficiente"), true);
                    return false;
                }
                buyer.addBalance(-totalCost);
            }

            if (!shop.isAdmin()) {
                PlayerService.addBalance(shop.getOwner(), totalCost);
            }

            transferItemsToPlayer(shop, player.level(), player);
            player.displayClientMessage(Component.literal("Compra realizada. Novo saldo: §2" + buyer.getFormattedBalance()), true);
            return true;
        }, executor);
    }

    public static CompletableFuture<Boolean> sellToShop(ShopSign shop, Player player) {
        return CompletableFuture.supplyAsync(() -> {
            if (shop.getOwner().equals(player.getUUID().toString())) {
                player.displayClientMessage(Component.literal("§cVocê não pode vender para sua própria loja"), true);
                return true;
            }

            if (!shop.isAdmin()) {
                if (!hasChestBelow(shop, player.level())) {
                    player.displayClientMessage(Component.literal("§cEsta loja não tem um baú"), true);
                    return false;
                }
                if (!hasSpaceInChest(shop, player.level())) {
                    player.displayClientMessage(Component.literal("§cEsta loja não tem espaço suficiente"), true);
                    return false;
                }
            }

            if (!hasEnoughItemsInInventory(player, shop)) {
                player.displayClientMessage(Component.literal("§cVocê não tem itens suficientes"), true);
                return false;
            }

            int totalValue = shop.getValue();
            String playerUuid = player.getUUID().toString();

            if (!shop.isAdmin() && !PlayerService.deductBalance(shop.getOwner(), totalValue)) {
                player.displayClientMessage(Component.literal("§cO dono da loja não tem dinheiro suficiente"), true);
                return false;
            }

            GPlayer seller = PlayerService.get(playerUuid);
            if (seller == null) {
                if (!shop.isAdmin()) PlayerService.addBalance(shop.getOwner(), totalValue);
                player.displayClientMessage(Component.literal("§cErro ao processar transação"), true);
                return false;
            }

            seller.addBalance(totalValue);
            transferItemsToChest(shop, player.level(), player);
            player.displayClientMessage(Component.literal("Venda realizada. Novo saldo: §2" + seller.getFormattedBalance()), true);
            return true;
        }, executor);
    }

    private static SignText buildSignText(ShopSign shop) {
        String title = shop.isAdmin() ? "§l[§4Loja§r§l]" : "§l[§eLoja§r§l]";
        String type = (shop.getShopType() == ShopSign.ShopType.Vender ? "§bVender" : "§2Comprar") + ":§r " + shop.getQuantity();
        String valueStr = String.format("%.2f", shop.getValue() / 100.0);
        String itemName = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(shop.getItem()).getDescription().getString();
        Component[] lines = {Component.literal(title), Component.literal(type), Component.literal(valueStr), Component.literal(itemName)};
        return new SignText(lines, lines, DyeColor.BLACK, false);
    }

    private static boolean hasChestBelow(ShopSign shop, net.minecraft.world.level.Level level) {
        var chestPos = new net.minecraft.core.BlockPos((int) shop.getPos().x, (int) shop.getPos().y, (int) shop.getPos().z).below();
        return level.getBlockState(chestPos).getBlock() instanceof net.minecraft.world.level.block.ChestBlock;
    }

    private static boolean hasEnoughItems(ShopSign shop, net.minecraft.world.level.Level level) {
        if (shop.isAdmin()) return true;
        var chestPos = new net.minecraft.core.BlockPos((int) shop.getPos().x, (int) shop.getPos().y, (int) shop.getPos().z).below();
        try {
            var server = level.getServer();
            if (server == null) return false;
            return CompletableFuture.supplyAsync(() -> {
                if (!(level.getBlockEntity(chestPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest)) return false;
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(shop.getItem());
                int count = 0;
                for (int i = 0; i < chest.getContainerSize(); i++) {
                    var stack = chest.getItem(i);
                    if (stack.getItem() == item) count += stack.getCount();
                }
                return count >= shop.getQuantity();
            }, server).get();
        } catch (Exception e) {
            LOGGER.error("Error checking chest contents: {}", e.getMessage());
            return false;
        }
    }

    private static boolean hasSpaceInChest(ShopSign shop, net.minecraft.world.level.Level level) {
        if (shop.isAdmin()) return true;
        var chestPos = new net.minecraft.core.BlockPos((int) shop.getPos().x, (int) shop.getPos().y, (int) shop.getPos().z).below();
        try {
            var server = level.getServer();
            if (server == null) return false;
            return CompletableFuture.supplyAsync(() -> {
                if (!(level.getBlockEntity(chestPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest)) return false;
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(shop.getItem());
                int space = 0;
                for (int i = 0; i < chest.getContainerSize(); i++) {
                    var stack = chest.getItem(i);
                    if (stack.isEmpty()) space += 64;
                    else if (stack.getItem() == item) space += stack.getMaxStackSize() - stack.getCount();
                }
                return space >= shop.getQuantity();
            }, server).get();
        } catch (Exception e) {
            LOGGER.error("Error checking chest space: {}", e.getMessage());
            return false;
        }
    }

    private static boolean hasEnoughItemsInInventory(Player player, ShopSign shop) {
        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(shop.getItem());
        int count = 0;
        for (var stack : player.getInventory().items) {
            if (stack.getItem() == item) count += stack.getCount();
        }
        return count >= shop.getQuantity();
    }

    private static void transferItemsToPlayer(ShopSign shop, net.minecraft.world.level.Level level, Player player) {
        if (shop.isAdmin()) {
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(shop.getItem());
            player.getInventory().add(new net.minecraft.world.item.ItemStack(item, shop.getQuantity()));
            return;
        }
        var chestPos = new net.minecraft.core.BlockPos((int) shop.getPos().x, (int) shop.getPos().y, (int) shop.getPos().z).below();
        try {
            var server = level.getServer();
            if (server == null) return;
            CompletableFuture.runAsync(() -> {
                if (!(level.getBlockEntity(chestPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest)) return;
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(shop.getItem());
                int remaining = shop.getQuantity();
                for (int i = 0; i < chest.getContainerSize() && remaining > 0; i++) {
                    var stack = chest.getItem(i);
                    if (stack.getItem() == item) {
                        int toRemove = Math.min(remaining, stack.getCount());
                        stack.shrink(toRemove);
                        chest.setItem(i, stack);
                        player.getInventory().add(new net.minecraft.world.item.ItemStack(item, toRemove));
                        remaining -= toRemove;
                    }
                }
            }, server).get();
        } catch (Exception e) {
            LOGGER.error("Error transferring items to player: {}", e.getMessage());
        }
    }

    private static void transferItemsToChest(ShopSign shop, net.minecraft.world.level.Level level, Player player) {
        if (shop.isAdmin()) {
            removeItemsFromPlayer(player, shop);
            return;
        }
        var chestPos = new net.minecraft.core.BlockPos((int) shop.getPos().x, (int) shop.getPos().y, (int) shop.getPos().z).below();
        try {
            var server = level.getServer();
            if (server == null) return;
            CompletableFuture.runAsync(() -> {
                if (!(level.getBlockEntity(chestPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest)) return;
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(shop.getItem());
                int remaining = shop.getQuantity();
                for (int i = 0; i < player.getInventory().items.size() && remaining > 0; i++) {
                    var stack = player.getInventory().items.get(i);
                    if (stack.getItem() == item) {
                        int toRemove = Math.min(remaining, stack.getCount());
                        stack.shrink(toRemove);
                        addItemsToChest(chest, item, toRemove);
                        remaining -= toRemove;
                    }
                }
            }, server).get();
        } catch (Exception e) {
            LOGGER.error("Error transferring items to chest: {}", e.getMessage());
        }
    }

    private static void removeItemsFromPlayer(Player player, ShopSign shop) {
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
        for (int i = 0; i < chest.getContainerSize() && remaining > 0; i++) {
            var stack = chest.getItem(i);
            if (stack.getItem() == item && stack.getCount() < stack.getMaxStackSize()) {
                int canAdd = Math.min(remaining, stack.getMaxStackSize() - stack.getCount());
                stack.grow(canAdd);
                chest.setItem(i, stack);
                remaining -= canAdd;
            }
        }
        for (int i = 0; i < chest.getContainerSize() && remaining > 0; i++) {
            var stack = chest.getItem(i);
            if (stack.isEmpty()) {
                int toAdd = Math.min(remaining, 64);
                chest.setItem(i, new net.minecraft.world.item.ItemStack(item, toAdd));
                remaining -= toAdd;
            }
        }
    }
}
