package dev.glicio.commands.economy;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import dev.glicio.database.DatabaseHelper;
import dev.glicio.database.PlayerDao;
import dev.glicio.model.GPlayer;
import dev.glicio.service.PlayerService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.sql.Connection;

public class PayCommand {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var cmd = Commands.literal("pay")
            .requires(source -> source.hasPermission(0))
            .then(Commands.argument("player", EntityArgument.player())
            .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
            .executes(PayCommand::execute)));
        dispatcher.register(cmd);
        dispatcher.register(Commands.literal("pagar")
            .requires(source -> source.hasPermission(0))
            .then(Commands.argument("player", EntityArgument.player())
            .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
            .executes(PayCommand::execute))));
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        Player sender = context.getSource().getPlayer();
        try {
            Player recipient = EntityArgument.getPlayer(context, "player");
            double amount = DoubleArgumentType.getDouble(context, "amount");

            if (sender == null) return 0;

            int amountInCents = (int) Math.round(amount * 100);
            if (amountInCents <= 0) {
                sender.sendSystemMessage(Component.literal("§cValor inválido"));
                return 0;
            }
            if (sender.getUUID().equals(recipient.getUUID())) {
                sender.sendSystemMessage(Component.literal("§cVocê não pode pagar a si mesmo"));
                return 0;
            }

            GPlayer senderGPlayer = PlayerService.get(sender.getUUID().toString());
            GPlayer recipientGPlayer = PlayerService.get(recipient.getUUID().toString());

            if (senderGPlayer == null) {
                sender.sendSystemMessage(Component.literal("§cErro ao buscar seu saldo"));
                return 0;
            }
            if (recipientGPlayer == null) {
                sender.sendSystemMessage(Component.literal("§cJogador não encontrado"));
                return 0;
            }

            // Synchronized check-and-deduct on sender
            synchronized (senderGPlayer) {
                if (senderGPlayer.getBalance() < amountInCents) {
                    sender.sendSystemMessage(Component.literal("§cVocê não tem dinheiro suficiente"));
                    return 0;
                }
                senderGPlayer.addBalance(-amountInCents);
            }
            recipientGPlayer.addBalance(amountInCents);

            // Persist atomically — rollback in-memory on failure
            try (Connection con = DatabaseHelper.getConnection()) {
                con.setAutoCommit(false);
                PlayerDao.saveBalance(sender.getUUID().toString(), senderGPlayer.getBalance(), con);
                PlayerDao.saveBalance(recipient.getUUID().toString(), recipientGPlayer.getBalance(), con);
                con.commit();
                senderGPlayer.clearDirty();
                recipientGPlayer.clearDirty();
            } catch (Exception e) {
                senderGPlayer.addBalance(amountInCents);
                recipientGPlayer.addBalance(-amountInCents);
                LOGGER.error("DB error during payment: {}", e.getMessage());
                sender.sendSystemMessage(Component.literal("§cErro ao processar o pagamento. Tente novamente."));
                return 0;
            }

            String formatted = String.format("%.2f", amount);
            sender.sendSystemMessage(Component.literal("§aVocê enviou $" + formatted + " para " + recipient.getName().getString()));
            recipient.sendSystemMessage(Component.literal("§aVocê recebeu $" + formatted + " de " + sender.getName().getString()));
            return 1;

        } catch (Exception e) {
            if (sender != null) {
                String msg = "No player was found".equals(e.getMessage())
                    ? "§cO jogador não está online"
                    : "§cErro ao processar o pagamento";
                sender.sendSystemMessage(Component.literal(msg));
            }
            return 0;
        }
    }
}
