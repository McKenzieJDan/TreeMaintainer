package io.mckenz.treemaintainer;

import org.bukkit.plugin.java.JavaPlugin;

public class TreeMaintainer extends JavaPlugin {
    @Override
    public void onEnable() {
        // Register our event listener
        getServer().getPluginManager().registerEvents(new TreeListener(this), this);
        getLogger().info("TreeMaintainer has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("TreeMaintainer has been disabled!");
    }
} 