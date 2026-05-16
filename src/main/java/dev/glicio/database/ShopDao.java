package dev.glicio.database;

import dev.glicio.model.ShopSign;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ShopDao {

    public static void createTable(Connection connection) throws SQLException {
        try (var stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS shop (
                    id SERIAL PRIMARY KEY,
                    posX INTEGER NOT NULL,
                    posY INTEGER NOT NULL,
                    posZ INTEGER NOT NULL,
                    owner TEXT NOT NULL,
                    value INTEGER NOT NULL,
                    item TEXT NOT NULL,
                    is_admin BOOLEAN NOT NULL DEFAULT FALSE,
                    quantity INTEGER NOT NULL DEFAULT 1,
                    shop_type TEXT NOT NULL
                )
            """);
        }
    }

    public static List<ShopSign> loadAll(Connection connection) throws SQLException {
        List<ShopSign> shops = new ArrayList<>();
        try (var stmt = connection.prepareStatement("SELECT * FROM shop");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                ShopSign shop = new ShopSign(
                        rs.getInt("id"),
                        new Vec3(rs.getInt("posX"), rs.getInt("posY"), rs.getInt("posZ")),
                        rs.getString("owner"),
                        rs.getInt("value"),
                        ResourceLocation.parse(rs.getString("item")),
                        rs.getBoolean("is_admin"),
                        rs.getString("shop_type").equalsIgnoreCase("vender") ? ShopSign.ShopType.Vender : ShopSign.ShopType.Comprar
                );
                shop.setQuantity(rs.getInt("quantity"));
                shops.add(shop);
            }
        }
        return shops;
    }

    public static int save(ShopSign shop, Connection connection) throws SQLException {
        try (var stmt = connection.prepareStatement(
                "INSERT INTO shop (posX, posY, posZ, owner, value, item, is_admin, quantity, shop_type) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id")) {
            stmt.setInt(1, (int) shop.getPos().x);
            stmt.setInt(2, (int) shop.getPos().y);
            stmt.setInt(3, (int) shop.getPos().z);
            stmt.setString(4, shop.getOwner());
            stmt.setInt(5, shop.getValue());
            stmt.setString(6, shop.getItem().toString());
            stmt.setBoolean(7, shop.isAdmin());
            stmt.setInt(8, shop.getQuantity());
            stmt.setString(9, shop.getShopType().name());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    public static void delete(int shopId, Connection connection) throws SQLException {
        try (var stmt = connection.prepareStatement("DELETE FROM shop WHERE id = ?")) {
            stmt.setInt(1, shopId);
            stmt.execute();
        }
    }
}
