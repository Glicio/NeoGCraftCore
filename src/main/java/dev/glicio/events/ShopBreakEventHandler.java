package dev.glicio.events;

import com.mojang.logging.LogUtils;
import dev.glicio.GCraftCore;
import dev.glicio.model.ShopSign;
import dev.glicio.service.ShopService;
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
import org.slf4j.Logger;

import java.util.Objects;

import static dev.glicio.GCraftCore.luckPermsApi;

@EventBusSubscriber(modid = GCraftCore.MODID)
public class ShopBreakEventHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        var pos = event.getPos();
        var player = event.getPlayer();
        var level = player.level();

        if (hasAttachedShops(level, pos)) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal("Por favor, exclua todas as lojas neste bloco antes de quebrá-lo"));
            return;
        }

        var block = level.getBlockEntity(pos);
        if (!(block instanceof SignBlockEntity sign)) return;

        Vec3 signPos = new Vec3(pos.getX(), pos.getY(), pos.getZ());
        ShopSign shop = ShopService.get(signPos);
        if (shop == null) return;

        if (shop.getSign() == null) shop.setSign(sign);

        User user = luckPermsApi.getUserManager().getUser(player.getUUID());
        assert user != null;
        String groupName = Objects.requireNonNull(
            user.getNodes().stream().filter(NodeType.INHERITANCE::matches)
                .map(NodeType.INHERITANCE::cast).findFirst().orElse(null)
        ).getGroupName();
        Group group = luckPermsApi.getGroupManager().getGroup(groupName);
        assert group != null;
        Node shopAdmin = group.getNodes().stream()
            .filter(n -> n.getKey().equalsIgnoreCase("gcraftcore.shop.admin"))
            .findFirst().orElse(null);

        if (!shop.getOwner().equals(player.getUUID().toString())) {
            if (shopAdmin == null || !shopAdmin.getValue()) {
                event.setCanceled(true);
                player.sendSystemMessage(Component.literal("Você não tem permissão para deletar esta loja"));
                return;
            }
        }

        try {
            ShopService.delete(shop);
            player.sendSystemMessage(Component.literal("Loja deletada com sucesso"));
        } catch (Exception e) {
            event.setCanceled(true);
            LOGGER.error("Failed to delete shop: {}", e.getMessage());
        }
    }

    private static boolean hasAttachedShops(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        net.minecraft.core.BlockPos[] neighbors = {pos.above(), pos.north(), pos.south(), pos.east(), pos.west()};
        for (var attachPos : neighbors) {
            if (!(level.getBlockEntity(attachPos) instanceof SignBlockEntity)) continue;
            var state = level.getBlockState(attachPos);

            if (state.getBlock() instanceof net.minecraft.world.level.block.WallSignBlock) {
                var dir = state.getValue(net.minecraft.world.level.block.WallSignBlock.FACING);
                if (attachPos.relative(dir.getOpposite()).equals(pos)) {
                    if (ShopService.get(new Vec3(attachPos.getX(), attachPos.getY(), attachPos.getZ())) != null)
                        return true;
                }
            } else if (state.getBlock() instanceof net.minecraft.world.level.block.StandingSignBlock) {
                if (attachPos.equals(pos.above())) {
                    if (ShopService.get(new Vec3(attachPos.getX(), attachPos.getY(), attachPos.getZ())) != null)
                        return true;
                }
            }
        }
        return false;
    }
}
