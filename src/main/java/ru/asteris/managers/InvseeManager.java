package ru.asteris.managers;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import ru.asteris.Main;
import ru.asteris.utils.ColorUtils;
import ru.asteris.utils.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InvseeManager implements Listener {

    private final Map<UUID, UUID> activeViews = new ConcurrentHashMap<>();

    public void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, UUID> entry : activeViews.entrySet()) {
                    Player viewer = Bukkit.getPlayer(entry.getKey());
                    Player target = Bukkit.getPlayer(entry.getValue());

                    if (viewer == null || target == null || !viewer.isOnline() || !target.isOnline()) {
                        if (viewer != null && viewer.isOnline()) viewer.closeInventory();
                        activeViews.remove(entry.getKey());
                        continue;
                    }
                    updateInventory(viewer.getOpenInventory().getTopInventory(), target, viewer);
                }
            }
        }.runTaskTimer(Main.getInstance(), 10L, 10L);
    }

    public void openInvsee(Player viewer, Player target) {
        String title = Main.getInstance().getConfig().getString("messages.gui-title", "Invsee").replace("%player%", target.getName());
        Inventory inv = Bukkit.createInventory(null, 54, ColorUtils.colorize(viewer, title));
        updateInventory(inv, target, viewer);
        viewer.openInventory(inv);
        activeViews.put(viewer.getUniqueId(), target.getUniqueId());
        LogUtils.log(viewer.getName() + " открыл инвентарь игрока " + target.getName());
    }

    private void updateInventory(Inventory inv, Player target, Player viewer) {
        FileConfiguration config = Main.getInstance().getConfig();

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        if (headMeta != null) {
            headMeta.setOwningPlayer(target);
            String nameFormat = config.getString("gui.head.name", "&a%player%");
            headMeta.setDisplayName(ColorUtils.colorize(target, nameFormat.replace("%player%", target.getName())));

            List<String> lore = new ArrayList<>();
            double maxHealth = target.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null ? target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() : 20.0;
            String healthStr = String.valueOf(Math.round(target.getHealth()));
            String maxHealthStr = String.valueOf(Math.round(maxHealth));
            String foodStr = String.valueOf(target.getFoodLevel());

            for (String line : config.getStringList("gui.head.lore")) {
                String replaced = line.replace("%health%", healthStr)
                        .replace("%max_health%", maxHealthStr)
                        .replace("%food%", foodStr)
                        .replace("%player%", target.getName());
                lore.add(ColorUtils.colorize(target, replaced));
            }
            headMeta.setLore(lore);
            head.setItemMeta(headMeta);
        }
        inv.setItem(0, head);

        boolean hasEffects = !target.getActivePotionEffects().isEmpty();
        String colorHex = hasEffects ? config.getString("gui.bottle.filled-color", "") : config.getString("gui.bottle.empty-color", "");
        Material bottleMat = colorHex.isEmpty() ? Material.GLASS_BOTTLE : Material.POTION;

        ItemStack bottle = new ItemStack(bottleMat);
        ItemMeta bottleMeta = bottle.getItemMeta();
        if (bottleMeta != null) {
            bottleMeta.setDisplayName(ColorUtils.colorize(target, config.getString("gui.bottle.name")));
            List<String> lore = new ArrayList<>();
            if (!hasEffects) {
                lore.add(ColorUtils.colorize(target, config.getString("gui.bottle.empty-text")));
            } else {
                String effFormat = config.getString("gui.bottle.effect-format");
                for (PotionEffect effect : target.getActivePotionEffects()) {
                    String effName = getTranslatedEffect(effect.getType());
                    String timeStr = ColorUtils.formatTime(effect.getDuration());
                    lore.add(ColorUtils.colorize(target, effFormat
                            .replace("%effect%", effName)
                            .replace("%level%", String.valueOf(effect.getAmplifier() + 1))
                            .replace("%time%", timeStr)));
                }
            }
            bottleMeta.setLore(lore);

            if (bottleMeta instanceof PotionMeta && !colorHex.isEmpty()) {
                PotionMeta pMeta = (PotionMeta) bottleMeta;
                pMeta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
                pMeta.setColor(hexToColor(colorHex));
            }
            bottle.setItemMeta(bottleMeta);
        }
        inv.setItem(1, bottle);

        inv.setItem(2, target.getInventory().getItemInOffHand());
        inv.setItem(3, target.getInventory().getItemInMainHand());

        ItemStack decor = createGuiItem("gui.decor", target);
        inv.setItem(4, decor);
        for (int i = 9; i <= 15; i++) {
            inv.setItem(i, decor);
        }

        if (viewer.hasPermission("astinvsee.admin") && config.getBoolean("saves.enabled", true)) {
            inv.setItem(16, createGuiItem("gui.save-btn", target));
            inv.setItem(17, createGuiItem("gui.load-btn", target));
        } else {
            inv.setItem(16, decor);
            inv.setItem(17, decor);
        }

        inv.setItem(5, target.getInventory().getHelmet());
        inv.setItem(6, target.getInventory().getChestplate());
        inv.setItem(7, target.getInventory().getLeggings());
        inv.setItem(8, target.getInventory().getBoots());

        for (int i = 0; i < 36; i++) {
            inv.setItem(i + 18, target.getInventory().getItem(i));
        }
    }

    private ItemStack createGuiItem(String path, OfflinePlayer target) {
        FileConfiguration config = Main.getInstance().getConfig();
        Material mat = Material.matchMaterial(config.getString(path + ".material", "STONE"));
        if (mat == null) mat = Material.STONE;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (config.contains(path + ".name")) {
                meta.setDisplayName(ColorUtils.colorize(target, config.getString(path + ".name")));
            }
            if (config.contains(path + ".lore")) {
                List<String> lore = new ArrayList<>();
                for (String line : config.getStringList(path + ".lore")) {
                    lore.add(ColorUtils.colorize(target, line));
                }
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private String getTranslatedEffect(PotionEffectType type) {
        FileConfiguration config = Main.getInstance().getConfig();
        String path = "effects." + type.getName();
        if (config.contains(path)) {
            return config.getString(path);
        }
        return type.getName();
    }

    private Color hexToColor(String hex) {
        hex = hex.replace("#", "");
        if (hex.length() != 6) return Color.WHITE;
        try {
            return Color.fromRGB(
                    Integer.valueOf(hex.substring(0, 2), 16),
                    Integer.valueOf(hex.substring(2, 4), 16),
                    Integer.valueOf(hex.substring(4, 6), 16)
            );
        } catch (Exception e) {
            return Color.WHITE;
        }
    }

    public void closeAll() {
        for (UUID uuid : activeViews.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.closeInventory();
            }
        }
        activeViews.clear();
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        activeViews.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        handleTargetLeave(event.getPlayer());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        handleTargetLeave(event.getEntity());
    }

    private void handleTargetLeave(Player target) {
        List<UUID> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, UUID> entry : activeViews.entrySet()) {
            if (entry.getValue().equals(target.getUniqueId())) {
                Player viewer = Bukkit.getPlayer(entry.getKey());
                if (viewer != null && viewer.isOnline()) {
                    viewer.closeInventory();
                    viewer.sendMessage(ColorUtils.colorize(viewer, Main.getInstance().getConfig().getString("messages.offline")));
                }
                toRemove.add(entry.getKey());
            }
        }
        for (UUID uuid : toRemove) {
            activeViews.remove(uuid);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (activeViews.containsValue(player.getUniqueId())) {
            boolean isTopInv = event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory());
            if (!activeViews.containsKey(player.getUniqueId()) || !isTopInv) {
                checkFreeze(player, event);
                if (event.isCancelled()) return;
            }
        }

        if (!activeViews.containsKey(player.getUniqueId())) return;

        UUID targetId = activeViews.get(player.getUniqueId());
        Player target = Bukkit.getPlayer(targetId);
        boolean isAdmin = player.hasPermission("astinvsee.admin");

        if (event.getClick() == ClickType.DROP || event.getClick() == ClickType.CONTROL_DROP) {
            if (!isAdmin) {
                event.setCancelled(true);
                return;
            }
        }

        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv != null && event.getView().getTopInventory().equals(clickedInv)) {
            int slot = event.getSlot();

            if (slot >= 0 && slot <= 17) {
                if (slot == 16 && isAdmin && Main.getInstance().getConfig().getBoolean("saves.enabled", true) && target != null && target.isOnline()) {
                    event.setCancelled(true);
                    Main.getInstance().getSaveManager().startSavePrompt(player, target.getInventory().getContents(), target.getInventory().getArmorContents(), target.getInventory().getItemInOffHand());
                    return;
                }

                if (slot == 17 && isAdmin && Main.getInstance().getConfig().getBoolean("saves.enabled", true) && target != null && target.isOnline()) {
                    event.setCancelled(true);
                    player.closeInventory();
                    Main.getInstance().getSaveManager().openSavesGUI(player, target, 0);
                    return;
                }

                if (isAdmin && (slot == 2 || slot == 3 || (slot >= 5 && slot <= 8))) {
                    if (target != null && target.isOnline()) {
                        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                            syncToTarget(event.getView().getTopInventory(), target);
                            LogUtils.log(player.getName() + " изменил инвентарь игрока " + target.getName());
                        });
                    }
                } else {
                    event.setCancelled(true);
                }
                return;
            }

            if (!isAdmin) {
                event.setCancelled(true);
            } else if (target != null && target.isOnline()) {
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    syncToTarget(event.getView().getTopInventory(), target);
                    LogUtils.log(player.getName() + " изменил инвентарь игрока " + target.getName());
                });
            }
        } else if (event.isShiftClick() && !player.hasPermission("astinvsee.admin")) {
            event.setCancelled(true);
        } else if (event.isShiftClick() && player.hasPermission("astinvsee.admin") && target != null && target.isOnline()) {
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                syncToTarget(event.getView().getTopInventory(), target);
                LogUtils.log(player.getName() + " изменил инвентарь игрока " + target.getName());
            });
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (activeViews.containsValue(player.getUniqueId())) {
            boolean touchesTop = false;
            for (int slot : event.getRawSlots()) {
                if (slot < event.getView().getTopInventory().getSize()) touchesTop = true;
            }
            if (!activeViews.containsKey(player.getUniqueId()) || !touchesTop) {
                checkFreeze(player, event);
                if (event.isCancelled()) return;
            }
        }

        if (!activeViews.containsKey(player.getUniqueId())) return;

        boolean isAdmin = player.hasPermission("astinvsee.admin");
        boolean touchesTop = false;

        for (int slot : event.getRawSlots()) {
            if (slot < 54) {
                touchesTop = true;
                if (slot == 2 || slot == 3 || (slot >= 5 && slot <= 8)) {
                    if (!isAdmin) {
                        event.setCancelled(true);
                        return;
                    }
                } else if (slot <= 17 || !isAdmin) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (touchesTop && isAdmin) {
            UUID targetId = activeViews.get(player.getUniqueId());
            Player target = Bukkit.getPlayer(targetId);
            if (target != null && target.isOnline()) {
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    syncToTarget(event.getView().getTopInventory(), target);
                    LogUtils.log(player.getName() + " изменил инвентарь игрока " + target.getName());
                });
            }
        }
    }

    @EventHandler
    public void onTargetDrop(PlayerDropItemEvent event) {
        checkFreeze(event.getPlayer(), event);
    }

    @EventHandler
    public void onTargetPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            checkFreeze((Player) event.getEntity(), event);
        }
    }

    private void checkFreeze(Player target, org.bukkit.event.Cancellable event) {
        if (!Main.getInstance().getConfig().getBoolean("freeze-when-viewed", true)) return;

        for (Map.Entry<UUID, UUID> entry : activeViews.entrySet()) {
            if (entry.getValue().equals(target.getUniqueId())) {
                Player viewer = Bukkit.getPlayer(entry.getKey());
                if (viewer != null && viewer.hasPermission("astinvsee.admin")) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    private void syncToTarget(Inventory gui, Player target) {
        for (int i = 0; i < 36; i++) {
            target.getInventory().setItem(i, gui.getItem(i + 18));
        }
        target.getInventory().setHelmet(gui.getItem(5));
        target.getInventory().setChestplate(gui.getItem(6));
        target.getInventory().setLeggings(gui.getItem(7));
        target.getInventory().setBoots(gui.getItem(8));
        target.getInventory().setItemInOffHand(gui.getItem(2));
        target.getInventory().setItemInMainHand(gui.getItem(3));
    }
}