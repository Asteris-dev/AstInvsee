package ru.asteris.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.asteris.Main;
import ru.asteris.utils.ColorUtils;

import java.util.ArrayList;
import java.util.List;

public class AstInvseeCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (!player.hasPermission("astinvsee.admin")) {
            player.sendMessage(ColorUtils.colorize(player, Main.getInstance().getConfig().getString("messages.no-permission")));
            return true;
        }

        if (args.length == 0) {
            for (String line : Main.getInstance().getConfig().getStringList("messages.usage-astinvsee")) {
                player.sendMessage(ColorUtils.colorize(player, line));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            Main.getInstance().reloadConfig();
            player.sendMessage(ColorUtils.colorize(player, Main.getInstance().getConfig().getString("messages.reloaded")));
            return true;
        }

        if (args[0].equalsIgnoreCase("saves")) {
            if (args.length < 2) {
                for (String line : Main.getInstance().getConfig().getStringList("messages.usage-astinvsee")) {
                    player.sendMessage(ColorUtils.colorize(player, line));
                }
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            Main.getInstance().getSaveManager().openSavesGUI(player, target, 0);
            return true;
        }

        for (String line : Main.getInstance().getConfig().getStringList("messages.usage-astinvsee")) {
            player.sendMessage(ColorUtils.colorize(player, line));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!sender.hasPermission("astinvsee.admin")) return completions;

        if (args.length == 1) {
            String partialName = args[0].toLowerCase();
            if ("reload".startsWith(partialName)) completions.add("reload");
            if ("saves".startsWith(partialName)) completions.add("saves");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("saves")) {
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