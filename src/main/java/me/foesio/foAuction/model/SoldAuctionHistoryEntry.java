package me.foesio.foAuction.model;

import me.foesio.foAuction.storage.DatabaseManager;
import me.foesio.core.item.FoItemStacks;
import org.bukkit.inventory.ItemStack;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SoldAuctionHistoryEntry {
    private final UUID sellerUuid;
    private final UUID buyerUuid;
    private final ItemStack item;
    private final double price;
    private final long soldAt;

    public SoldAuctionHistoryEntry(
            UUID sellerUuid,
            UUID buyerUuid,
            ItemStack item,
            double price,
            long soldAt
    ) {
        this.sellerUuid = sellerUuid;
        this.buyerUuid = buyerUuid;
        this.item = item.clone();
        this.price = price;
        this.soldAt = soldAt;
    }

    public UUID getSellerUuid() {
        return sellerUuid;
    }

    public UUID getBuyerUuid() {
        return buyerUuid;
    }

    public ItemStack getItem() {
        return item.clone();
    }

    public double getPrice() {
        return price;
    }

    public long getSoldAt() {
        return soldAt;
    }

    public SoldAuctionHistoryEntry copy() {
        return new SoldAuctionHistoryEntry(sellerUuid, buyerUuid, item, price, soldAt);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("seller", sellerUuid.toString());
        map.put("buyer", buyerUuid.toString());
        map.put("item", item.clone());
        map.put("price", price);
        map.put("soldAt", soldAt);
        return map;
    }

    public static SoldAuctionHistoryEntry fromMap(Map<?, ?> map, UUID ownerUuid) {
        Object sellerObject = map.get("seller");
        Object buyerObject = firstPresent(map, "buyer", "buyerUuid", "buyer_name", "buyerName");
        Object itemObject = map.get("item");
        Object priceObject = map.get("price");
        Object soldAtObject = firstPresent(map, "soldAt", "sold_time", "soldTime");

        if (!(itemObject instanceof ItemStack itemStack)
                || !(priceObject instanceof Number priceNumber)
                || !(soldAtObject instanceof Number soldAtNumber)) {
            return null;
        }

        try {
            UUID sellerUuid = parseUuidOrFallback(sellerObject, ownerUuid, "legacy-seller");
            UUID buyerUuid = parseUuidOrFallback(buyerObject, null, "legacy-buyer");
            if (buyerUuid == null) {
                return null;
            }
            double price = priceNumber.doubleValue();
            long soldAt = soldAtNumber.longValue();
            if (itemStack.getType().isAir()
                    || itemStack.getAmount() <= 0
                    || !Double.isFinite(price)
                    || price < 0D
                    || soldAt <= 0L) {
                return null;
            }
            return new SoldAuctionHistoryEntry(
                    sellerUuid,
                    buyerUuid,
                    itemStack,
                    price,
                    soldAt
            );
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static Object firstPresent(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public void saveToDatabase(DatabaseManager dbManager, Connection connection, UUID playerUuid) throws SQLException {
        String itemData = serializeItemStack(item);
        if (itemData.isEmpty()) {
            throw new SQLException("Could not serialize sold auction history item");
        }
        dbManager.executeUpdate(
            connection,
            "INSERT OR REPLACE INTO sold_history (player_uuid, item_data, price, buyer_name, sold_time) VALUES (?, ?, ?, ?, ?)",
            playerUuid.toString(),
            itemData,
            price,
            buyerUuid.toString(),
            soldAt
        );
    }

    public static SoldAuctionHistoryEntry fromDatabaseRow(ResultSet rs, UUID ownerUuid) throws SQLException {
        try {
            String itemData = rs.getString("item_data");
            ItemStack item = deserializeItemStack(itemData);
            if (item == null) {
                return null;
            }

            double price = rs.getDouble("price");
            String buyerName = rs.getString("buyer_name");
            long soldAt = rs.getLong("sold_time");

            if (!Double.isFinite(price) || price < 0D || soldAt <= 0L) {
                return null;
            }

            UUID buyerUuid = parseUuidOrFallback(buyerName, null, "legacy-buyer");
            if (buyerUuid == null) {
                return null;
            }
            return new SoldAuctionHistoryEntry(
                ownerUuid,
                buyerUuid,
                item,
                price,
                soldAt
            );
        } catch (Exception e) {
            return null;
        }
    }

    private static UUID parseUuidOrFallback(Object rawValue, UUID fallback, String namespace) {
        if (rawValue == null) {
            return fallback;
        }

        String rawString = String.valueOf(rawValue).trim();
        if (rawString.isEmpty()) {
            return fallback;
        }

        try {
            return UUID.fromString(rawString);
        } catch (IllegalArgumentException ignored) {
            return UUID.nameUUIDFromBytes((namespace + ":" + rawString.toLowerCase()).getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String serializeItemStack(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return "";
        }
        try {
            return FoItemStacks.encode(item);
        } catch (RuntimeException exception) {
            return "";
        }
    }

    private static ItemStack deserializeItemStack(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        ItemStack coreItem = FoItemStacks.decode(data).orElse(null);
        if (coreItem != null && !coreItem.getType().isAir() && coreItem.getAmount() > 0) {
            return coreItem;
        }
        return deserializeLegacyItemStack(data);
    }

    private static ItemStack deserializeLegacyItemStack(String data) {
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            try (org.bukkit.util.io.BukkitObjectInputStream bois = new org.bukkit.util.io.BukkitObjectInputStream(new java.io.ByteArrayInputStream(bytes))) {
                Object obj = bois.readObject();
                if (obj instanceof ItemStack) {
                    return (ItemStack) obj;
                }
            }
        } catch (Exception e) {
            // Ignore deserialization errors
        }
        return null;
    }
}
