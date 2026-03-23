package ru.asteris.managers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.asteris.Main;
import ru.asteris.utils.ColorUtils;
import ru.asteris.utils.LogUtils;
import ru.asteris.utils.SaveUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SaveManager implements Listener {

    private final Map<UUID, PendingSave> savePrompts = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> viewingSaves = new ConcurrentHashMap<>();
    private final Map<UUID, ConfirmData> confirmViews = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> viewerPages = new ConcurrentHashMap<>();

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

    private static class ConfirmData {
        UUID targetId;
        String saveName;

        ConfirmData(UUID targetId, String saveName) {
            this.targetId = targetId;
            this.saveName = saveName;
        }
    }

    public void startSavePrompt(Player admin, ItemStack[] contents, ItemStack[] armor, ItemStack offhand) {
        admin.closeInventory();
        savePrompts.put(admin.getUniqueId(), new PendingSave(contents, armor, offhand));
        String prompt = Main.getInstance().getConfig().getString("messages.save-prompt");
        String cancelWord = Main.getInstance().getConfig().getString("messages.save-cancel-word", "cancel");
        admin.sendMessage(ColorUtils.colorize(admin, prompt.replace("%cancel_word%", cancelWord)));
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
                player.sendMessage(ColorUtils.colorize(player, Main.getInstance().getConfig().getString("messages.save-cancelled")));
                return;
            }

            SaveUtils.createSave(message, pending.contents, pending.armor, pending.offhand);
            String msg = Main.getInstance().getConfig().getString("messages.save-success").replace("%name%", message);
            player.sendMessage(ColorUtils.colorize(player, msg));
            LogUtils.log(player.getName() + " создал сохранение инвентаря: " + message);
        });
    }

    public void openSavesGUI(Player viewer, OfflinePlayer target, int page) {
        Set<String> savesSet = SaveUtils.getSaves();
        if (savesSet.isEmpty()) {
            viewer.sendMessage(ColorUtils.colorize(viewer, Main.getInstance().getConfig().getString("messages.saves-empty")));
            return;
        }

        List<String> saves = new ArrayList<>(savesSet);
        FileConfiguration config = Main.getInstance().getConfig();
        String title = config.getString("gui.saves-title").replace("%player%", target.getName() != null ? target.getName() : "Offline");
        Inventory inv = Bukkit.createInventory(null, 54, ColorUtils.colorize(viewer, title));

        Material mat = Material.matchMaterial(config.getString("gui.save-item.material", "PAPER"));
        if (mat == null) mat = Material.PAPER;
        NamespacedKey key = new NamespacedKey(Main.getInstance(), "ast_save_name");

        int startIndex = page * 45;
        int endIndex = Math.min(startIndex + 45, saves.size());

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            String saveName = saves.get(i);
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ColorUtils.colorize(viewer, config.getString("gui.save-item.name").replace("%name%", saveName)));
                List<String> lore = new ArrayList<>();
                for (String line : config.getStringList("gui.save-item.lore")) {
                    lore.add(ColorUtils.colorize(viewer, line.replace("%date%", SaveUtils.getSaveDate(saveName))));
                }
                meta.setLore(lore);
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, saveName);
                item.setItemMeta(meta);
            }
            inv.setItem(slot++, item);
        }

        if (page > 0) {
            inv.setItem(45, createGuiItem("gui.prev-page", viewer));
        }
        if (endIndex < saves.size()) {
            inv.setItem(53, createGuiItem("gui.next-page", viewer));
        }

        viewerPages.put(viewer.getUniqueId(), page);
        viewingSaves.put(viewer.getUniqueId(), target.getUniqueId());
        viewer.openInventory(inv);
    }

    private ItemStack createGuiItem(String path, Player viewer) {
        FileConfiguration config = Main.getInstance().getConfig();
        Material mat = Material.matchMaterial(config.getString(path + ".material", "ARROW"));
        if (mat == null) mat = Material.ARROW;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (config.contains(path + ".name")) {
                meta.setDisplayName(ColorUtils.colorize(viewer, config.getString(path + ".name")));
            }
            if (config.contains(path + ".lore")) {
                List<String> lore = new ArrayList<>();
                for (String line : config.getStringList(path + ".lore")) {
                    lore.add(ColorUtils.colorize(viewer, line));
                }
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public void openConfirmGUI(Player viewer, UUID targetId, String saveName) {
        FileConfiguration config = Main.getInstance().getConfig();
        String title = config.getString("gui.confirm-title").replace("%name%", saveName);
        Inventory inv = Bukkit.createInventory(null, 27, ColorUtils.colorize(viewer, title));

        Material yesMat = Material.matchMaterial(config.getString("gui.confirm-yes.material", "GREEN_STAINED_GLASS_PANE"));
        if (yesMat == null) yesMat = Material.GREEN_STAINED_GLASS_PANE;
        ItemStack yes = new ItemStack(yesMat);
        ItemMeta yesMeta = yes.getItemMeta();
        if (yesMeta != null) {
            yesMeta.setDisplayName(ColorUtils.colorize(viewer, config.getString("gui.confirm-yes.name")));
            yes.setItemMeta(yesMeta);
        }

        Material noMat = Material.matchMaterial(config.getString("gui.confirm-no.material", "RED_STAINED_GLASS_PANE"));
        if (noMat == null) noMat = Material.RED_STAINED_GLASS_PANE;
        ItemStack no = new ItemStack(noMat);
        ItemMeta noMeta = no.getItemMeta();
        if (noMeta != null) {
            noMeta.setDisplayName(ColorUtils.colorize(viewer, config.getString("gui.confirm-no.name")));
            no.setItemMeta(noMeta);
        }

        inv.setItem(11, yes);
        inv.setItem(15, no);

        viewer.openInventory(inv);
        confirmViews.put(viewer.getUniqueId(), new ConfirmData(targetId, saveName));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player viewer = (Player) event.getWhoClicked();

        if (viewingSaves.containsKey(viewer.getUniqueId())) {
            event.setCancelled(true);
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
                ItemStack item = event.getCurrentItem();
                int slot = event.getSlot();
                UUID targetId = viewingSaves.get(viewer.getUniqueId());
                int currentPage = viewerPages.getOrDefault(viewer.getUniqueId(), 0);

                if (slot == 45 && item != null && item.getType() != Material.AIR) {
                    openSavesGUI(viewer, Bukkit.getOfflinePlayer(targetId), currentPage - 1);
                    return;
                }
                if (slot == 53 && item != null && item.getType() != Material.AIR) {
                    openSavesGUI(viewer, Bukkit.getOfflinePlayer(targetId), currentPage + 1);
                    return;
                }

                if (item != null && item.hasItemMeta()) {
                    NamespacedKey key = new NamespacedKey(Main.getInstance(), "ast_save_name");
                    if (item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                        String saveName = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
                        viewer.closeInventory();
                        openConfirmGUI(viewer, targetId, saveName);
                    }
                }
            }
        } else if (confirmViews.containsKey(viewer.getUniqueId())) {
            event.setCancelled(true);
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
                if (event.getSlot() == 11) {
                    ConfirmData data = confirmViews.get(viewer.getUniqueId());
                    Player target = Bukkit.getPlayer(data.targetId);
                    if (target != null && target.isOnline()) {
                        ItemStack[] contents = new ItemStack[36];
                        ItemStack[] armor = new ItemStack[4];
                        ItemStack[] offHand = new ItemStack[1];
                        SaveUtils.loadSaveToArrays(data.saveName, contents, armor, offHand);

                        target.getInventory().setContents(contents);
                        target.getInventory().setArmorContents(armor);
                        if (offHand[0] != null) target.getInventory().setItemInOffHand(offHand[0]);

                        String msg = Main.getInstance().getConfig().getString("messages.save-loaded").replace("%name%", data.saveName);
                        viewer.sendMessage(ColorUtils.colorize(viewer, msg));
                        LogUtils.log(viewer.getName() + " загрузил сохранение '" + data.saveName + "' игроку " + target.getName());
                    } else {
                        viewer.sendMessage(ColorUtils.colorize(viewer, Main.getInstance().getConfig().getString("messages.player-offline-load")));
                    }
                    viewer.closeInventory();
                } else if (event.getSlot() == 15) {
                    viewer.closeInventory();
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        viewingSaves.remove(event.getPlayer().getUniqueId());
        confirmViews.remove(event.getPlayer().getUniqueId());
        viewerPages.remove(event.getPlayer().getUniqueId());
    }

    public void closeAll() {
        for (UUID uuid : viewingSaves.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) p.closeInventory();
        }
        for (UUID uuid : confirmViews.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) p.closeInventory();
        }
        viewingSaves.clear();
        confirmViews.clear();
        viewerPages.clear();
    }
}