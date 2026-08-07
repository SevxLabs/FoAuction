package me.foesio.foAuction.service;

import me.foesio.core.discord.DiscordWebhookEmbed;
import me.foesio.core.discord.DiscordWebhookMessage;
import me.foesio.core.discord.DiscordWebhookResult;
import me.foesio.core.logging.FoFileLogger;
import me.foesio.foAuction.FoAuction;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.Locale;

/**
 * Maps FoAuction event names and embed content onto the shared core webhook transport.
 * Event toggles and URL normalization remain configuration-authoritative in FoPluginCore.
 */
public final class DiscordWebhookService {
    private final me.foesio.core.discord.DiscordWebhookService delegate;

    public DiscordWebhookService(JavaPlugin plugin) {
        this.delegate = me.foesio.core.discord.DiscordWebhookService.create(plugin);
    }

    public void send(EventType eventType, String message) {
        if (eventType == null) {
            return;
        }

        DiscordWebhookEmbed embed = DiscordWebhookEmbed.builder()
                .title("FoAuction")
                .description(normalizeMessage(message))
                .color(eventType.embedColor())
                .field("Event", eventType.displayName(), true)
                .footer("FoAuction")
                .timestamp(Instant.now())
                .build();

        delegate.send(eventType.configKey(), DiscordWebhookMessage.builder()
                        .embed(embed)
                        .suppressMentions(true)
                        .build())
                .thenAccept(result -> logFailure(eventType, result));
    }

    /** The core transport uses its own async HTTP client and has no close operation. */
    public void close() {
    }

    private void logFailure(EventType eventType, DiscordWebhookResult result) {
        FoFileLogger logger = FoAuction.fileLogger();
        if (result != null && !result.sent() && logger != null) {
            logger.warn(
                    "Discord webhook " + eventType.configKey() + " send did not complete: "
                            + (result.error() == null ? result.status().name() : result.error()) + "."
            );
        }
    }

    private String normalizeMessage(String message) {
        String normalized = message == null || message.isBlank() ? "No details provided." : message.trim();
        return normalized.length() <= 3500 ? normalized : normalized.substring(0, 3500) + "...";
    }

    public enum EventType {
        SELLING("Selling", 0x2ECC71),
        LISTING("Listing", 0x3498DB),
        ADMIN_REMOVING("Admin Removing", 0xE74C3C),
        CLAIMS("Claims", 0xF1C40F),
        CANCELLING_LISTINGS("Cancelling Listing", 0x95A5A6),
        CLAIMING_LISTINGS("Claiming Listing", 0x9B59B6);

        private final String displayName;
        private final int embedColor;

        EventType(String displayName, int embedColor) {
            this.displayName = displayName;
            this.embedColor = embedColor;
        }

        public String displayName() {
            return displayName;
        }

        public int embedColor() {
            return embedColor;
        }

        public String configKey() {
            return name().toLowerCase(Locale.ROOT).replace('_', '-');
        }
    }
}
