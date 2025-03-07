package dev.glicio.events;

import dev.glicio.GCraftCore;
import dev.glicio.blocks.ShopSign;
import dev.glicio.shop.ShopHandler;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.Objects;

import static dev.glicio.GCraftCore.luckPermsApi;

@EventBusSubscriber(modid = GCraftCore.MODID)
public class ShopBreakEventHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onShopBreak(BlockEvent.BreakEvent event) {
        var targetPos = event.getPos();
        var player = event.getPlayer();
        var level = event.getPlayer().level();
        var block = level.getBlockEntity(targetPos);

        // Check if there's a sign on the block being broken
        if (hasAttachedShops(level, targetPos)) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal("Por favor, exclua todas as lojas neste bloco antes de quebrá-lo"));
            return;
        }

        Vec3 pos = new Vec3(targetPos.getX(), targetPos.getY(), targetPos.getZ());
        if (block != null) {
            if (block instanceof SignBlockEntity) {
                ShopSign shop = ShopHandler.getShop(pos);
                if(shop == null) {
                    //sign is not a shop
                    return;
                }
                if(shop.getSign() == null) {
                    shop.setSign((SignBlockEntity) block);
                }

                User user = luckPermsApi.getUserManager().getUser(player.getUUID());
                assert user != null;
                //get user prefix
                String groupName = Objects.requireNonNull(user.getNodes().stream().filter(NodeType.INHERITANCE::matches).map(NodeType.INHERITANCE::cast).findFirst().orElse(null)).getGroupName();
                Group group = luckPermsApi.getGroupManager().getGroup(groupName);
                assert group != null;
                Node shopAdmin = group.getNodes().stream().filter(node -> node.getKey().equalsIgnoreCase("gcraftcore.shop.admin")).findFirst().orElse(null);
                if(!shop.getOwner().equals(player.getUUID().toString())) {
                    if (shopAdmin == null || !shopAdmin.getValue()) {
                        event.setCanceled(true);
                        player.sendSystemMessage(Component.literal("Você não tem permissão para deletar esta loja"));
                        return;
                    }
                }
                try {
                    ShopHandler.deleteShop(shop);
                    player.sendSystemMessage(Component.literal("Loja deletada com sucesso"));
                } catch (Exception e) {
                    event.setCanceled(true);
                    LOGGER.error("Failed to delete shop: {}", e.getMessage());
                }

            }
        }
    }

    private static boolean hasAttachedShops(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        // Check if there are any signs attached to this block
        net.minecraft.core.BlockPos[] attachmentPositions = {
            pos.above(),    // up
            pos.north(),    // north
            pos.south(),    // south
            pos.east(),     // east
            pos.west()      // west
        };

        for (net.minecraft.core.BlockPos attachPos : attachmentPositions) {
            var attachedBlockEntity = level.getBlockEntity(attachPos);
            if (attachedBlockEntity instanceof SignBlockEntity) {
                // Check if this sign is attached to our block
                var signState = level.getBlockState(attachPos);
                
                // Check for wall signs
                if (signState.getBlock() instanceof net.minecraft.world.level.block.WallSignBlock) {
                    // Get the direction the sign is facing and check if it points to our block
                    var signDirection = signState.getValue(net.minecraft.world.level.block.WallSignBlock.FACING);
                    var attachedTo = attachPos.relative(signDirection.getOpposite());
                    
                    if (attachedTo.equals(pos)) {
                        Vec3 signPos = new Vec3(attachPos.getX(), attachPos.getY(), attachPos.getZ());
                        ShopSign shop = ShopHandler.getShop(signPos);
                        if (shop != null) {
                            return true;
                        }
                    }
                }
                // Check for standing signs
                else if (signState.getBlock() instanceof net.minecraft.world.level.block.StandingSignBlock) {
                    // For standing signs, we only need to check if it's directly above and if it's a shop
                    if (attachPos.equals(pos.above())) {
                        Vec3 signPos = new Vec3(attachPos.getX(), attachPos.getY(), attachPos.getZ());
                        ShopSign shop = ShopHandler.getShop(signPos);
                        if (shop != null) {
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }
}
