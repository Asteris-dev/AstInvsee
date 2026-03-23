package ru.asteris.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import ru.asteris.Main;
import me.clip.placeholderapi.PlaceholderAPI;

public class ColorUtils {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([a-fA-F0-9]{6})");
    private static final boolean hasPAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    public static Component parse(String message) {
        if (message == null) return Component.empty();

        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder buffer = new StringBuilder();

        while (matcher.find()) {
            matcher.appendReplacement(buffer, "<#" + matcher.group(1) + ">");
        }

        message = matcher.appendTail(buffer).toString();
        message = message.replace("&0", "<black>").replace("&1", "<dark_blue>").replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>").replace("&4", "<dark_red>").replace("&5", "<dark_purple>")
                .replace("&6", "<gold>").replace("&7", "<gray>").replace("&8", "<dark_gray>")
                .replace("&9", "<blue>").replace("&a", "<green>").replace("&b", "<aqua>")
                .replace("&c", "<red>").replace("&d", "<light_purple>").replace("&e", "<yellow>")
                .replace("&f", "<white>").replace("&l", "<bold>").replace("&o", "<italic>")
                .replace("&n", "<underlined>").replace("&m", "<strikethrough>").replace("&k", "<obfuscated>")
                .replace("&r", "<reset>").replace("§", "&");

        try {
            return MiniMessage.miniMessage().deserialize(message);
        } catch (Exception e) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
        }
    }

    public static String colorize(String message) {
        if (message == null) return "";
        return SERIALIZER.serialize(parse(message));
    }

    public static String colorize(OfflinePlayer player, String message) {
        if (message == null) return "";
        if (hasPAPI && player != null) {
            message = PlaceholderAPI.setPlaceholders(player, message);
        }
        return colorize(message);
    }

    public static String formatMoney(double amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);
        return formatter.format(amount);
    }

    public static String formatTime(int ticks) {
        FileConfiguration config = Main.getInstance().getConfig();
        int totalSeconds = ticks / 20;
        int days = totalSeconds / 86400;
        int hours = (totalSeconds % 86400) / 3600;
        int minutes = ((totalSeconds % 86400) % 3600) / 60;
        int seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append(config.getString("time-format.days", "д")).append(" ");
        if (hours > 0) sb.append(hours).append(config.getString("time-format.hours", "ч")).append(" ");
        if (minutes > 0) sb.append(minutes).append(config.getString("time-format.minutes", "м")).append(" ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append(config.getString("time-format.seconds", "с"));

        return sb.toString().trim();
    }
}