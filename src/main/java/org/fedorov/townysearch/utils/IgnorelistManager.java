package org.fedorov.townysearch.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.fedorov.townysearch.TownySearch;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class IgnorelistManager {
    private final TownySearch plugin;
    private final MessageManager msg;
    private final Map<UUID, Set<UUID>> data;
    private final File file;
    private static final int PER_PAGE = 10;

    public IgnorelistManager(TownySearch plugin) {
        this.plugin = plugin;
        this.msg = plugin.getMessageManager();
        this.data = new HashMap<>();
        this.file = new File(plugin.getDataFolder(), "ignore.yml");
    }

    public void load() {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Не создан ignore.yml: " + e.getMessage());
            }
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (String key : cfg.getKeys(false)) {
            try {
                UUID uid = UUID.fromString(key);
                List<String> list = cfg.getStringList(key);
                Set<UUID> set = list.stream().map(UUID::fromString).collect(Collectors.toSet());
                data.put(uid, set);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Неверный UUID: " + key);
            }
        }
    }

    public void save() {
        FileConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, Set<UUID>> e : data.entrySet()) {
            List<String> list = e.getValue().stream().map(UUID::toString).collect(Collectors.toList());
            cfg.set(e.getKey().toString(), list);
        }
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Не сохранен ignore.yml: " + e.getMessage());
        }
    }

    public void add(Player p, String target) {
        Player t = Bukkit.getPlayer(target);
        if (t == null) {
            msg.send(p, "player-not-found");
            return;
        }
        data.computeIfAbsent(p.getUniqueId(), k -> new HashSet<>()).add(t.getUniqueId());
        msg.send(p, "ignore-added");
    }

    public void remove(Player p, String target) {
        Player t = Bukkit.getPlayer(target);
        if (t == null) {
            msg.send(p, "player-not-found");
            return;
        }
        Set<UUID> set = data.get(p.getUniqueId());
        if (set != null && set.remove(t.getUniqueId())) {
            msg.send(p, "ignore-removed");
        } else {
            msg.send(p, "not-in-ignore");
        }
    }

    public void show(Player p, int page) {
        Set<UUID> set = data.getOrDefault(p.getUniqueId(), new HashSet<>());
        if (set.isEmpty()) {
            msg.send(p, "ignore-empty");
            return;
        }

        List<UUID> list = new ArrayList<>(set);
        int pages = (int) Math.ceil((double) list.size() / PER_PAGE);
        if (page < 1) page = 1;
        if (page > pages) page = pages;

        final int finalPage = page;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

            List<Component> messagesToSend = new ArrayList<>();

            String header = plugin.getConfig().getString("messages.ignore-header");
            if (header != null && !header.isEmpty()) {
                messagesToSend.add(MiniMessage.miniMessage().deserialize(header));
            }

            int start = (finalPage - 1) * PER_PAGE;
            int end = Math.min(start + PER_PAGE, list.size());

            for (int i = start; i < end; i++) {
                UUID id = list.get(i);
                String name = Bukkit.getOfflinePlayer(id).getName();
                if (name == null) continue;
                messagesToSend.add(msg.buildIgnoreEntry(name));
            }

            messagesToSend.add(buildNav(finalPage, pages));

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (p.isOnline()) {
                    for (Component c : messagesToSend) {
                        p.sendMessage(c);
                    }
                }
            });
        });
    }

    private Component buildNav(int cur, int total) {
        MiniMessage mm = MiniMessage.miniMessage();
        Component nav = Component.empty();
        String prev = plugin.getConfig().getString("messages.prev-btn");
        String next = plugin.getConfig().getString("messages.next-btn");
        String prevH = plugin.getConfig().getString("messages.prev-hover");
        String nextH = plugin.getConfig().getString("messages.next-hover");
        String info = plugin.getConfig().getString("messages.page-info");
        if (prev != null && !prev.isEmpty()) {
            Component btn = mm.deserialize(prev);
            if (cur > 1) {
                if (prevH != null && !prevH.isEmpty()) btn = btn.hoverEvent(HoverEvent.showText(mm.deserialize(prevH)));
                btn = btn.clickEvent(ClickEvent.runCommand("/tsearch ignore " + (cur - 1)));
            } else {
                String inactive = plugin.getConfig().getString("messages.prev-inactive");
                if (inactive != null && !inactive.isEmpty()) btn = mm.deserialize(inactive);
                String hover = plugin.getConfig().getString("messages.first-hover");
                if (hover != null && !hover.isEmpty()) btn = btn.hoverEvent(HoverEvent.showText(mm.deserialize(hover)));
            }
            nav = nav.append(btn).append(Component.space());
        }
        if (info != null && !info.isEmpty()) {
            String text = info.replace("<cur>", String.valueOf(cur)).replace("<total>", String.valueOf(total));
            nav = nav.append(mm.deserialize(text));
        }
        if (next != null && !next.isEmpty()) {
            if (info != null && !info.isEmpty()) nav = nav.append(Component.space());
            Component btn = mm.deserialize(next);
            if (cur < total) {
                if (nextH != null && !nextH.isEmpty()) btn = btn.hoverEvent(HoverEvent.showText(mm.deserialize(nextH)));
                btn = btn.clickEvent(ClickEvent.runCommand("/tsearch ignore " + (cur + 1)));
            } else {
                String inactive = plugin.getConfig().getString("messages.next-inactive");
                if (inactive != null && !inactive.isEmpty()) btn = mm.deserialize(inactive);
                String hover = plugin.getConfig().getString("messages.last-hover");
                if (hover != null && !hover.isEmpty()) btn = btn.hoverEvent(HoverEvent.showText(mm.deserialize(hover)));
            }
            nav = nav.append(btn);
        }
        return nav;
    }

    public boolean isIgnored(UUID uid1, UUID uid2) {
        return data.getOrDefault(uid1, new HashSet<>()).contains(uid2);
    }

    public int getPages(Player p) {
        Set<UUID> set = data.getOrDefault(p.getUniqueId(), new HashSet<>());
        return (int) Math.ceil((double) set.size() / PER_PAGE);
    }
}