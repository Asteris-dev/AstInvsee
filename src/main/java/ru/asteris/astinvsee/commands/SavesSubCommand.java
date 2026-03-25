package ru.asteris.astinvsee.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.asteris.astinvsee.Main;
import ru.asteris.astlib.commands.SubCommand;
import ru.asteris.astlib.utils.ColorUtils;

import java.util.ArrayList;
import java.util.List;

public class SavesSubCommand extends SubCommand {

    @Override
    public String getName() {
        return "saves";
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
            for (String line : Main.getInstance().getConfig().getStringList("messages.usage-astinvsee")) {
                player.sendMessage(ColorUtils.colorize(line));
            }
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        Main.getInstance().getSaveManager().openSavesGUI(player, target, 0);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 2) {
            String partialName = args[1].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(partialName)) {
                    completions.add(p.getName());
                }
            }
        }
        return completions;
    }
}