package dev.glicio.commands.economy;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import dev.glicio.GCraftCore;
import dev.glicio.GPlayer;
import dev.glicio.database.DatabaseHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;

public class PayCommand {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register both 'pay' and 'pagar' commands
        var payCommand = Commands.literal("pay")
                .requires(source -> source.hasPermission(0))
                .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                .executes(PayCommand::execute)));
                
        var pagarCommand = Commands.literal("pagar")
                .requires(source -> source.hasPermission(0))
                .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                .executes(PayCommand::execute)));
                
        dispatcher.register(payCommand);
        dispatcher.register(pagarCommand);
    }
    
    private static int execute(CommandContext<CommandSourceStack> context) {
        Player sender = context.getSource().getPlayer();
        try {
           
            Player recipient = EntityArgument.getPlayer(context, "player");
            double amount = DoubleArgumentType.getDouble(context, "amount");
            
            if (sender == null) {
                LOGGER.error("This command can only be executed as a player");
                return 0;
            }
            
            // Convert amount to cents (integer)
            int amountInCents = (int) Math.round(amount * 100);
            
            // Check if amount is valid
            if (amountInCents <= 0) {
                sender.sendSystemMessage(Component.literal("§cValor inválido"));
                return 0;
            }
            
            // Check if trying to pay themselves
            if (sender.getUUID().equals(recipient.getUUID())) {
                sender.sendSystemMessage(Component.literal("§cVocê não pode pagar a si mesmo"));
                return 0;
            }
            
            // Get sender's GPlayer
            GPlayer senderGPlayer = GCraftCore.getPlayer(sender.getUUID().toString());
            if (senderGPlayer == null) {
                sender.sendSystemMessage(Component.literal("§cErro ao buscar seu saldo"));
                return 0;
            }
            
            // Check if sender has enough money
            if (senderGPlayer.getBalance() < amountInCents) {
                sender.sendSystemMessage(Component.literal("§cVocê não tem dinheiro suficiente"));
                return 0;
            }
            
            // Get recipient's GPlayer
            GPlayer recipientGPlayer = GCraftCore.getPlayer(recipient.getUUID().toString());
            if (recipientGPlayer == null) {
                sender.sendSystemMessage(Component.literal("§cJogador não encontrado"));
                return 0;
            }
            
            // Process the transaction
            try (Connection con = DatabaseHelper.getConnection()) {
                con.setAutoCommit(false);
                
                // Update sender's balance
                senderGPlayer.addBalance(-amountInCents);
                dev.glicio.database.PlayerDb.updateBalance(sender.getUUID().toString(), -amountInCents, con);
                
                // Update recipient's balance
                recipientGPlayer.addBalance(amountInCents);
                dev.glicio.database.PlayerDb.updateBalance(recipient.getUUID().toString(), amountInCents, con);
                
                con.commit();
                
                // Send success messages
                String formattedAmount = String.format("%.2f", amount);
                sender.sendSystemMessage(Component.literal(String.format("§aVocê enviou $%s para %s", formattedAmount, recipient.getName().getString())));
                recipient.sendSystemMessage(Component.literal(String.format("§aVocê recebeu $%s de %s", formattedAmount, sender.getName().getString())));
                
                return 1;
            } catch (SQLException e) {
                LOGGER.error("Database error while processing payment: {}", e.getMessage());
                sender.sendSystemMessage(Component.literal("§cErro ao processar o pagamento. Por favor, tente novamente mais tarde."));
                return 0;
            } catch (Exception e) {
                LOGGER.error("Error while processing payment: {}", e.getMessage());
                sender.sendSystemMessage(Component.literal("§cErro ao processar o pagamento"));
                return 0;
            }
        } catch (Exception e) {
            if(e.getMessage().equals("No player was found")) {
                if(sender != null) {
                    sender.sendSystemMessage(Component.literal("§cO Jogador não está online"));
                }
            } else {
                LOGGER.error("Command execution failed: {}", e.getMessage());
                if(sender != null) {    
                    sender.sendSystemMessage(Component.literal("§cErro ao processar o pagamento"));
                }
            }
            return 0;
        }
    }
} 