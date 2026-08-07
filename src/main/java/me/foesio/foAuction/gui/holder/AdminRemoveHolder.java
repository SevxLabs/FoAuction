package me.foesio.foAuction.gui.holder;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class AdminRemoveHolder implements InventoryHolder {
    private final UUID viewerUuid;
    private final UUID listingId;
    private final int returnPage;
    private final Inventory inventory;

    public AdminRemoveHolder(UUID viewerUuid, UUID listingId, int returnPage, String title) {
        this.viewerUuid = viewerUuid;
        this.listingId = listingId;
        this.returnPage = returnPage;
        this.inventory = Bukkit.createInventory(this, 27, title);
    }

    public UUID getViewerUuid() {
        return viewerUuid;
    }

    public UUID getListingId() {
        return listingId;
    }

    public int getReturnPage() {
        return returnPage;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
