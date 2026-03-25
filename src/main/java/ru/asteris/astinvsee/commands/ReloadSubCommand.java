package ru.asteris.astinvsee.commands;

import org.bukkit.command.CommandSender;
import ru.asteris.astinvsee.Main;
import ru.asteris.astlib.commands.SubCommand;
import ru.asteris.astlib.utils.ColorUtils;

public class ReloadSubCommand extends SubCommand {

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getPermission() {
        return "astinvsee.admin";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Main.getInstance().reloadConfig();
        sender.sendMessage(ColorUtils.colorize(Main.getInstance().getConfig().getString("messages.reloaded")));
    }
}