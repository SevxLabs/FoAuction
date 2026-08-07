package me.foesio.foAuction.tasks;

import me.foesio.foAuction.service.AuctionService;
import me.foesio.foAuction.FoAuction;
import me.foesio.foAuction.utils.ColorPalette;
import me.foesio.core.scheduler.FoScheduler;
import org.bukkit.plugin.java.JavaPlugin;

public final class ExpirationTask {
    private final JavaPlugin plugin;
    private final AuctionService auctionService;
    private final FoScheduler scheduler;
    private final long intervalTicks;
    private volatile boolean cancelled;

    public ExpirationTask(JavaPlugin plugin, AuctionService auctionService, FoScheduler scheduler, long intervalTicks) {
        this.plugin = plugin;
        this.auctionService = auctionService;
        this.scheduler = scheduler;
        this.intervalTicks = intervalTicks;
    }

    public void start() {
        scheduleNext();
    }

    public void cancel() {
        cancelled = true;
    }

    private void scheduleNext() {
        if (cancelled) {
            return;
        }

        scheduler.runGlobalLater(() -> {
            if (cancelled || !plugin.isEnabled()) {
                return;
            }
            try {
                runOnce();
            } finally {
                scheduleNext();
            }
        }, intervalTicks);
    }

    private void runOnce() {
        int expired = auctionService.purgeExpired();
        if (expired > 0) {
            plugin.getLogger().info(ColorPalette.log("Expired " + expired + " auction listing(s)."));
            FoAuction.fileLogger().info("Expired " + expired + " auction listing(s).");
        }
    }
}
