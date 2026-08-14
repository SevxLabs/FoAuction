package me.foesio.foAuction.listeners;

import me.foesio.core.FoCoreContext;
import me.foesio.core.editor.EditorMenuHolder;
import me.foesio.core.gui.EntryBrowserHolder;
import me.foesio.core.material.MaterialChooserHolder;
import me.foesio.foAuction.gui.editor.AdminEditorHolder;
import me.foesio.foAuction.gui.editor.AdminEditorManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.function.Supplier;

public final class AdminEditorListener implements Listener {
    private final AdminEditorManager editorManager;
    private final Supplier<FoCoreContext> coreProvider;

    public AdminEditorListener(
            AdminEditorManager editorManager,
            Supplier<FoCoreContext> coreProvider
    ) {
        this.editorManager = editorManager;
        this.coreProvider = coreProvider;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory topInventory = event.getView().getTopInventory();
        InventoryHolder holder = topInventory.getHolder();
        if (!(holder instanceof AdminEditorHolder)
                && !(holder instanceof EditorMenuHolder)
                && !(holder instanceof MaterialChooserHolder)
                && !(holder instanceof EntryBrowserHolder)) {
            return;
        }

        if (event.getClickedInventory() == null) {
            event.setCancelled(true);
            return;
        }

        if (!event.getClickedInventory().equals(topInventory)) {
            InventoryAction action = event.getAction();
            if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY || action == InventoryAction.COLLECT_TO_CURSOR) {
                event.setCancelled(true);
            }
            return;
        }

        event.setCancelled(true);
        int slot = event.getRawSlot();
        ItemStack cursor = event.getCursor() == null ? null : event.getCursor().clone();
        InventoryHolder expectedHolder = holder;
        FoCoreContext core = coreProvider.get();
        core.scheduler().runForPlayer(player, () -> {
            if (player.getOpenInventory().getTopInventory().getHolder() != expectedHolder) {
                return;
            }
            if (expectedHolder instanceof AdminEditorHolder adminEditorHolder) {
                editorManager.handleClick(player, adminEditorHolder, slot, cursor);
            } else if (expectedHolder instanceof EditorMenuHolder editorMenuHolder) {
                editorManager.handleConfigEditorClick(player, editorMenuHolder, slot);
            } else if (expectedHolder instanceof MaterialChooserHolder materialChooserHolder) {
                editorManager.handleMaterialChooserClick(player, materialChooserHolder, slot);
            } else if (expectedHolder instanceof EntryBrowserHolder entryBrowserHolder) {
                editorManager.handleEntryBrowserClick(player, entryBrowserHolder, slot, event.getClick());
            }
        });
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        InventoryHolder holder = topInventory.getHolder();
        if (!(holder instanceof AdminEditorHolder)
                && !(holder instanceof EditorMenuHolder)
                && !(holder instanceof MaterialChooserHolder)
                && !(holder instanceof EntryBrowserHolder)) {
            return;
        }

        int topSize = topInventory.getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        FoCoreContext core = coreProvider.get();
        if (core != null && core.inventoryCloseSuppressor().consumeSuppressedClose(player)) {
            return;
        }

        if (event.getInventory().getHolder() instanceof AdminEditorHolder holder
                && holder.getPage() == AdminEditorHolder.Page.CONFIRM_REMOVE_NAME) {
            editorManager.handleConfirmationClose(player, holder);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        editorManager.clear(event.getPlayer());
    }
}
