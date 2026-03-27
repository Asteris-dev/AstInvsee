package ru.asteris.astinvsee.utils;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTCompoundList;
import de.tr7zw.changeme.nbtapi.NBTFile;
import de.tr7zw.changeme.nbtapi.NBTItem;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import java.io.File;

public class OfflineUtils {

    private static File getPlayerDataFile(OfflinePlayer player) {
        return new File(Bukkit.getWorlds().get(0).getWorldFolder(), "playerdata/" + player.getUniqueId() + ".dat");
    }

    public static void loadOfflineInventory(OfflinePlayer player, ItemStack[] contents, ItemStack[] armor, ItemStack[] offhand) {
        File file = getPlayerDataFile(player);
        if (!file.exists()) return;

        try {
            NBTFile nbtFile = new NBTFile(file);
            NBTCompoundList inventory = nbtFile.getCompoundList("Inventory");

            for (ReadWriteNBT rw : inventory) {
                int slot = rw.getByte("Slot");
                ItemStack item = NBTItem.convertNBTtoItem((NBTCompound) rw);

                if (item == null) continue;

                if (slot >= 0 && slot < 36) {
                    contents[slot] = item;
                } else if (slot >= 100 && slot <= 103) {
                    armor[slot - 100] = item;
                } else if (slot == -106) {
                    offhand[0] = item;
                }
            }
        } catch (Exception ignored) {}
    }

    public static void saveOfflineInventory(OfflinePlayer player, ItemStack[] contents, ItemStack[] armor, ItemStack[] offhand) {
        File file = getPlayerDataFile(player);
        if (!file.exists()) return;

        try {
            NBTFile nbtFile = new NBTFile(file);
            nbtFile.getCompoundList("Inventory").clear();
            NBTCompoundList inventory = nbtFile.getCompoundList("Inventory");

            for (int i = 0; i < contents.length; i++) {
                if (contents[i] != null && !contents[i].getType().isAir()) {
                    ReadWriteNBT itemData = inventory.addCompound();
                    itemData.mergeCompound(NBTItem.convertItemtoNBT(contents[i]));
                    itemData.setByte("Slot", (byte) i);
                }
            }

            for (int i = 0; i < armor.length; i++) {
                if (armor[i] != null && !armor[i].getType().isAir()) {
                    ReadWriteNBT itemData = inventory.addCompound();
                    itemData.mergeCompound(NBTItem.convertItemtoNBT(armor[i]));
                    itemData.setByte("Slot", (byte) (100 + i));
                }
            }

            if (offhand[0] != null && !offhand[0].getType().isAir()) {
                ReadWriteNBT itemData = inventory.addCompound();
                itemData.mergeCompound(NBTItem.convertItemtoNBT(offhand[0]));
                itemData.setByte("Slot", (byte) -106);
            }

            nbtFile.save();
        } catch (Exception ignored) {}
    }
}