package me.foesio.foAuction.model;

import me.foesio.core.editor.CycleOption;
import me.foesio.core.editor.CycleOptions;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum FilterMode {
    ALL("All") {
        @Override
        public boolean matches(Material material) {
            return true;
        }
    },
    WEAPONS("Weapons") {
        @Override
        public boolean matches(Material material) {
            return containsAny(material, "SWORD", "AXE", "BOW", "CROSSBOW", "TRIDENT", "MACE");
        }
    },
    ARMOR("Armor") {
        @Override
        public boolean matches(Material material) {
            return containsAny(material, "_HELMET", "_CHESTPLATE", "_LEGGINGS", "_BOOTS", "ELYTRA", "SHIELD");
        }
    },
    TOOLS("Tools") {
        @Override
        public boolean matches(Material material) {
            return containsAny(
                    material,
                    "PICKAXE",
                    "SHOVEL",
                    "HOE",
                    "SHEARS",
                    "FISHING_ROD",
                    "FLINT_AND_STEEL",
                    "BRUSH",
                    "SPYGLASS",
                    "COMPASS",
                    "CLOCK"
            );
        }
    },
    BLOCKS("Blocks") {
        @Override
        public boolean matches(Material material) {
            return material.isBlock();
        }
    },
    FOOD("Food") {
        @Override
        public boolean matches(Material material) {
            return material.isEdible();
        }
    },
    FARM("Farm") {
        @Override
        public boolean matches(Material material) {
            return containsAny(
                    material,
                    "SEEDS",
                    "WHEAT",
                    "CARROT",
                    "POTATO",
                    "BEETROOT",
                    "NETHER_WART",
                    "SUGAR_CANE",
                    "CACTUS",
                    "BAMBOO",
                    "MELON",
                    "PUMPKIN",
                    "SAPLING",
                    "COCOA",
                    "BONE_MEAL",
                    "HONEY_BOTTLE",
                    "HONEYCOMB"
            );
        }
    },
    REDSTONE("Redstone") {
        @Override
        public boolean matches(Material material) {
            return containsAny(
                    material,
                    "REDSTONE",
                    "REPEATER",
                    "COMPARATOR",
                    "OBSERVER",
                    "PISTON",
                    "DISPENSER",
                    "DROPPER",
                    "HOPPER",
                    "NOTE_BLOCK",
                    "LEVER",
                    "BUTTON",
                    "PRESSURE_PLATE",
                    "DAYLIGHT_DETECTOR",
                    "SCULK_SENSOR",
                    "TARGET",
                    "TRIPWIRE",
                    "RAIL"
            );
        }
    },
    POTIONS("Potions") {
        @Override
        public boolean matches(Material material) {
            return containsAny(material, "POTION", "TIPPED_ARROW");
        }
    },
    MISC("Misc") {
        @Override
        public boolean matches(Material material) {
            return !WEAPONS.matches(material)
                    && !ARMOR.matches(material)
                    && !TOOLS.matches(material)
                    && !BLOCKS.matches(material)
                    && !FOOD.matches(material)
                    && !FARM.matches(material)
                    && !REDSTONE.matches(material)
                    && !POTIONS.matches(material);
        }
    };

    private final String displayName;
    private static final List<CycleOption> CYCLE_OPTIONS = Arrays.stream(values())
            .map(mode -> new CycleOption(mode.name(), mode.displayName))
            .toList();

    FilterMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public abstract boolean matches(Material material);

    public FilterMode next() {
        return fromName(CycleOptions.nextValue(name(), CYCLE_OPTIONS));
    }

    public static List<CycleOption> cycleOptions() {
        return CYCLE_OPTIONS;
    }

    public static FilterMode fromName(String name) {
        if (name == null || name.isBlank()) {
            return ALL;
        }

        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return ALL;
        }
    }

    private static boolean containsAny(Material material, String... parts) {
        String name = material.name();
        for (String part : parts) {
            if (name.contains(part)) {
                return true;
            }
        }
        return false;
    }
}
