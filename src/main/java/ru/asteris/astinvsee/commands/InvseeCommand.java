package ru.asteris.astinvsee.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.asteris.astinvsee.Main;
import ru.asteris.astlib.commands.AbstractCommand;
import ru.asteris.astlib.utils.ColorUtils;

import java.util.ArrayList;
import java.util.List;

public class InvseeCommand extends AbstractCommand {

    public InvseeCommand() {
        super("invsee");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;

        if (!player.hasPermission("astinvsee.use")) {
            player.sendMessage(ColorUtils.colorize(Main.getInstance().getConfig().getString("messages.no-permission")));
            return;
        }

        if (args.length != 1) {
            player.sendMessage(ColorUtils.colorize(Main.getInstance().getConfig().getString("messages.usage-invsee")));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(ColorUtils.colorize(Main.getInstance().getConfig().getString("messages.offline")));
            return;
        }

        if (target.isOnline()) {
            Main.getInstance().getInvseeManager().openInvsee(player, target.getPlayer());
        } else {
            Main.getInstance().getInvseeManager().openOfflineInvsee(player, target);
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
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