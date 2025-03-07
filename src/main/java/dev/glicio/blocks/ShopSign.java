package dev.glicio.blocks;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;

import static dev.glicio.utils.StringIntParser.convertStringToInt;


public class ShopSign{

    private static final String[] VALID_TITLE = new String[] {"loja", "shop" };
    public enum ShopType {
        Vender, //sell
        Comprar //buy
    }

    private int id;
    private ShopType type;
    private int value;
    private int quantity;
    private ResourceLocation item;
    private Vec3 pos;
    private String owner;
    private SignBlockEntity sign;
    /*
     * If this shop is an admin shop
     * Admin shops doesn't need an inventory thus having an unlimited stock
     */
    private boolean isAdmin;

    public static boolean isSignShop(SignBlockEntity sign) {
        SignText frontText = sign.getFrontText();
        Component[] messages = frontText.getMessages(false);
        if (messages.length != 4) {
            return false;
        }

        if (Arrays.stream(VALID_TITLE).noneMatch(title -> messages[0].getString().equals(title))) {
            return false;
        }

        String typeLine = messages[1].getString()
            .replace(":", " ")
            .replace(",", " ")
            .trim();
        String[] parts = typeLine.split("\\s+");

        if (parts.length != 2) {
            return false;
        }

        String type = parts[0].trim();
        if (!type.equalsIgnoreCase("Vender") && !type.equalsIgnoreCase("Comprar")) {
            return false;
        }

        try {
            int quantity = Integer.parseInt(parts[1].trim());
            if (quantity <= 0) {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }

        String itemName = messages[3].getString();
        ResourceLocation itemLocation;
        try {
            itemLocation = ResourceLocation.parse(itemName.toLowerCase());
        } catch (Exception e) {
            return false;
        }

        return BuiltInRegistries.ITEM.containsKey(itemLocation);
    }
    public ShopSign(SignBlockEntity sign, boolean isAdmin, String owner) {
        SignText frontText = sign.getFrontText();
        Component[] messages = frontText.getMessages(false);
        if(messages.length != 4) {
            throw new IllegalArgumentException("NOT_SHOP");
        }

        //remove []
        String titleText = messages[0].getString().replace("[", "").replace("]", "");

        if(Arrays.stream(VALID_TITLE).noneMatch(titleText::equalsIgnoreCase)) {
            throw new IllegalArgumentException("NOT_SHOP");
        }

        String typeLine = messages[1].getString()
            .replace(":", " ")
            .replace(",", " ")
            .trim();
        String[] parts = typeLine.split("\\s+");

        if (parts.length != 2) {
            throw new IllegalArgumentException("Placa inválida: Formato inválido, use 'Tipo: Quantidade' ou 'Tipo Quantidade'");
        }

        String type = parts[0].trim();
        if(!type.equalsIgnoreCase("vender") && !type.equalsIgnoreCase("comprar")) {
            throw new IllegalArgumentException("Placa inválida: Tipo inválido, use vender ou comprar");
        }
        if(type.equalsIgnoreCase("vender")) {
            this.type = ShopType.Vender;
        } else {
            this.type = ShopType.Comprar;
        }

        try {
            this.quantity = Integer.parseInt(parts[1].trim());
            if(this.quantity <= 0) {
                throw new IllegalArgumentException("Quantidade inválida");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Placa inválida: Quantidade inválida");
        }

        this.isAdmin = isAdmin;
        try {
            int value;
            value = convertStringToInt(messages[2].getString());
            if(value <= 0 || value == Integer.MAX_VALUE){
                throw new IllegalArgumentException("Valor inválido");
            }
            this.value = value;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Placa inválida: Valor inválido");
        }
        String itemName = messages[3].getString();
        ResourceLocation itemLocation;
        try {
            itemLocation = ResourceLocation.parse(itemName.toLowerCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Placa inválida: Nome do Item inválido");
        }

        if(!BuiltInRegistries.ITEM.containsKey(itemLocation)){
            throw new IllegalArgumentException(String.format("Placa inválida: Item %s não encontrado", itemName));
        }
        this.item = itemLocation;
        this.owner = owner;
        this.sign = sign;
        this.pos = new Vec3(sign.getBlockPos().getX(), sign.getBlockPos().getY(), sign.getBlockPos().getZ());
    }

    public ShopSign(int id, Vec3 pos, String owner, int value, ResourceLocation item, boolean isAdmin) {
        this.id = id;
        this.pos = pos;
        this.owner = owner;
        this.value = value;
        this.item = item;
        this.isAdmin = isAdmin;
    }

    public ShopSign(int id, Vec3 pos, String owner, int value, ResourceLocation item, boolean isAdmin, ShopType type) {
        this.id = id;
        this.pos = pos;
        this.owner = owner;
        this.value = value;
        this.item = item;
        this.isAdmin = isAdmin;
        this.type = type;
    }

    public ShopType getShopType() {
        return this.type;
    }

    public boolean isAdmin() {
        return this.isAdmin;
    }

    public int getValue() {
        return this.value;
    }

    public ResourceLocation getItem() {
        return this.item;
    }

    public int getId() {
        return this.id;
    }

    public Vec3 getPos() {
        return this.pos;
    }

    public String getOwner() {
        return this.owner;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String toString() {
        return String.format("ShopSign{id=%d, pos=%s, owner=%s, value=%d, item=%s, isAdmin=%b}", id, pos, owner, value, item, isAdmin);
    }

    public SignBlockEntity getSign() {
        return this.sign;
    }

    public void setSign(SignBlockEntity sign) {
        this.sign = sign;
    }

    public int getQuantity() {
        return this.quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setShopType(ShopType type) {
        this.type = type;
    }

}
