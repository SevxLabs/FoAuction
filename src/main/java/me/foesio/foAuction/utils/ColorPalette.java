package me.foesio.foAuction.utils;

import me.foesio.core.message.FoStyle;
import net.md_5.bungee.api.ChatColor;

public final class ColorPalette {
    public static final String PLUGIN_DISPLAY_NAME = "FoAuction";
    public static final ChatColor THEME_COLOR = ChatColor.of(FoStyle.THEME);
    public static final ChatColor LIGHT_GRAY_COLOR = ChatColor.of(FoStyle.MUTED);
    public static final ChatColor WHITE_COLOR = ChatColor.of(FoStyle.WHITE);
    public static final ChatColor GOOD_COLOR = ChatColor.of(FoStyle.GOOD);
    public static final ChatColor BAD_COLOR = ChatColor.of(FoStyle.BAD);
    private static final String LOG_PREFIX = "[" + PLUGIN_DISPLAY_NAME + "] ";
    private ColorPalette() {
    }

    public static String log(String message) {
        return LOG_PREFIX + (message == null ? "" : message);
    }

}
