package dev.glicio.commands.economy;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import dev.glicio.database.DatabaseHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BalanceTopCommand {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PLAYERS_PER_PAGE = 10;
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register both 'baltop' and 'maisricos' commands with optional page argument
        var baltopCommand = Commands.literal("baltop")
                .requires(source -> source.hasPermission(0))
                .executes(BalanceTopCommand::execute);
                
        var maisricosCommand = Commands.literal("maisricos")
                .requires(source -> source.hasPermission(0))
                .executes(BalanceTopCommand::execute);
                
        // Add page argument to both commands
        baltopCommand.then(Commands.argument("page", IntegerArgumentType.integer(1))
                .executes(context -> execute(context, IntegerArgumentType.getInteger(context, "page"))));
                
        maisricosCommand.then(Commands.argument("page", IntegerArgumentType.integer(1))
                .executes(context -> execute(context, IntegerArgumentType.getInteger(context, "page"))));
                
        dispatcher.register(baltopCommand);
        dispatcher.register(maisricosCommand);
    }
    
    private static int execute(CommandContext<CommandSourceStack> context) {
        return execute(context, 1); // Default to first page
    }
    
    private static int execute(CommandContext<CommandSourceStack> context, int page) {
        Player player = context.getSource().getPlayer();
        if (player == null) {
            LOGGER.error("This command can only be executed as a player");
            return 0;
        }
        
        // Get total count of players
        int totalPlayers = getTotalPlayerCount();
        
        // If no players, show appropriate message
        if (totalPlayers == 0) {
            player.sendSystemMessage(Component.literal("§cNenhum jogador encontrado"));
            return 0;
        }
        
        // Calculate total pages (at least 1 page if there are any players)
        int totalPages = Math.max(1, (int) Math.ceil(totalPlayers / (double) PLAYERS_PER_PAGE));
        
        // Validate page number
        if (page > totalPages) {
            player.sendSystemMessage(Component.literal("§cPágina inválida. Total de páginas: " + totalPages));
            return 0;
        }
        
        List<TopPlayer> topPlayers = getTopPlayers(page);
        
        if (topPlayers.isEmpty()) {
            player.sendSystemMessage(Component.literal("Erro ao buscar os jogadores mais ricos"));
            return 0;
        }
        
        // Send header with page info
        player.sendSystemMessage(Component.literal(String.format("§6=== Jogadores Mais Ricos (Página %d/%d) ===", page, totalPages)));
        
        // Send each player's balance
        int startRank = ((page - 1) * PLAYERS_PER_PAGE) + 1;
        for (int i = 0; i < topPlayers.size(); i++) {
            TopPlayer tp = topPlayers.get(i);
            String message = String.format("§e%d. §f%s: §a$%s", startRank + i, tp.name, formatBalance(tp.balance));
            player.sendSystemMessage(Component.literal(message));
        }
        
        // Send footer with navigation help if there are multiple pages
        if (totalPages > 1) {
            String navHelp = String.format("§7Use /baltop %d para ver a próxima página", Math.min(page + 1, totalPages));
            if (page > 1) {
                navHelp = String.format("§7Use /baltop %d para ver a página anterior", page - 1) + "\n" + navHelp;
            }
            player.sendSystemMessage(Component.literal(navHelp));
        }
        
        return 1;
    }
    
    private static int getTotalPlayerCount() {
        try (Connection con = DatabaseHelper.getConnection()) {
            PreparedStatement stmt = con.prepareStatement("SELECT COUNT(*) as count FROM player WHERE balance > 0");
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            LOGGER.error("Database error while getting player count: {}", e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error while getting player count: {}", e.getMessage());
        }
        return 0;
    }
    
    private static List<TopPlayer> getTopPlayers(int page) {
        List<TopPlayer> topPlayers = new ArrayList<>();
        
        try (Connection con = DatabaseHelper.getConnection()) {
            PreparedStatement stmt = con.prepareStatement(
                "SELECT player_name, balance FROM player WHERE balance > 0 ORDER BY balance DESC OFFSET ? LIMIT ?"
            );
            stmt.setInt(1, (page - 1) * PLAYERS_PER_PAGE);
            stmt.setInt(2, PLAYERS_PER_PAGE);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                topPlayers.add(new TopPlayer(
                    rs.getString("player_name"),
                    rs.getInt("balance")
                ));
            }
        } catch (SQLException e) {
            LOGGER.error("Database error while getting top players: {}", e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error while getting top players: {}", e.getMessage());
        }
        
        return topPlayers;
    }
    
    private static String formatBalance(int balance) {
        return String.format("%.2f", balance / 100.0);
    }
    
    private static class TopPlayer {
        final String name;
        final int balance;
        
        TopPlayer(String name, int balance) {
            this.name = name;
            this.balance = balance;
        }
    }
} 