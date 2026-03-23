package ru.asteris.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.asteris.Main;
import ru.asteris.utils.ColorUtils;

import java.util.ArrayList;
import java.util.List;

public class InvseeCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (!player.hasPermission("astinvsee.use")) {
            player.sendMessage(ColorUtils.colorize(player, Main.getInstance().getConfig().getString("messages.no-permission")));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ColorUtils.colorize(player, Main.getInstance().getConfig().getString("messages.usage-invsee")));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null || !target.isOnline()) {
            player.sendMessage(ColorUtils.colorize(player, Main.getInstance().getConfig().getString("messages.offline")));
            return true;
        }

        if (player.equals(target)) {
            player.sendMessage(ColorUtils.colorize(player, Main.getInstance().getConfig().getString("messages.self")));
            return true;
        }

        Main.getInstance().getInvseeManager().openInvsee(player, target);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1 && sender.hasPermission("astinvsee.use")) {
            String partialName = args[0].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(partialName)) {
                    completions.add(p.getName());
                }
            }
        }
        return completions;
    }
}