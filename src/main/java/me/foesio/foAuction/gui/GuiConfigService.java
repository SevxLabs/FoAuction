package me.foesio.foAuction.gui;

import me.foesio.foAuction.FoAuction;
import me.foesio.foAuction.utils.ColorPalette;
import me.foesio.core.config.ResourceFiles;
import me.foesio.core.material.MaterialTypes;
import me.foesio.core.message.FoMessageService;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GuiConfigService {
    private static final String[] GUI_FILES = {
            "main.yml",
            "listings.yml",
            "seller-view.yml",
            "claims.yml",
            "history.yml",
            "preview.yml",
            "confirmation.yml"
    };
    private static final String[] LEGACY_GUI_FILES = {
            "admin-remove.yml"
    };

    private final JavaPlugin plugin;
    private final FoMessageService messages;
    private final Map<String, FileConfiguration> configs;

    public GuiConfigService(JavaPlugin plugin, FoMessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
        this.configs = new HashMap<>();
        reload();
    }

    public void reload() {
        File folder = new File(plugin.getDataFolder(), "guis");
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning(ColorPalette.log("Could not create guis folder: " + folder.getAbsolutePath()));
            FoAuction.fileLogger().warn("Could not create guis folder: " + folder.getAbsolutePath() + ".");
        }

        configs.clear();
        for (String fileName : GUI_FILES) {
            File file = new File(folder, fileName);
            ResourceFiles.saveDefault(plugin, "guis/" + fileName);
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            backfillMissingDefaults(fileName, file, config);
            configs.put(fileName, config);
        }
        for (String fileName : LEGACY_GUI_FILES) {
            File file = new File(folder, fileName);
            configs.put(fileName, file.isFile()
                    ? YamlConfiguration.loadConfiguration(file)
                    : new YamlConfiguration());
        }
    }

    public String text(String fileName, String path, String fallback, Object... placeholders) {
        FileConfiguration config = config(fileName);
        return messages.renderTemplate(config.getString(path, fallback), placeholderValues(placeholders));
    }

    public List<String> textList(String fileName, String path, List<String> fallback, Object... placeholders) {
        FileConfiguration config = config(fileName);
        List<String> values = config.isList(path) ? config.getStringList(path) : fallback;
        Map<String, String> placeholderValues = placeholderValues(placeholders);
        return values.stream().map(line -> messages.renderTemplate(line, placeholderValues)).toList();
    }

    public int slot(String fileName, String path, int fallback) {
        FileConfiguration config = config(fileName);
        int slot = config.getInt(path, fallback);
        if (slot < 0 || slot > 53) {
            plugin.getLogger().warning(ColorPalette.log("Invalid GUI slot " + slot + " at guis/" + fileName + ":" + path + ". Using " + fallback + "."));
            FoAuction.fileLogger().warn("Invalid GUI slot " + slot + " at guis/" + fileName + ":" + path + ". Using " + fallback + ".");
            return fallback;
        }
        return slot;
    }

    public int[] slots(String fileName, String path, int[] fallback) {
        FileConfiguration config = config(fileName);
        List<Integer> values = config.getIntegerList(path);
        if (values.isEmpty()) {
            return fallback;
        }

        int[] slots = values.stream()
                .filter(slot -> {
                    boolean valid = slot >= 0 && slot <= 53;
                    if (!valid) {
                        plugin.getLogger().warning(ColorPalette.log("Invalid GUI content slot " + slot + " at guis/" + fileName + ":" + path + "."));
                        FoAuction.fileLogger().warn("Invalid GUI content slot " + slot + " at guis/" + fileName + ":" + path + ".");
                    }
                    return valid;
                })
                .mapToInt(Integer::intValue)
                .toArray();
        return slots.length == 0 ? fallback : slots;
    }

    public ItemStack item(
            String fileName,
            String path,
            Material fallbackMaterial,
            String fallbackName,
            List<String> fallbackLore,
            boolean fallbackGlow,
            Object... placeholders
    ) {
        FileConfiguration config = config(fileName);
        Material material = material(fileName, path + ".material", fallbackMaterial);
        ItemStack item = new ItemStack(material, Math.max(1, config.getInt(path + ".amount", 1)));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = config.isList(path + ".lore")
                    ? config.getStringList(path + ".lore")
                    : fallbackLore;
            Map<String, String> placeholderValues = placeholderValues(placeholders);
            meta.setDisplayName(messages.renderTemplate(config.getString(path + ".name", fallbackName), placeholderValues));
            meta.setLore(lore.stream().map(line -> messages.renderTemplate(line, placeholderValues)).toList());
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            if (config.getBoolean(path + ".hide-enchants", true)) {
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            boolean glow = config.isSet(path + ".glow")
                    ? config.getBoolean(path + ".glow")
                    : fallbackGlow;
            if (glow) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Applies only explicitly configured legacy item fields to a new core item.
     * This keeps core defaults for new installations while preserving old GUI
     * material, text, lore, amount, glow, and item-flag choices.
     */
    public ItemStack itemOrFallback(
            String fileName,
            String path,
            ItemStack fallback,
            Object... placeholders
    ) {
        FileConfiguration config = config(fileName);
        return itemOrFallback(fileName, config, path, fallback, placeholders);
    }

    private ItemStack itemOrFallback(
            String fileName,
            FileConfiguration config,
            String path,
            ItemStack fallback,
            Object... placeholders
    ) {
        if (!hasItemOverride(config, path)) {
            return fallback;
        }

        ItemStack item = fallback.clone();
        if (config.isSet(path + ".material")) {
            item.setType(material(fileName, path + ".material", item.getType()));
        }
        if (config.isSet(path + ".amount")) {
            item.setAmount(Math.max(1, config.getInt(path + ".amount", item.getAmount())));
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        Map<String, String> placeholderValues = placeholderValues(placeholders);
        if (config.isSet(path + ".name")) {
            meta.setDisplayName(messages.renderTemplate(config.getString(path + ".name", ""), placeholderValues));
        }
        if (config.isSet(path + ".lore")) {
            List<String> lore = config.isList(path + ".lore") ? config.getStringList(path + ".lore") : List.of();
            meta.setLore(lore.stream().map(line -> messages.renderTemplate(line, placeholderValues)).toList());
        }
        if (config.isSet(path + ".hide-enchants")) {
            if (config.getBoolean(path + ".hide-enchants")) {
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } else {
                meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
        }
        if (config.isSet(path + ".glow")) {
            if (config.getBoolean(path + ".glow")) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } else {
                meta.removeEnchant(Enchantment.UNBREAKING);
            }
        }
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Reads an optional legacy frame/filler material. Exact old light-gray
     * defaults are upgraded to the current gray default; custom materials win.
     */
    public Material fillerMaterial(String fileName, Material fallback) {
        FileConfiguration config = config(fileName);
        for (String path : List.of("filler.material", "frame.material", "items.filler.material")) {
            if (!config.isSet(path)) {
                continue;
            }
            Material selected = material(fileName, path, fallback);
            return selected == Material.LIGHT_GRAY_STAINED_GLASS_PANE ? fallback : selected;
        }
        return fallback;
    }

    /**
     * Reads an optional legacy empty-entry filler without changing the current
     * intentional light-gray default for empty auction/claim/history slots.
     */
    public Material contentFillerMaterial(String fileName, Material fallback) {
        FileConfiguration config = config(fileName);
        for (String path : List.of(
                "content-filler.material",
                "empty-slot-filler.material",
                "items.empty-slot.material"
        )) {
            if (config.isSet(path)) {
                return material(fileName, path, fallback);
            }
        }
        return fallback;
    }

    private Map<String, String> placeholderValues(Object... placeholders) {
        Map<String, String> values = new HashMap<>();
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            values.put(String.valueOf(placeholders[i]), String.valueOf(placeholders[i + 1]));
        }
        return values;
    }

    private Material material(String fileName, String path, Material fallback) {
        String raw = config(fileName).getString(path, fallback.name());
        Material material = MaterialTypes.match(raw == null ? "" : raw);
        if (material == null || material.isAir()) {
            plugin.getLogger().warning(ColorPalette.log("Invalid GUI material " + raw + " at guis/" + fileName + ":" + path + ". Using " + fallback.name() + "."));
            FoAuction.fileLogger().warn("Invalid GUI material " + raw + " at guis/" + fileName + ":" + path + ". Using " + fallback.name() + ".");
            return fallback;
        }
        return material;
    }

    private FileConfiguration config(String fileName) {
        FileConfiguration config = configs.get(fileName);
        if (config == null) {
            plugin.getLogger().warning(ColorPalette.log("Missing GUI config guis/" + fileName + ". Using built-in fallbacks."));
            FoAuction.fileLogger().warn("Missing GUI config guis/" + fileName + ". Using built-in fallbacks.");
            return new YamlConfiguration();
        }
        return config;
    }

    private void backfillMissingDefaults(String fileName, File file, YamlConfiguration config) {
        try (InputStream input = plugin.getResource("guis/" + fileName)) {
            if (input == null) {
                return;
            }

            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(input, StandardCharsets.UTF_8)
            );
            boolean changed = false;
            changed |= migrateLegacyMainHistoryButtons(fileName, config);
            for (String path : defaults.getKeys(true)) {
                Object value = defaults.get(path);
                if (value instanceof ConfigurationSection) {
                    continue;
                }
                if (!config.isSet(path)) {
                    config.set(path, value);
                    changed = true;
                }
            }
            changed |= migrateLegacyAuctionContentSlots(fileName, config);
            changed |= migrateLegacyHistoryLayout(fileName, config);
            changed |= migrateLegacyStandardSlots(fileName, config);
            changed |= migrateLegacyFillerDefaults(config);

            if (changed) {
                backupBeforeMigration(file);
                config.save(file);
                FoAuction.fileLogger().info("Backfilled missing GUI defaults in guis/" + fileName + ".");
            }
        } catch (IOException exception) {
            plugin.getLogger().warning(ColorPalette.log("Could not update GUI config guis/" + fileName + ": " + exception.getMessage()));
                FoAuction.fileLogger().warn("Could not update GUI config guis/" + fileName + ": " + exception.getMessage() + ".");
        }
    }

    private boolean migrateLegacyAuctionContentSlots(String fileName, YamlConfiguration config) {
        if (!"listings.yml".equals(fileName) && !"claims.yml".equals(fileName)) {
            return false;
        }

        List<Integer> legacySlots = List.of(
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        );
        if (!config.getIntegerList("content-slots").equals(legacySlots)) {
            return false;
        }

        config.set("content-slots", List.of(
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        ));
        return true;
    }

    private boolean migrateLegacyHistoryLayout(String fileName, YamlConfiguration config) {
        if (!"history.yml".equals(fileName)) {
            return false;
        }

        boolean changed = false;
        List<Integer> legacyContentSlots = List.of(
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25
        );
        if (config.getIntegerList("content-slots").equals(legacyContentSlots)) {
            config.set("content-slots", List.of(
                    10, 11, 12, 13, 14, 15, 16,
                    19, 20, 21, 22, 23, 24, 25,
                    28, 29, 30, 31, 32, 33, 34
            ));
            changed = true;
        }

        if (config.getInt("items.previous.slot", -1) == 27) {
            config.set("items.previous.slot", 45);
            changed = true;
        }
        if (config.getInt("items.back.slot", -1) == 31) {
            config.set("items.back.slot", 49);
            changed = true;
        }
        if (config.getInt("items.next.slot", -1) == 35) {
            config.set("items.next.slot", 53);
            changed = true;
        }
        if (config.getInt("items.empty.slot", -1) == 13) {
            config.set("items.empty.slot", 4);
            changed = true;
        }
        return changed;
    }

    private boolean migrateLegacyStandardSlots(String fileName, YamlConfiguration config) {
        if (!"listings.yml".equals(fileName)
                && !"seller-view.yml".equals(fileName)
                && !"claims.yml".equals(fileName)) {
            return false;
        }

        boolean changed = false;
        changed |= migrateExactSlot(config, "items.back.slot", 46, 49);
        changed |= migrateExactSlot(config, "items.refresh.slot", 49, 48);
        return changed;
    }

    private boolean migrateExactSlot(YamlConfiguration config, String path, int oldDefault, int newDefault) {
        if (config.getInt(path, Integer.MIN_VALUE) != oldDefault) {
            return false;
        }
        config.set(path, newDefault);
        return true;
    }

    private boolean migrateLegacyFillerDefaults(YamlConfiguration config) {
        boolean changed = false;
        for (String path : List.of("filler.material", "frame.material", "items.filler.material")) {
            if (!config.isSet(path)) {
                continue;
            }
            Material configured = MaterialTypes.match(config.getString(path, ""));
            if (configured == Material.LIGHT_GRAY_STAINED_GLASS_PANE) {
                config.set(path, Material.GRAY_STAINED_GLASS_PANE.name());
                changed = true;
            }
        }
        return changed;
    }

    private boolean migrateLegacyMainHistoryButtons(String fileName, YamlConfiguration config) {
        if (!"main.yml".equals(fileName) || !hasItemOverride(config, "items.history-or-clear")) {
            return false;
        }

        boolean changed = false;
        for (String targetPath : List.of("items.history", "items.clear-search")) {
            if (hasItemOverride(config, targetPath)) {
                continue;
            }
            for (String field : List.of("material", "amount", "name", "lore", "hide-enchants", "glow")) {
                String source = "items.history-or-clear." + field;
                if (!config.isSet(source)) {
                    continue;
                }
                config.set(targetPath + "." + field, config.get(source));
                changed = true;
            }
        }
        return changed;
    }

    private boolean hasItemOverride(FileConfiguration config, String path) {
        return config.isSet(path + ".material")
                || config.isSet(path + ".amount")
                || config.isSet(path + ".name")
                || config.isSet(path + ".lore")
                || config.isSet(path + ".hide-enchants")
                || config.isSet(path + ".glow");
    }

    private void backupBeforeMigration(File file) throws IOException {
        if (!file.isFile()) {
            return;
        }
        File backup = new File(file.getParentFile(), file.getName() + ".pre-migration.bak");
        if (!backup.exists()) {
            Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
        }
    }
}
