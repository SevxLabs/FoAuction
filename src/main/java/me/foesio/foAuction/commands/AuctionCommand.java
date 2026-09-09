package me.foesio.foAuction.commands;

import me.foesio.foAuction.config.AuctionSettings;
import me.foesio.foAuction.gui.AuctionGuiManager;
import me.foesio.foAuction.service.AuctionService;
import me.foesio.core.economy.VaultEconomyBridge;
import me.foesio.foAuction.utils.FormatUtils;
import me.foesio.foAuction.utils.InputValidationUtils;
import me.foesio.foAuction.utils.SoundFeedback;
import me.foesio.core.message.FoMessageService;
import me.foesio.core.sound.FoAdminSounds;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class AuctionCommand implements CommandExecutor, TabCompleter {
    private final AuctionGuiManager guiManager;
    private final AuctionService auctionService;
    private final VaultEconomyBridge economyService;
    private final AuctionSettings settings;
    private final FoMessageService messages;
    private final FoAdminSounds adminSounds;

    public AuctionCommand(
            AuctionGuiManager guiManager,
            AuctionService auctionService,
            VaultEconomyBridge economyService,
            AuctionSettings settings,
            FoMessageService messages,
            FoAdminSounds adminSounds
    ) {
        this.guiManager = guiManager;
        this.auctionService = auctionService;
        this.economyService = economyService;
        this.settings = settings;
        this.messages = messages;
        this.adminSounds = adminSounds;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("foauction.use")) {
            messages.sendConfigured(sender, "command.no-permission", "label", label);
            adminSounds.updateError(sender);
            return true;
        }
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                messages.sendConfigured(sender, "command.only-players-open-gui");
                return true;
            }

            guiManager.openMainGui(player, 0);
            SoundFeedback.menuOpen(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "sell" -> {
                if (!(sender instanceof Player player)) {
                    messages.sendConfigured(sender, "command.only-players-sell");
                    return true;
                }

                if (args.length < 2) {
                    messages.sendConfigured(sender, "command.usage-sell", "label", label);
                    adminSounds.updateError(sender);
                    return true;
                }

                double price;
                try {
                    price = InputValidationUtils.validatePrice(args[1]);
                } catch (InputValidationUtils.InvalidInputException exception) {
                    messages.sendConfigured(sender, "command.invalid-price", "error", exception.getMessage());
                    adminSounds.updateError(player);
                    return true;
                }

                // Check if confirmation GUI is enabled
                if (settings.isConfirmationGuiEnabled()) {
                      ItemStack item = player.getInventory().getItemInMainHand();
                      if (item == null || item.getType().isAir()) {
                          messages.sendConfigured(sender, "command.hold-item");
                          adminSounds.updateError(player);
                          return true;
                      }

                    // Validate price range before showing GUI
                      if (price < settings.getMinPrice() || price > settings.getMaxPrice()) {
                          messages.sendConfigured(
                                  sender,
                                  "command.price-range",
                                  "min", formatDisplayPrice(settings.getMinPrice()),
                                  "max", formatDisplayPrice(settings.getMaxPrice())
                          );
                          adminSounds.updateError(player);
                          return true;
                      }

                    double listingFee = price * (settings.getListingFeePercent() / 100.0);
                    guiManager.openConfirmationGui(player, item, price, listingFee);
                    SoundFeedback.menuOpen(player);
                    return true;
                }

                AuctionService.SellResult result = auctionService.createListing(player, price);
                switch (result.type()) {
                      case SUCCESS -> {
                          messages.sendConfigured(
                                  sender,
                                  "command.listed",
                                  "price", formatEconomyPrice(result.listing().getPrice()),
                                  "active", String.valueOf(auctionService.getActiveCount(player.getUniqueId())),
                                  "max", String.valueOf(result.maxSlots())
                          );
                          SoundFeedback.listingCreated(player);
                      }
                      case NO_ITEM -> {
                          messages.sendConfigured(sender, "command.hold-item");
                          adminSounds.updateError(player);
                      }
                      case PRICE_OUT_OF_RANGE -> {
                          messages.sendConfigured(
                                  sender,
                                  "command.price-range",
                                  "min", formatDisplayPrice(result.minPrice()),
                                  "max", formatDisplayPrice(result.maxPrice())
                          );
                          adminSounds.updateError(player);
                      }
                      case INSUFFICIENT_FUNDS -> {
                          messages.sendConfigured(sender, "command.need-fee", "amount", formatEconomyPrice(result.missingFee()));
                          adminSounds.updateError(player);
                      }
                      case NO_SLOTS -> {
                          messages.sendConfigured(sender, "command.no-slots", "max", String.valueOf(result.maxSlots()));
                          adminSounds.updateError(player);
                      }
                      case BLACKLISTED -> {
                          messages.sendConfigured(sender, "command.blacklisted");
                          adminSounds.updateError(player);
                      }
                      case ECONOMY_ERROR -> {
                          messages.sendConfigured(sender, "command.economy-error");
                          adminSounds.updateError(player);
                      }
                }
                return true;
            }
            case "claims" -> {
                if (!(sender instanceof Player player)) {
                    messages.sendConfigured(sender, "command.only-players-claims");
                    return true;
                }

                guiManager.openClaimsGui(player, 0);
                SoundFeedback.menuOpen(player);
                return true;
            }
            case "search" -> {
                if (!(sender instanceof Player player)) {
                    messages.sendConfigured(sender, "command.only-players-search");
                    return true;
                }

                if (args.length < 2) {
                    messages.sendConfigured(sender, "command.usage-search", "label", label);
                    return true;
                }

                String query = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
                if (query.isBlank() || "clear".equalsIgnoreCase(query)) {
                    auctionService.clearSearchQuery(player.getUniqueId());
                    messages.sendConfigured(sender, "command.search-cleared");
                    SoundFeedback.clearSearch(player);
                } else {
                    try {
                        String validatedQuery = InputValidationUtils.validateSearchQuery(query);
                        auctionService.setSearchQuery(player.getUniqueId(), validatedQuery);
                        messages.sendConfigured(sender, "command.searching", "query", validatedQuery);
                        SoundFeedback.search(player);
                    } catch (InputValidationUtils.InvalidInputException exception) {
                        messages.sendConfigured(sender, "command.invalid-search-query", "error", exception.getMessage());
                        adminSounds.updateError(player);
                        return true;
                    }
                }

                guiManager.openMainGui(player, 0);
                SoundFeedback.menuOpen(player);
                return true;
            }
            case "view" -> {
                if (!(sender instanceof Player player)) {
                    messages.sendConfigured(sender, "command.only-players-view");
                    return true;
                }

                OfflinePlayer target = player;
                if (args.length >= 2) {
                    try {
                        String validatedPlayerName = InputValidationUtils.validatePlayerName(args[1]);
                          target = findOfflinePlayerByName(validatedPlayerName);
                      if (target == null) {
                          messages.sendConfigured(sender, "command.player-not-found", "player", validatedPlayerName);
                          adminSounds.updateError(player);
                          return true;
                      }
                  } catch (InputValidationUtils.InvalidInputException exception) {
                      messages.sendConfigured(sender, "command.invalid-player-name", "error", exception.getMessage());
                      adminSounds.updateError(player);
                      return true;
                      }
                  }

                  String targetName = target.getName() != null ? target.getName() : target.getUniqueId().toString().substring(0, 8);
                  messages.sendConfigured(sender, "command.opening-listings", "player", targetName);
                  guiManager.openSellerViewGui(player, target.getUniqueId(), 0);
                SoundFeedback.menuOpen(player);
                return true;
            }
              default -> {
                  messages.sendConfigured(sender, "command.usage", "label", label);
                  adminSounds.updateError(sender);
                  return true;
              }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("sell", "claims", "search", "view"), args[0]);
        }

        if (args.length == 2 && "sell".equalsIgnoreCase(args[0])) {
            return filter(List.of("100", "1K", "10K", "100K", "1M"), args[1]);
        }

        if (args.length >= 2 && "search".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                return filter(List.of("clear", "sharpness", "diamond", "shulker", "seller"), args[1]);
            }
            return Collections.emptyList();
        }

        if (args.length == 2 && "view".equalsIgnoreCase(args[0])) {
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                names.add(online.getName());
            }
            return filter(names, args[1]);
        }

        return Collections.emptyList();
    }

    private List<String> filter(List<String> values, String input) {
        String lowered = input.toLowerCase(Locale.ROOT);
        List<String> results = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lowered)) {
                results.add(value);
            }
        }
        return results;
    }

    private String formatDisplayPrice(double value) {
        return FormatUtils.formatPrice(value, settings.isCompactPriceFormatEnabled());
    }

    private String formatEconomyPrice(double value) {
        return settings.isCompactPriceFormatEnabled()
                ? FormatUtils.formatPrice(value, true)
                : economyService.format(value);
    }

    private OfflinePlayer findOfflinePlayerByName(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }

        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (offlinePlayer.getName() != null && offlinePlayer.getName().equalsIgnoreCase(name)) {
                return offlinePlayer;
            }
        }

        return null;
    }
}
