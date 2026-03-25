package ru.asteris.astinvsee.managers;

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
import org.bukkit.event.inventory.InventoryClickEvent;
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
import ru.asteris.astinvsee.Main;
import ru.asteris.astlib.utils.AstGui;
import ru.asteris.astlib.utils.ColorUtils;
import ru.asteris.astlib.utils.ItemBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

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

                    Inventory top = viewer.getOpenInventory().getTopInventory();
                    if (top.getHolder() instanceof AstGui) {
                        updateInventory((AstGui) top.getHolder(), target, viewer);
                    }
                }
            }
        }.runTaskTimer(Main.getInstance(), 10L, 10L);
    }

    public void openInvsee(Player viewer, Player target) {
        String title = Main.getInstance().getConfig().getString("messages.gui-title").replace("%player%", target.getName());
        AstGui gui = new AstGui(54, ColorUtils.colorize(target, title));

        boolean isAdmin = viewer.hasPermission("astinvsee.admin");
        gui.setCancelClicks(!isAdmin);
        gui.onClose(event -> activeViews.remove(viewer.getUniqueId()));

        updateInventory(gui, target, viewer);

        viewer.openInventory(gui.getInventory());
        activeViews.put(viewer.getUniqueId(), target.getUniqueId());
    }

    private void updateInventory(AstGui gui, Player target, Player viewer) {
        FileConfiguration config = Main.getInstance().getConfig();
        boolean isAdmin = viewer.hasPermission("astinvsee.admin");

        Consumer<InventoryClickEvent> syncAction = e -> {
            if (isAdmin) {
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> syncToTarget(e.getView().getTopInventory(), target));
            }
        };

        ItemStack head = new ItemBuilder(Material.PLAYER_HEAD).build();
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
        gui.setItem(0, head, e -> e.setCancelled(true));

        boolean hasEffects = !target.getActivePotionEffects().isEmpty();
        String colorHex = hasEffects ? config.getString("gui.bottle.filled-color", "") : config.getString("gui.bottle.empty-color", "");
        Material bottleMat = colorHex.isEmpty() ? Material.GLASS_BOTTLE : Material.POTION;

        ItemStack bottle = new ItemBuilder(bottleMat).build();
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
        gui.setItem(1, bottle, e -> e.setCancelled(true));

        gui.setItem(2, target.getInventory().getItemInOffHand(), syncAction);
        gui.setItem(3, target.getInventory().getItemInMainHand(), syncAction);

        ItemStack decor = createGuiItem("gui.decor", target);
        gui.setItem(4, decor, e -> e.setCancelled(true));
        for (int i = 9; i <= 15; i++) {
            gui.setItem(i, decor, e -> e.setCancelled(true));
        }

        if (isAdmin && config.getBoolean("saves.enabled", true)) {
            gui.setItem(16, createGuiItem("gui.save-btn", target), e -> {
                e.setCancelled(true);
                Main.getInstance().getSaveManager().startSavePrompt(viewer, target.getInventory().getContents(), target.getInventory().getArmorContents(), target.getInventory().getItemInOffHand());
            });
            gui.setItem(17, createGuiItem("gui.load-btn", target), e -> {
                e.setCancelled(true);
                viewer.closeInventory();
                Main.getInstance().getSaveManager().openSavesGUI(viewer, target, 0);
            });
        } else {
            gui.setItem(16, decor, e -> e.setCancelled(true));
            gui.setItem(17, decor, e -> e.setCancelled(true));
        }

        gui.setItem(5, target.getInventory().getHelmet(), syncAction);
        gui.setItem(6, target.getInventory().getChestplate(), syncAction);
        gui.setItem(7, target.getInventory().getLeggings(), syncAction);
        gui.setItem(8, target.getInventory().getBoots(), syncAction);

        for (int i = 0; i < 36; i++) {
            gui.setItem(i + 18, target.getInventory().getItem(i), syncAction);
        }
    }

    private ItemStack createGuiItem(String path, OfflinePlayer target) {
        FileConfiguration config = Main.getInstance().getConfig();
        Material mat = Material.matchMaterial(config.getString(path + ".material", "STONE"));
        if (mat == null) mat = Material.STONE;

        List<String> lore = new ArrayList<>();
        for (String line : config.getStringList(path + ".lore")) {
            lore.add(ColorUtils.colorize(target, line));
        }

        return new ItemBuilder(mat)
                .setName(ColorUtils.colorize(target, config.getString(path + ".name")))
                .setLore(lore)
                .build();
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
                    viewer.sendMessage(ColorUtils.colorize(Main.getInstance().getConfig().getString("messages.offline")));
                }
                toRemove.add(entry.getKey());
            }
        }
        for (UUID uuid : toRemove) {
            activeViews.remove(uuid);
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