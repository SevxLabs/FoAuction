package me.foesio.foAuction.utils;

import me.foesio.core.sound.FoSoundService;
import me.foesio.core.sound.FoGuiSounds;
import org.bukkit.entity.Player;

public final class SoundFeedback {
    private static volatile FoSoundService sounds;
    private static volatile FoGuiSounds guiSounds;

    private SoundFeedback() {
    }

    public static void configure(FoSoundService soundService) {
        sounds = soundService;
    }

    public static void configureGui(FoGuiSounds guiSoundService) {
        guiSounds = guiSoundService;
    }

    public static void listingCreated(Player player) {
        play(player, "auction.listing-created");
    }

    public static void menuOpen(Player player) {
        playGui(player, FoGuiSounds::open);
    }

    public static void click(Player player) {
        playGui(player, FoGuiSounds::click);
    }

    public static void back(Player player) {
        playGui(player, FoGuiSounds::back);
    }

    public static void nextPage(Player player) {
        playGui(player, FoGuiSounds::nextPage);
    }

    public static void previousPage(Player player) {
        playGui(player, FoGuiSounds::previousPage);
    }

    public static void search(Player player) {
        playGui(player, FoGuiSounds::search);
    }

    public static void clearSearch(Player player) {
        playGui(player, FoGuiSounds::clearSearch);
    }

    public static void sort(Player player) {
        playGui(player, FoGuiSounds::sort);
    }

    public static void filter(Player player) {
        playGui(player, FoGuiSounds::filter);
    }

    public static void confirm(Player player) {
        playGui(player, FoGuiSounds::confirm);
    }

    public static void cancel(Player player) {
        playGui(player, FoGuiSounds::cancel);
    }

    public static void insufficientFunds(Player player) {
        play(player, "auction.insufficient-funds");
    }

    public static void purchaseSuccess(Player player) {
        play(player, "auction.purchase-success");
    }

    public static void listingCancelled(Player player) {
        play(player, "auction.listing-cancelled");
    }

    public static void claimSuccess(Player player) {
        play(player, "auction.claim-success");
    }

    public static void expiredClaimSuccess(Player player) {
        play(player, "auction.expired-claim-success");
    }

    public static void listingExpired(Player player) {
        play(player, "auction.listing-expired");
    }

    public static void listingRemoved(Player player) {
        play(player, "auction.listing-removed");
    }

    public static void pendingNotifications(Player player) {
        play(player, "auction.pending-notifications");
    }

    public static void adminAction(Player player) {
        play(player, "auction.admin-action");
    }

    public static void denied(Player player) {
        playGui(player, FoGuiSounds::error);
    }

    private static void playGui(Player player, GuiSoundAction action) {
        FoGuiSounds service = guiSounds;
        if (service != null && player != null && player.isOnline()) {
            action.play(service, player);
        }
    }

    private static void play(Player player, String path) {
        FoSoundService service = sounds;
        if (service != null && player != null && player.isOnline()) {
            service.play(player, path);
        }
    }

    @FunctionalInterface
    private interface GuiSoundAction {
        boolean play(FoGuiSounds sounds, Player player);
    }
}
