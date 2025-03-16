package dev.glicio.commands.region;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import dev.glicio.GCraftCore;
import dev.glicio.GPlayer;
import dev.glicio.regions.Region;
import dev.glicio.regions.RegionHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import net.minecraft.world.phys.Vec2;

import static dev.glicio.GCraftCore.luckPermsApi;

public class RegionCommand {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Base command requiring permissions
        var regionCommand = Commands.literal("r")
            .requires(source -> {
                if (!(source.getEntity() instanceof Player player)) return false;
                var user = luckPermsApi.getUserManager().getUser(player.getUUID());
                if(user == null) {
                    return false;
                }

                if(user.getCachedData().getPermissionData().checkPermission("gcraftcore.region.admin").asBoolean()) {
                    return true;
                }
                
                return user.getCachedData().getPermissionData().checkPermission("gcraftcore.region.create").asBoolean();
            })
            .executes(RegionCommand::toggleSelectionMode); // Default /r command toggles selection mode

        // Add /r create [name] subcommand
        regionCommand.then(Commands.literal("create")
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(context -> createRegion(context, StringArgumentType.getString(context, "name")))));
        
        // Add /r delete subcommand
        regionCommand.then(Commands.literal("delete")
            .executes(RegionCommand::deleteRegion));

        // Add /r list subcommand - only for admins
        regionCommand.then(Commands.literal("list")
            .requires(source -> {
                if (!(source.getEntity() instanceof Player player)) return false;
                var user = luckPermsApi.getUserManager().getUser(player.getUUID());
                return user != null && user.getCachedData().getPermissionData().checkPermission("gcraftcore.region.admin").asBoolean();
            })
            .executes(RegionCommand::listRegions));

        // Register the command
        dispatcher.register(regionCommand);
    }

    /**
     * Toggles region selection mode for the player
     */
    private static int toggleSelectionMode(CommandContext<CommandSourceStack> context) {
        Player player = context.getSource().getPlayer();
        if (player == null) {
            LOGGER.error("This command can only be executed as a player");
            return 0;
        }

        GPlayer playerData = GCraftCore.getPlayer(player.getUUID().toString());
        playerData.setSelectingRegion(!playerData.isSelectingRegion());
        player.sendSystemMessage(Component.literal(playerData.isSelectingRegion() ? "Modo de seleção de região §2ativado" : "Modo de seleção de região §4desativado"));

        // Check if player has a golden shovel when activating selection mode
        if (playerData.isSelectingRegion()) {
            boolean hasGoldenShovel = false;
            
            // Check player's inventory for a golden shovel
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (player.getInventory().getItem(i).is(net.minecraft.world.item.Items.GOLDEN_SHOVEL)) {
                    hasGoldenShovel = true;
                    break;
                }
            }
            
            // Give player a golden shovel if they don't have one
            if (!hasGoldenShovel) {
                player.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLDEN_SHOVEL));
                player.sendSystemMessage(Component.literal("Você recebeu uma §6pá de ouro§r para selecionar regiões"));
            }
        } else {
            playerData.clearSelection();
        }

        return 1;
    }

    /**
     * Creates a new region with the given name
     */
    private static int createRegion(CommandContext<CommandSourceStack> context, String name) {
        Player player = context.getSource().getPlayer();
        if (player == null) {
            LOGGER.error("This command can only be executed as a player");
            return 0;
        }
        
        GPlayer playerData = GCraftCore.getPlayer(player.getUUID().toString());
        Region.Rect2D selection = playerData.getSelection();

        if(name == null) {
            player.sendSystemMessage(Component.literal("Insira um nome para a região"));
            return 0;
        }

        if(!name.matches("^[a-zA-Z0-9]{4,29}$")) {
            player.sendSystemMessage(Component.literal("O nome da região deve ter entre 4 e 29 caracteres e conter apenas letras e números"));
            return 0;
        }

        if(selection.getTopLeft() == null || selection.getBottomRight() == null) {
            player.sendSystemMessage(Component.literal("Selecione as duas extremidades da região"));
            return 0;
        }
        
        // Get player UUID
        String playerUUID = player.getUUID().toString();
        String playerName = player.getName().getString();
        
        // Create a temporary region object to check for collisions
        Region newRegion = new Region(name, selection.getTopLeft(), selection.getBottomRight(), playerUUID, playerName);
        
        // Check for collisions with existing regions
        java.util.Map<Integer, Region> existingRegions = RegionHandler.getAllRegions();
        for (Region existingRegion : existingRegions.values()) {
            if (newRegion.checkCollision(existingRegion)) {
                player.sendSystemMessage(Component.literal("§cErro: A região '" + name + "' se sobrepõe com a região existente '" + existingRegion.getName() + "'"));
                return 0;
            }
        }
        
        // No collisions found, save the region
        RegionHandler.saveRegion(newRegion);
        playerData.clearSelection();
        playerData.setSelectingRegion(false);

        // Calculate width and height for a clearer message
        Vec2 pos1 = selection.getTopLeft();
        Vec2 pos2 = selection.getBottomRight();
        int width = (int)Math.abs(pos2.x - pos1.x) + 1;  // +1 to include border block
        int height = (int)Math.abs(pos2.y - pos1.y) + 1;  // +1 to include border block

        player.sendSystemMessage(Component.literal(String.format(
            "§aRegião '%s' criada com sucesso! §7(Tamanho: §f%dx%d§7, Área: §f%d blocos²§7)", 
            name,
            width, height,
            newRegion.getArea()
        )));
        return 1;
    }

    /**
     * Deletes the region the player is currently in
     */
    private static int deleteRegion(CommandContext<CommandSourceStack> context) {
        Player player = context.getSource().getPlayer();
        if (player == null) {
            LOGGER.error("This command can only be executed as a player");
            return 0;
        }
        
        // Get the player's current position
        Vec2 playerPos = new Vec2((float)player.getX(), (float)player.getZ());
        String playerUUID = player.getUUID().toString();
        
        // Check if the player is in any region
        java.util.Map<Integer, Region> regionsMap = RegionHandler.getAllRegions();
        Region regionToDelete = null;
        
        for (Region region : regionsMap.values()) {
            if (region.checkCollision(playerPos)) {
                regionToDelete = region;
                break;
            }
        }
        
        if (regionToDelete == null) {
            player.sendSystemMessage(Component.literal("§cVocê não está em nenhuma região"));
            return 0;
        }
        
        // Check if player has permission to delete this region
        boolean isAdmin = luckPermsApi.getUserManager().getUser(player.getUUID())
            .getCachedData().getPermissionData().checkPermission("gcraftcore.region.admin").asBoolean();
            
        boolean isOwner = playerUUID.equals(regionToDelete.getOwnerId());
        
        if (!isAdmin && !isOwner) {
            player.sendSystemMessage(Component.literal("§cVocê não tem permissão para deletar esta região"));
            return 0;
        }
        
        // Delete the region
        String regionName = regionToDelete.getName();
        String ownerDisplay = regionToDelete.getOwnerName() != null ? 
            regionToDelete.getOwnerName() : 
            (regionToDelete.getOwnerId() != null ? regionToDelete.getOwnerId() : "Desconhecido");
            
        RegionHandler.deleteRegion(regionToDelete);
        player.sendSystemMessage(Component.literal(String.format(
            "§aRegião '%s' (de %s) removida com sucesso!", regionName, ownerDisplay
        )));
        return 1;
    }

    /**
     * Lists all regions for admin players
     */
    private static int listRegions(CommandContext<CommandSourceStack> context) {
        Player player = context.getSource().getPlayer();
        if (player == null) {
            LOGGER.error("This command can only be executed as a player");
            return 0;
        }
        
        // Get the regions map using Java reflection since there's no direct access method
        java.util.Map<Integer, Region> regionsMap = RegionHandler.getAllRegions();
        
        if (regionsMap.isEmpty()) {
            player.sendSystemMessage(Component.literal("Não há regiões criadas"));
            return 1;
        }
        
        player.sendSystemMessage(Component.literal("§6§lLista de Regiões:"));
        for (Region region : regionsMap.values()) {
            var bounds = region.getBounds();
            var pos1 = bounds.getTopLeft();
            var pos2 = bounds.getBottomRight();
            
            // Format the owner information - use the owner's name if available, otherwise use UUID or "Unknown"
            String ownerInfo;
            if (region.getOwnerName() != null && !region.getOwnerName().isEmpty()) {
                ownerInfo = String.format("§7Dono: §f%s", region.getOwnerName());
            } else if (region.getOwnerId() != null) {
                ownerInfo = String.format("§7Dono: §f%s", region.getOwnerId());
            } else {
                ownerInfo = "§7Dono: §fDesconhecido";
            }
            
            // Get the area of the region
            int area = region.getArea();
                
            player.sendSystemMessage(Component.literal(String.format(
                "§e%s§r - %s - §7Área: §f%d blocos²",
                region.getName(), 
                ownerInfo,
                area
            )));
        }
        
        return 1;
    }
}

