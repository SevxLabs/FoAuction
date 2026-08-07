package me.foesio.foAuction.gui.holder;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class ContainerPreviewHolder implements InventoryHolder {
    private final UUID viewerUuid;
    private final UUID sellerUuid;
    private final int returnPage;
    private final Inventory inventory;

    public ContainerPreviewHolder(UUID viewerUuid, int returnPage, String title) {
        this(viewerUuid, null, returnPage, title);
    }

    public ContainerPreviewHolder(UUID viewerUuid, UUID sellerUuid, int returnPage, String title) {
        this.viewerUuid = viewerUuid;
        this.sellerUuid = sellerUuid;
        this.returnPage = returnPage;
        this.inventory = Bukkit.createInventory(this, 45, title);
    }

    public UUID getViewerUuid() {
        return viewerUuid;
    }

    public int getReturnPage() {
        return returnPage;
    }

    public UUID getSellerUuid() {
        return sellerUuid;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
