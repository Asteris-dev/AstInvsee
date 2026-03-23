package ru.asteris.utils;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import ru.asteris.Main;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SaveUtils {

    public static void createSave(String name, ItemStack[] contents, ItemStack[] armor, ItemStack offHand) {
        File dir = new File(Main.getInstance().getDataFolder(), "saves");
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, name + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        config.set("date", new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date()));
        config.set("contents", contents);
        config.set("armor", armor);
        config.set("offhand", offHand);

        try {
            config.save(file);
        } catch (IOException ignored) {}
    }

    public static Set<String> getSaves() {
        Set<String> saves = new HashSet<>();
        File dir = new File(Main.getInstance().getDataFolder(), "saves");
        if (!dir.exists() || !dir.isDirectory()) return saves;

        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                saves.add(file.getName().replace(".yml", ""));
            }
        }
        return saves;
    }

    public static String getSaveDate(String name) {
        File file = new File(Main.getInstance().getDataFolder() + "/saves", name + ".yml");
        if (!file.exists()) return Main.getInstance().getConfig().getString("messages.unknown-date", "Неизвестно");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        return config.getString("date", Main.getInstance().getConfig().getString("messages.unknown-date", "Неизвестно"));
    }

    public static void loadSaveToArrays(String name, ItemStack[] contents, ItemStack[] armor, ItemStack[] offHand) {
        File file = new File(Main.getInstance().getDataFolder() + "/saves", name + ".yml");
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        List<?> contentsList = config.getList("contents");
        if (contentsList != null) {
            ItemStack[] loaded = contentsList.toArray(new ItemStack[0]);
            System.arraycopy(loaded, 0, contents, 0, Math.min(loaded.length, contents.length));
        }

        List<?> armorList = config.getList("armor");
        if (armorList != null) {
            ItemStack[] loaded = armorList.toArray(new ItemStack[0]);
            System.arraycopy(loaded, 0, armor, 0, Math.min(loaded.length, armor.length));
        }

        ItemStack loadedOffhand = config.getItemStack("offhand");
        if (loadedOffhand != null) {
            offHand[0] = loadedOffhand;
        }
    }
}