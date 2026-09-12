package me.foesio.foAuction.utils;

import me.foesio.core.number.NumberFormatters;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class FormatUtils {
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0.####");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private FormatUtils() {
    }

    public static String formatPrice(double value) {
        return PRICE_FORMAT.format(value);
    }

    public static String formatPrice(double value, boolean compact) {
        if (!compact || Math.abs(value) < 1_000D) {
            return formatPrice(value);
        }
        return NumberFormatters.compact(value);
    }

    public static String formatDuration(long millis) {
        if (millis <= 0) {
            return "Expired";
        }

        Duration duration = Duration.ofMillis(millis);
        long days = duration.toDays();
        duration = duration.minusDays(days);
        long hours = duration.toHours();
        duration = duration.minusHours(hours);
        long minutes = duration.toMinutes();

        if (days > 0) {
            return days + "d " + hours + "h";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }

    public static String formatDateTime(long epochMillis) {
        return DATE_TIME_FORMAT.format(Instant.ofEpochMilli(epochMillis));
    }
}
