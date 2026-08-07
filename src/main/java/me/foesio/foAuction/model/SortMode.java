package me.foesio.foAuction.model;

import me.foesio.core.editor.CycleOption;
import me.foesio.core.editor.CycleOptions;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum SortMode {
    PRICE_HIGH_LOW("Highest Price"),
    PRICE_LOW_HIGH("Lowest Price"),
    NEWEST("Most Recent"),
    OLDEST("Least Recent");

    private final String displayName;
    private static final List<CycleOption> CYCLE_OPTIONS = Arrays.stream(values())
            .map(mode -> new CycleOption(mode.name(), mode.displayName))
            .toList();

    SortMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public SortMode next() {
        return fromName(CycleOptions.nextValue(name(), CYCLE_OPTIONS));
    }

    public static List<CycleOption> cycleOptions() {
        return CYCLE_OPTIONS;
    }

    public static SortMode fromName(String name) {
        if (name == null || name.isBlank()) {
            return NEWEST;
        }

        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return NEWEST;
        }
    }
}
