package io.mckenz.treemaintainer;

import org.bukkit.plugin.java.JavaPlugin;

public class TreeMaintainer extends JavaPlugin {
    private boolean debug;

    @Override
    public void onEnable() {
        // Save default config if it doesn't exist
        saveDefaultConfig();
        
        // Load config values
        debug = getConfig().getBoolean("debug", false);
        
        // Register our event listener
        getServer().getPluginManager().registerEvents(new TreeListener(this), this);
        getLogger().info("TreeMaintainer has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("TreeMaintainer has been disabled!");
    }

    public void debug(String message) {
        if (debug) {
            getLogger().info("[Debug] " + message);
        }
    }
} 