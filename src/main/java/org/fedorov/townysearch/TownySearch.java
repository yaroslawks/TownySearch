package org.fedorov.townysearch;

import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;
import org.fedorov.townysearch.cmd.Commands;
import org.fedorov.townysearch.utils.IgnorelistManager;
import org.fedorov.townysearch.utils.MessageManager;
import org.fedorov.townysearch.utils.Search;

import java.util.Objects;

public class TownySearch extends JavaPlugin {

    private IgnorelistManager ignore;
    private MessageManager msg;

    @Override
    public void onEnable() {
        this.msg = new MessageManager(this);
        this.ignore = new IgnorelistManager(this);
        Search search = new Search(this, ignore, msg);
        Commands cmd = new Commands(this, search, ignore, msg);

        saveDefaultConfig();
        ignore.load();

        Objects.requireNonNull(getCommand("tsearch")).setExecutor(cmd);
        Objects.requireNonNull(getCommand("tsearch")).setTabCompleter(cmd);

        int pluginId = 27658;
        new Metrics(this, pluginId);

        sendBannerTS();
    }

    private void sendBannerTS() {
        getLogger().info("=================================================");
        getLogger().info("TownySearch");
        getLogger().info("Плагин успешно запустился!");
        getLogger().info("Автор: fedorov");
        getLogger().info("Версия: " + getDescription().getVersion());
        getLogger().info("=================================================");
    }

    @Override
    public void onDisable() {
        ignore.save();
        getLogger().info("Плагин выключен!");
    }

    public MessageManager getMessageManager() {
        return msg;
    }
}