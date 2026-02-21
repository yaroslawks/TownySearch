package org.fedorov.townysearch.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.fedorov.townysearch.TownySearch;
import java.util.List;
import java.util.Map;

public class MessageManager {
    private final TownySearch plugin;
    private final MiniMessage mm;

    public MessageManager(TownySearch plugin) {
        this.plugin = plugin;
        this.mm = MiniMessage.miniMessage();
    }

    public void send(Player p, String path) {
        send(p, path, Map.of());
    }

    public void send(Player p, String path, Map<String, String> ph) {
        String msg = plugin.getConfig().getString("messages." + path);
        if (msg == null || msg.isEmpty()) return;
        for (Map.Entry<String, String> e : ph.entrySet()) {
            msg = msg.replace("<" + e.getKey() + ">", e.getValue());
        }
        p.sendMessage(mm.deserialize(msg));
    }

    public Component buildSearchMsg(Player s) {
        List<String> lines = plugin.getConfig().getStringList("messages.search");
        if (lines.isEmpty()) return Component.empty();
        Component msg = Component.empty();
        boolean first = true;
        for (String line : lines) {
            if (!first) msg = msg.append(Component.newline());
            first = false;
            if (line.equals("[buttons]")) {
                msg = msg.append(buildButtons(s));
            } else {
                msg = msg.append(mm.deserialize(line.replace("<player>", s.getName())));
            }
        }
        return msg;
    }

    public Component buildIgnoreEntry(String name) {
        String entry = plugin.getConfig().getString("messages.ignore-entry");
        if (entry == null || entry.isEmpty()) return Component.empty();
        entry = entry.replace("<player>", name);
        String hover = plugin.getConfig().getString("messages.remove-hover");
        Component hText = (hover != null && !hover.isEmpty()) ? mm.deserialize(hover) : Component.empty();
        Component comp = mm.deserialize(entry);
        if (!hText.equals(Component.empty())) comp = comp.hoverEvent(HoverEvent.showText(hText));
        return comp.clickEvent(ClickEvent.runCommand("/tsearch ignore remove " + name));
    }

    private Component buildButtons(Player s) {
        String accept = plugin.getConfig().getString("messages.accept-btn");
        String ignore = plugin.getConfig().getString("messages.ignore-btn");
        Component aBtn = buildButton(accept, "accept-hover", "/t invite " + s.getName());
        Component iBtn = buildButton(ignore, "ignore-hover", "/tsearch ignore add " + s.getName());
        if (aBtn != null && iBtn != null) return aBtn.append(Component.space()).append(iBtn);
        return aBtn != null ? aBtn : iBtn;
    }

    private Component buildButton(String text, String hoverKey, String cmd) {
        if (text == null || text.isEmpty()) return null;
        Component btn = mm.deserialize(text);
        String hover = plugin.getConfig().getString("messages." + hoverKey);
        if (hover != null && !hover.isEmpty()) btn = btn.hoverEvent(HoverEvent.showText(mm.deserialize(hover)));
        return btn.clickEvent(ClickEvent.runCommand(cmd));
    }
}