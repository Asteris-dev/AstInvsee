package ru.asteris.astinvsee.managers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import ru.asteris.astinvsee.Main;
import ru.asteris.astlib.utils.AstGui;
import ru.asteris.astlib.utils.ColorUtils;
import ru.asteris.astlib.utils.ItemBuilder;
import ru.asteris.astinvsee.utils.LogUtils;
import ru.asteris.astinvsee.utils.SaveUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SaveManager implements Listener {

    private final Map<UUID, PendingSave> savePrompts = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> viewingSaves = new ConcurrentHashMap<>();

    private static class PendingSave {
        ItemStack[] contents;
        ItemStack[] armor;
        ItemStack offhand;

        PendingSave(ItemStack[] contents, ItemStack[] armor, ItemStack offhand) {
            this.contents = contents;
            this.armor = armor;
            this.offhand = offhand;
        }
    }

    public void startSavePrompt(Player admin, ItemStack[] contents, ItemStack[] armor, ItemStack offhand) {
        admin.closeInventory();
        savePrompts.put(admin.getUniqueId(), new PendingSave(contents, armor, offhand));
        String prompt = Main.getInstance().getConfig().getString("messages.save-prompt");
        String cancelWord = Main.getInstance().getConfig().getString("messages.save-cancel-word", "cancel");
        admin.sendMessage(ColorUtils.colorize(prompt.replace("%cancel_word%", cancelWord)));
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!savePrompts.containsKey(player.getUniqueId())) return;

        event.setCancelled(true);
        String message = event.getMessage();
        PendingSave pending = savePrompts.remove(player.getUniqueId());

        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            String cancelWord = Main.getInstance().getConfig().getString("messages.save-cancel-word", "cancel");
            if (message.equalsIgnoreCase(cancelWord)) {
                player.sendMessage(ColorUtils.colorize(Main.getInstance().getConfig().getString("messages.save-cancelled")));
                return;
            }

            SaveUtils.createSave(message, pending.contents, pending.armor, pending.offhand);
            String msg = Main.getInstance().getConfig().getString("messages.save-success").replace("%name%", message);
            player.sendMessage(ColorUtils.colorize(msg));
            LogUtils.log(player.getName() + " создал сохранение инвентаря: " + message);
        });
    }

    public void openSavesGUI(Player viewer, OfflinePlayer target, int page) {
        Set<String> savesSet = SaveUtils.getSaves();
        if (savesSet.isEmpty()) {
            viewer.sendMessage(ColorUtils.colorize(Main.getInstance().getConfig().getString("messages.saves-empty")));
            return;
        }

        List<String> saves = new ArrayList<>(savesSet);
        FileConfiguration config = Main.getInstance().getConfig();
        String title = config.getString("gui.saves-title").replace("%player%", target.getName() != null ? target.getName() : "Offline");

        AstGui gui = new AstGui(54, title);
        gui.setCancelClicks(true);

        Material mat = Material.matchMaterial(config.getString("gui.save-item.material", "PAPER"));
        if (mat == null) mat = Material.PAPER;

        int startIndex = page * 45;
        int endIndex = Math.min(startIndex + 45, saves.size());

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            String saveName = saves.get(i);
            List<String> lore = new ArrayList<>();
            for (String line : config.getStringList("gui.save-item.lore")) {
                lore.add(line.replace("%date%", SaveUtils.getSaveDate(saveName)));
            }

            ItemStack item = new ItemBuilder(mat)
                    .setName(config.getString("gui.save-item.name").replace("%name%", saveName))
                    .setLore(lore)
                    .build();

            gui.setItem(slot++, item, e -> {
                viewer.closeInventory();
                openConfirmGUI(viewer, target.getUniqueId(), saveName);
            });
        }

        if (page > 0) {
            gui.setItem(45, createGuiItem("gui.prev-page"), e -> openSavesGUI(viewer, target, page - 1));
        }
        if (endIndex < saves.size()) {
            gui.setItem(53, createGuiItem("gui.next-page"), e -> openSavesGUI(viewer, target, page + 1));
        }

        gui.onClose(e -> viewingSaves.remove(viewer.getUniqueId()));
        viewingSaves.put(viewer.getUniqueId(), target.getUniqueId());
        viewer.openInventory(gui.getInventory());
    }

    public void openConfirmGUI(Player viewer, UUID targetId, String saveName) {
        FileConfiguration config = Main.getInstance().getConfig();
        String title = config.getString("gui.confirm-title").replace("%name%", saveName);

        AstGui gui = new AstGui(27, title);
        gui.setCancelClicks(true);

        gui.setItem(11, createGuiItem("gui.confirm-yes"), e -> {
            Player target = Bukkit.getPlayer(targetId);
            if (target != null && target.isOnline()) {
                ItemStack[] contents = new ItemStack[36];
                ItemStack[] armor = new ItemStack[4];
                ItemStack[] offHand = new ItemStack[1];
                SaveUtils.loadSaveToArrays(saveName, contents, armor, offHand);

                target.getInventory().setContents(contents);
                target.getInventory().setArmorContents(armor);
                if (offHand[0] != null) target.getInventory().setItemInOffHand(offHand[0]);

                String msg = Main.getInstance().getConfig().getString("messages.save-loaded").replace("%name%", saveName);
                viewer.sendMessage(ColorUtils.colorize(msg));
                LogUtils.log(viewer.getName() + " загрузил сохранение '" + saveName + "' игроку " + target.getName());
            } else {
                viewer.sendMessage(ColorUtils.colorize(Main.getInstance().getConfig().getString("messages.player-offline-load")));
            }
            viewer.closeInventory();
        });

        gui.setItem(15, createGuiItem("gui.confirm-no"), e -> viewer.closeInventory());

        viewer.openInventory(gui.getInventory());
    }

    private ItemStack createGuiItem(String path) {
        FileConfiguration config = Main.getInstance().getConfig();
        Material mat = Material.matchMaterial(config.getString(path + ".material", "STONE"));
        if (mat == null) mat = Material.STONE;
        return new ItemBuilder(mat)
                .setName(config.getString(path + ".name"))
                .setLore(config.getStringList(path + ".lore"))
                .build();
    }

    public void closeAll() {
        for (UUID uuid : viewingSaves.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) p.closeInventory();
        }
        viewingSaves.clear();
    }
}