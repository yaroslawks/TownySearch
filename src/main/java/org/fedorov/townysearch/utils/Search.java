package org.fedorov.townysearch.utils;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.fedorov.townysearch.TownySearch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Search {
    private final TownySearch plugin;
    private final IgnorelistManager ignore;
    private final MessageManager msg;
    private final TownyAPI towny;
    private final Map<UUID, Long> cooldowns;

    public Search(TownySearch plugin, IgnorelistManager ignore, MessageManager msg) {
        this.plugin = plugin;
        this.ignore = ignore;
        this.msg = msg;
        this.towny = TownyAPI.getInstance();
        this.cooldowns = new HashMap<>();
    }

    public boolean canSearch(Player p) {
        Resident r = towny.getResident(p.getUniqueId());
        Town t = r != null ? r.getTownOrNull() : null;
        if (t != null) {
            msg.send(p, "has-town");
            return false;
        }
        return !checkCooldown(p);
    }

    private boolean checkCooldown(Player p) {
        long cdTime = plugin.getConfig().getLong("cooldown", 60) * 1000;
        long now = System.currentTimeMillis();
        if (cooldowns.containsKey(p.getUniqueId())) {
            long last = cooldowns.get(p.getUniqueId());
            if (now - last < cdTime) {
                long rem = (cdTime - (now - last)) / 1000;
                msg.send(p, "cooldown", Map.of("time", String.valueOf(rem)));
                return true;
            }
        }
        cooldowns.put(p.getUniqueId(), now);
        return false;
    }

    public void broadcast(Player s) {
        Component msg = this.msg.buildSearchMsg(s);
        List<String> roles = plugin.getConfig().getStringList("allowed-roles");
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (shouldReceive(p, s, roles)) {
                p.sendMessage(msg);
            }
        }
        this.msg.send(s, "search-sent");
    }

    private boolean shouldReceive(Player r, Player s, List<String> roles) {
        Resident res = towny.getResident(r.getUniqueId());
        if (res == null) return false;
        Town t = res.getTownOrNull();
        if (t == null) return false;

        return hasRole(res, t, roles) && !ignore.isIgnored(r.getUniqueId(), s.getUniqueId());
    }

    private boolean hasRole(Resident r, Town t, List<String> roles) {
        if (roles.contains("mayor") && isMayor(r, t)) return true;
        if (roles.contains("assistant") && isAssistant(r)) return true;
        if (roles.contains("resident") && t.hasResident(r)) return true;

        for (String role : roles) {
            if (!List.of("mayor", "assistant", "resident").contains(role) && hasRank(r, role)) {
                return true;
            }
        }
        return false;
    }

    private boolean isMayor(Resident r, Town t) {
        return t.getMayor() != null && t.getMayor().getUUID().equals(r.getUUID());
    }

    private boolean isAssistant(Resident r) {
        return hasRank(r, "assistant");
    }

    private boolean hasRank(Resident r, String rank) {
        try {
            List<String> ranks = r.getTownRanks();
            return ranks != null && ranks.stream().anyMatch(rank::equalsIgnoreCase);
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка ранга: " + r.getName());
        }
        return false;
    }
}