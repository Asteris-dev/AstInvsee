package ru.asteris.astinvsee.commands;

import org.bukkit.command.CommandSender;
import ru.asteris.astinvsee.Main;
import ru.asteris.astlib.commands.AbstractCommand;
import ru.asteris.astlib.utils.ColorUtils;

public class AstInvseeCommand extends AbstractCommand {

    public AstInvseeCommand() {
        super("astinvsee");
        addSubCommand(new ReloadSubCommand());
        addSubCommand(new SavesSubCommand());
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("astinvsee.admin")) {
            sender.sendMessage(ColorUtils.colorize(Main.getInstance().getConfig().getString("messages.no-permission")));
            return;
        }

        for (String line : Main.getInstance().getConfig().getStringList("messages.usage-astinvsee")) {
            sender.sendMessage(ColorUtils.colorize(line));
        }
    }
}