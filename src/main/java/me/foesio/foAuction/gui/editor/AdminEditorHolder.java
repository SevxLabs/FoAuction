package me.foesio.foAuction.gui.editor;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AdminEditorHolder implements InventoryHolder {
    public enum Page {
        MAIN,
        AUCTION,
        DISCORD,
        BLACKLIST,
        NAMES,
        CONFIRM_REMOVE_NAME
    }

    private final UUID viewerUuid;
    private final Page page;
    private final int listPage;
    private final String value;
    private final Inventory inventory;
    private final Map<Integer, String> valuesBySlot;

    public AdminEditorHolder(UUID viewerUuid, Page page, int size, String title) {
        this(viewerUuid, page, size, title, 0, "");
    }

    public AdminEditorHolder(UUID viewerUuid, Page page, int size, String title, int listPage, String value) {
        this.viewerUuid = viewerUuid;
        this.page = page;
        this.listPage = listPage;
        this.value = value == null ? "" : value;
        this.inventory = Bukkit.createInventory(this, size, title);
        this.valuesBySlot = new HashMap<>();
    }

    public UUID getViewerUuid() {
        return viewerUuid;
    }

    public Page getPage() {
        return page;
    }

    public int getListPage() {
        return listPage;
    }

    public String getValue() {
        return value;
    }

    public void mapValue(int slot, String mappedValue) {
        valuesBySlot.put(slot, mappedValue);
    }

    public String getValue(int slot) {
        return valuesBySlot.get(slot);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
