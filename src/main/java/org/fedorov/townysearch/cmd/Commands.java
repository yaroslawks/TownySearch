package org.fedorov.townysearch.cmd;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.fedorov.townysearch.utils.IgnorelistManager;
import org.fedorov.townysearch.utils.MessageManager;
import org.fedorov.townysearch.utils.Search;
import org.fedorov.townysearch.TownySearch;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Commands implements CommandExecutor, TabCompleter {
    private final TownySearch plugin;
    private final Search search;
    private final IgnorelistManager ignore;
    private final MessageManager msg;

    public Commands(TownySearch plugin, Search search, IgnorelistManager ignore, MessageManager msg) {
        this.plugin = plugin;
        this.search = search;
        this.ignore = ignore;
        this.msg = msg;
    }
    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command cmd, @NotNull String lbl, String[] a) {
        if (!(s instanceof Player p)) {
            s.sendMessage("Только игроки могут использовать ЭТУ команду!");
            return true;
        }

        if (a.length == 0) {
            handleSearch(p);
            return true;
        }

        switch (a[0].toLowerCase()) {
            case "ignore":
                handleIgnore(p, Arrays.copyOfRange(a, 1, a.length));
                return true;
            case "reload":
                handleReload(p);
                return true;
            default:
                msg.send(p, "unknown-cmd");
                return true;
        }
    }
    @Override
    public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command cmd, @NotNull String alias, String[] a) {
        List<String> comp = new ArrayList<>();
        if (a.length == 1) {
            comp.add("ignore");
            comp.add("reload");
        } else if (a.length == 2 && a[0].equalsIgnoreCase("ignore")) {
            comp.add("add");
            comp.add("remove");
            if (s instanceof Player) {
                int pages = ignore.getPages((Player) s);
                for (int i = 1; i <= Math.min(pages, 5); i++) {
                    comp.add(String.valueOf(i));
                }
            }
        }
        return comp;
    }

    private void handleSearch(Player p) {
        if (search.canSearch(p)) {
            search.broadcast(p);
        }
    }

    private void handleIgnore(Player p, String[] a) {
        if (!p.hasPermission("townysearch.ignore")) {
            msg.send(p, "no-perm");
            return;
        }

        if (a.length == 0) {
            ignore.show(p, 1);
            return;
        }

        try {
            int page = Integer.parseInt(a[0]);
            ignore.show(p, page);
            return;
        } catch (NumberFormatException ignored) {}

        switch (a[0].toLowerCase()) {
            case "add":
                if (a.length > 1) ignore.add(p, a[1]);
                else msg.send(p, "usage-add");
                break;
            case "remove":
                if (a.length > 1) ignore.remove(p, a[1]);
                else msg.send(p, "usage-remove");
                break;
            default:
                msg.send(p, "usage-ignore");
        }
    }

    private void handleReload(Player p) {
        if (!p.hasPermission("townysearch.reload")) {
            msg.send(p, "no-perm");
            return;
        }
        plugin.reloadConfig();
        ignore.load();
        msg.send(p, "reload-ok");
    }
}