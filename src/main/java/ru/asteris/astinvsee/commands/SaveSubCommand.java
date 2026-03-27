package ru.asteris.astinvsee.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.asteris.astinvsee.Main;
import ru.asteris.astinvsee.utils.LogUtils;
import ru.asteris.astinvsee.utils.SaveUtils;
import ru.asteris.astlib.commands.SubCommand;
import ru.asteris.astlib.utils.ColorUtils;

public class SaveSubCommand extends SubCommand {

    @Override
    public String getName() {
        return "save";
    }

    @Override
    public String getPermission() {
        return "astinvsee.admin";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;

        if (args.length < 2) {
            player.sendMessage(ColorUtils.colorize(Main.getInstance().getConfig().getString("messages.usage-save")));
            return;
        }

        String saveName = args[1];
        SaveUtils.createSave(saveName, player.getInventory().getContents(), player.getInventory().getArmorContents(), player.getInventory().getItemInOffHand());

        String msg = Main.getInstance().getConfig().getString("messages.save-success").replace("%name%", saveName);
        player.sendMessage(ColorUtils.colorize(msg));
        LogUtils.log(player.getName() + " создал сохранение своего инвентаря: " + saveName);
    }
}