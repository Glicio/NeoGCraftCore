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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BalanceTopCommand {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PLAYERS_PER_PAGE = 10;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var cmd = Commands.literal("baltop")
            .requires(source -> source.hasPermission(0))
            .executes(BalanceTopCommand::execute)
            .then(Commands.argument("page", IntegerArgumentType.integer(1))
                .executes(ctx -> execute(ctx, IntegerArgumentType.getInteger(ctx, "page"))));
        dispatcher.register(cmd);

        var cmdPt = Commands.literal("maisricos")
            .requires(source -> source.hasPermission(0))
            .executes(BalanceTopCommand::execute)
            .then(Commands.argument("page", IntegerArgumentType.integer(1))
                .executes(ctx -> execute(ctx, IntegerArgumentType.getInteger(ctx, "page"))));
        dispatcher.register(cmdPt);
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        return execute(context, 1);
    }

    private static int execute(CommandContext<CommandSourceStack> context, int page) {
        Player player = context.getSource().getPlayer();
        if (player == null) return 0;

        int totalPlayers = getTotalPlayerCount();
        if (totalPlayers == 0) {
            player.sendSystemMessage(Component.literal("§cNenhum jogador encontrado"));
            return 0;
        }

        int totalPages = Math.max(1, (int) Math.ceil(totalPlayers / (double) PLAYERS_PER_PAGE));
        if (page > totalPages) {
            player.sendSystemMessage(Component.literal("§cPágina inválida. Total de páginas: " + totalPages));
            return 0;
        }

        List<TopPlayer> topPlayers = getTopPlayers(page);
        if (topPlayers.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cErro ao buscar jogadores"));
            return 0;
        }

        player.sendSystemMessage(Component.literal(String.format("§6=== Jogadores Mais Ricos (Página %d/%d) ===", page, totalPages)));
        int startRank = (page - 1) * PLAYERS_PER_PAGE + 1;
        for (int i = 0; i < topPlayers.size(); i++) {
            TopPlayer tp = topPlayers.get(i);
            player.sendSystemMessage(Component.literal(
                String.format("§e%d. §f%s: §a$%s", startRank + i, tp.name, String.format("%.2f", tp.balance / 100.0))));
        }

        if (totalPages > 1) {
            if (page > 1) player.sendSystemMessage(Component.literal("§7Use /baltop " + (page - 1) + " para a página anterior"));
            if (page < totalPages) player.sendSystemMessage(Component.literal("§7Use /baltop " + (page + 1) + " para a próxima página"));
        }
        return 1;
    }

    private static int getTotalPlayerCount() {
        try (Connection con = DatabaseHelper.getConnection();
             var stmt = con.prepareStatement("SELECT COUNT(*) FROM player WHERE balance > 0");
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            LOGGER.error("Failed to get player count: {}", e.getMessage());
        }
        return 0;
    }

    private static List<TopPlayer> getTopPlayers(int page) {
        List<TopPlayer> result = new ArrayList<>();
        try (Connection con = DatabaseHelper.getConnection();
             var stmt = con.prepareStatement(
                 "SELECT player_name, balance FROM player WHERE balance > 0 ORDER BY balance DESC OFFSET ? LIMIT ?")) {
            stmt.setInt(1, (page - 1) * PLAYERS_PER_PAGE);
            stmt.setInt(2, PLAYERS_PER_PAGE);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) result.add(new TopPlayer(rs.getString("player_name"), rs.getInt("balance")));
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to get top players: {}", e.getMessage());
        }
        return result;
    }

    private record TopPlayer(String name, int balance) {}
}
