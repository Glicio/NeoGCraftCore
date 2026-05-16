package dev.glicio.commands.region;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import dev.glicio.GCraftCore;
import dev.glicio.model.GPlayer;
import dev.glicio.model.Region;
import dev.glicio.service.PlayerService;
import dev.glicio.service.RegionService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import org.slf4j.Logger;

import java.util.Map;

import static dev.glicio.GCraftCore.luckPermsApi;

public class RegionCommand {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var regionCommand = Commands.literal("r")
            .requires(source -> {
                if (!(source.getEntity() instanceof Player player)) return false;
                var user = luckPermsApi.getUserManager().getUser(player.getUUID());
                if (user == null) return false;
                return user.getCachedData().getPermissionData().checkPermission("gcraftcore.region.admin").asBoolean()
                    || user.getCachedData().getPermissionData().checkPermission("gcraftcore.region.create").asBoolean();
            })
            .executes(RegionCommand::toggleSelectionMode);

        regionCommand.then(Commands.literal("create")
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(ctx -> createRegion(ctx, StringArgumentType.getString(ctx, "name")))));

        regionCommand.then(Commands.literal("delete").executes(RegionCommand::deleteRegion));

        regionCommand.then(Commands.literal("list")
            .requires(source -> {
                if (!(source.getEntity() instanceof Player player)) return false;
                var user = luckPermsApi.getUserManager().getUser(player.getUUID());
                return user != null && user.getCachedData().getPermissionData()
                    .checkPermission("gcraftcore.region.admin").asBoolean();
            })
            .executes(RegionCommand::listRegions));

        dispatcher.register(regionCommand);
    }

    private static int toggleSelectionMode(CommandContext<CommandSourceStack> context) {
        Player player = context.getSource().getPlayer();
        if (player == null) return 0;

        GPlayer playerData = PlayerService.get(player.getUUID().toString());
        if (playerData == null) return 0;

        playerData.setSelectingRegion(!playerData.isSelectingRegion());
        player.sendSystemMessage(Component.literal(
            playerData.isSelectingRegion() ? "Modo de seleção de região §2ativado" : "Modo de seleção de região §4desativado"));

        if (playerData.isSelectingRegion()) {
            boolean hasGoldenShovel = false;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (player.getInventory().getItem(i).is(net.minecraft.world.item.Items.GOLDEN_SHOVEL)) {
                    hasGoldenShovel = true;
                    break;
                }
            }
            if (!hasGoldenShovel) {
                player.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLDEN_SHOVEL));
                player.sendSystemMessage(Component.literal("Você recebeu uma §6pá de ouro§r para selecionar regiões"));
            }
        } else {
            playerData.clearSelection();
        }
        return 1;
    }

    private static int createRegion(CommandContext<CommandSourceStack> context, String name) {
        Player player = context.getSource().getPlayer();
        if (player == null) return 0;

        GPlayer playerData = PlayerService.get(player.getUUID().toString());
        if (playerData == null) return 0;

        Region.Rect2D selection = playerData.getSelection();

        if (!name.matches("^[a-zA-Z0-9]{4,29}$")) {
            player.sendSystemMessage(Component.literal("O nome deve ter 4-29 caracteres alfanuméricos"));
            return 0;
        }
        if (selection == null || selection.getTopLeft() == null || selection.getBottomRight() == null) {
            player.sendSystemMessage(Component.literal("Selecione as duas extremidades da região"));
            return 0;
        }

        Region newRegion = new Region(name, selection.getTopLeft(), selection.getBottomRight(),
            player.getUUID().toString(), player.getName().getString());

        for (Region existing : RegionService.getAll().values()) {
            if (newRegion.checkCollision(existing)) {
                player.sendSystemMessage(Component.literal("§cA região '" + name + "' se sobrepõe com '" + existing.getName() + "'"));
                return 0;
            }
        }

        RegionService.save(newRegion);
        playerData.clearSelection();
        playerData.setSelectingRegion(false);

        Vec2 pos1 = selection.getTopLeft();
        Vec2 pos2 = selection.getBottomRight();
        int width = (int) Math.abs(pos2.x - pos1.x) + 1;
        int height = (int) Math.abs(pos2.y - pos1.y) + 1;

        player.sendSystemMessage(Component.literal(String.format(
            "§aRegião '%s' criada! §7(Tamanho: §f%dx%d§7, Área: §f%d blocos²§7)", name, width, height, newRegion.getArea())));
        return 1;
    }

    private static int deleteRegion(CommandContext<CommandSourceStack> context) {
        Player player = context.getSource().getPlayer();
        if (player == null) return 0;

        Vec2 playerPos = new Vec2((float) player.getX(), (float) player.getZ());
        String playerUUID = player.getUUID().toString();

        Region toDelete = null;
        for (Region region : RegionService.getAll().values()) {
            if (region.checkCollision(playerPos)) { toDelete = region; break; }
        }

        if (toDelete == null) {
            player.sendSystemMessage(Component.literal("§cVocê não está em nenhuma região"));
            return 0;
        }

        boolean isAdmin = luckPermsApi.getUserManager().getUser(player.getUUID())
            .getCachedData().getPermissionData().checkPermission("gcraftcore.region.admin").asBoolean();

        if (!isAdmin && !playerUUID.equals(toDelete.getOwnerId())) {
            player.sendSystemMessage(Component.literal("§cVocê não tem permissão para deletar esta região"));
            return 0;
        }

        String ownerDisplay = toDelete.getOwnerName() != null ? toDelete.getOwnerName()
            : toDelete.getOwnerId() != null ? toDelete.getOwnerId() : "Desconhecido";

        RegionService.delete(toDelete);
        player.sendSystemMessage(Component.literal(String.format("§aRegião '%s' (de %s) removida!", toDelete.getName(), ownerDisplay)));
        return 1;
    }

    private static int listRegions(CommandContext<CommandSourceStack> context) {
        Player player = context.getSource().getPlayer();
        if (player == null) return 0;

        Map<Integer, Region> regionsMap = RegionService.getAll();
        if (regionsMap.isEmpty()) {
            player.sendSystemMessage(Component.literal("Não há regiões criadas"));
            return 1;
        }

        player.sendSystemMessage(Component.literal("§6§lLista de Regiões:"));
        for (Region region : regionsMap.values()) {
            String ownerInfo = region.getOwnerName() != null && !region.getOwnerName().isEmpty()
                ? "§7Dono: §f" + region.getOwnerName()
                : region.getOwnerId() != null ? "§7Dono: §f" + region.getOwnerId()
                : "§7Dono: §fDesconhecido";
            player.sendSystemMessage(Component.literal(String.format(
                "§e%s§r - %s - §7Área: §f%d blocos²", region.getName(), ownerInfo, region.getArea())));
        }
        return 1;
    }
}
