package dev.glicio.model;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;

import static dev.glicio.utils.StringIntParser.convertStringToInt;

public class ShopSign {

    private static final String[] VALID_TITLE = {"loja", "shop"};

    public enum ShopType {
        Vender,
        Comprar
    }

    private int id;
    private ShopType type;
    private int value;
    private int quantity;
    private ResourceLocation item;
    private Vec3 pos;
    private String owner;
    private SignBlockEntity sign;
    private boolean isAdmin;

    public static boolean isSignShop(SignBlockEntity sign) {
        Component[] messages = sign.getFrontText().getMessages(false);
        if (messages.length != 4) return false;
        if (Arrays.stream(VALID_TITLE).noneMatch(t -> messages[0].getString().equals(t))) return false;

        String[] parts = messages[1].getString().replace(":", " ").replace(",", " ").trim().split("\\s+");
        if (parts.length != 2) return false;
        if (!parts[0].trim().equalsIgnoreCase("Vender") && !parts[0].trim().equalsIgnoreCase("Comprar")) return false;

        try {
            if (Integer.parseInt(parts[1].trim()) <= 0) return false;
        } catch (NumberFormatException e) {
            return false;
        }

        try {
            return BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(messages[3].getString().toLowerCase()));
        } catch (Exception e) {
            return false;
        }
    }

    public ShopSign(SignBlockEntity sign, boolean isAdmin, String owner) {
        Component[] messages = sign.getFrontText().getMessages(false);
        if (messages.length != 4) throw new IllegalArgumentException("NOT_SHOP");

        String titleText = messages[0].getString().replace("[", "").replace("]", "");
        if (Arrays.stream(VALID_TITLE).noneMatch(titleText::equalsIgnoreCase))
            throw new IllegalArgumentException("NOT_SHOP");

        String[] parts = messages[1].getString().replace(":", " ").replace(",", " ").trim().split("\\s+");
        if (parts.length != 2)
            throw new IllegalArgumentException("Placa inválida: Formato inválido, use 'Tipo: Quantidade' ou 'Tipo Quantidade'");

        String type = parts[0].trim();
        if (!type.equalsIgnoreCase("vender") && !type.equalsIgnoreCase("comprar"))
            throw new IllegalArgumentException("Placa inválida: Tipo inválido, use vender ou comprar");

        this.type = type.equalsIgnoreCase("vender") ? ShopType.Vender : ShopType.Comprar;

        try {
            this.quantity = Integer.parseInt(parts[1].trim());
            if (this.quantity <= 0) throw new IllegalArgumentException("Quantidade inválida");
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Placa inválida: Quantidade inválida");
        }

        this.isAdmin = isAdmin;

        try {
            int v = convertStringToInt(messages[2].getString());
            if (v <= 0 || v == Integer.MAX_VALUE) throw new IllegalArgumentException("Valor inválido");
            this.value = v;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Placa inválida: Valor inválido");
        }

        try {
            ResourceLocation loc = ResourceLocation.parse(messages[3].getString().toLowerCase());
            if (!BuiltInRegistries.ITEM.containsKey(loc))
                throw new IllegalArgumentException(String.format("Placa inválida: Item %s não encontrado", messages[3].getString()));
            this.item = loc;
        } catch (Exception e) {
            throw new IllegalArgumentException("Placa inválida: Nome do Item inválido");
        }

        this.owner = owner;
        this.sign = sign;
        this.pos = new Vec3(sign.getBlockPos().getX(), sign.getBlockPos().getY(), sign.getBlockPos().getZ());
    }

    public ShopSign(int id, Vec3 pos, String owner, int value, ResourceLocation item, boolean isAdmin) {
        this.id = id; this.pos = pos; this.owner = owner;
        this.value = value; this.item = item; this.isAdmin = isAdmin;
    }

    public ShopSign(int id, Vec3 pos, String owner, int value, ResourceLocation item, boolean isAdmin, ShopType type) {
        this(id, pos, owner, value, item, isAdmin);
        this.type = type;
    }

    public ShopType getShopType() { return type; }
    public void setShopType(ShopType type) { this.type = type; }
    public boolean isAdmin() { return isAdmin; }
    public int getValue() { return value; }
    public ResourceLocation getItem() { return item; }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Vec3 getPos() { return pos; }
    public String getOwner() { return owner; }
    public SignBlockEntity getSign() { return sign; }
    public void setSign(SignBlockEntity sign) { this.sign = sign; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    @Override
    public String toString() {
        return String.format("ShopSign{id=%d, pos=%s, owner=%s, value=%d, item=%s, isAdmin=%b}", id, pos, owner, value, item, isAdmin);
    }
}
