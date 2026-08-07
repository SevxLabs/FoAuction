package me.foesio.foAuction.utils;

import me.foesio.core.sound.SoundTypes;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class SoundFeedback {
    private SoundFeedback() {
    }

    public static void listingCreated(Player player) {
        play(player, "ENTITY_EXPERIENCE_ORB_PICKUP", 0.7F, 1.25F);
    }

    public static void menuOpen(Player player) {
        play(player, "BLOCK_BARREL_OPEN", 0.65F, 1.1F);
    }

    public static void pageTurn(Player player) {
        play(player, "ITEM_BOOK_PAGE_TURN", 0.7F, 1.0F);
    }

    public static void toggle(Player player) {
        play(player, "UI_BUTTON_CLICK", 0.6F, 1.15F);
    }

    public static void refresh(Player player) {
        play(player, "BLOCK_BEACON_AMBIENT", 0.55F, 1.35F);
    }

    public static void previewOpen(Player player) {
        play(player, "BLOCK_SHULKER_BOX_OPEN", 0.7F, 1.0F);
    }

    public static void searchUpdated(Player player) {
        play(player, "BLOCK_NOTE_BLOCK_PLING", 0.6F, 1.35F);
    }

    public static void searchCleared(Player player) {
        play(player, "BLOCK_NOTE_BLOCK_PLING", 0.6F, 0.9F);
    }

    public static void adminInfo(Player player) {
        play(player, "BLOCK_AMETHYST_BLOCK_CHIME", 0.7F, 1.1F);
    }

    public static void insufficientFunds(Player player) {
        play(player, "BLOCK_ANVIL_LAND", 0.55F, 1.45F);
    }

    public static void purchaseSuccess(Player player) {
        play(player, "ENTITY_PLAYER_LEVELUP", 0.6F, 1.45F);
    }

    public static void listingCancelled(Player player) {
        play(player, "BLOCK_CHEST_CLOSE", 0.8F, 1.0F);
    }

    public static void claimSuccess(Player player) {
        play(player, "ENTITY_ITEM_PICKUP", 0.8F, 1.0F);
    }

    public static void expiredClaimSuccess(Player player) {
        play(player, "BLOCK_AMETHYST_BLOCK_RESONATE", 0.7F, 1.15F);
    }

    public static void sellingConfirm(Player player) {
        play(player, "BLOCK_ENCHANTMENT_TABLE_USE", 0.7F, 1.1F);
    }

    public static void adminAction(Player player) {
        play(player, "ITEM_TRIDENT_RETURN", 0.65F, 1.2F);
    }

    public static void denied(Player player) {
        play(player, "ENTITY_VILLAGER_NO", 0.75F, 1.0F);
    }

    private static void play(Player player, String soundName, float volume, float pitch) {
        if (player == null || !player.isOnline()) {
            return;
        }
        Sound sound = sound(soundName);
        if (sound == null) {
            return;
        }
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    private static Sound sound(String soundName) {
        return SoundTypes.resolve(soundName).orElse(null);
    }
}
