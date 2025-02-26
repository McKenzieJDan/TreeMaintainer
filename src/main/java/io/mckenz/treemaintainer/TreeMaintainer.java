package io.mckenz.treemaintainer;

import org.bukkit.plugin.java.JavaPlugin;

public class TreeMaintainer extends JavaPlugin {
    private boolean enabled;
    private boolean debug;
    private boolean replantingEnabled;
    private int replantingDelay;
    private boolean cleanupEnabled;
    private int cleanupMaxDistance;
    private boolean requireAxe;
    private boolean respectEfficiency;
    private boolean oakEnabled;
    private boolean spruceEnabled;
    private boolean birchEnabled;
    private boolean acaciaEnabled;
    private boolean darkOakEnabled;
    private boolean mangroveEnabled;
    private boolean cherryEnabled;
    private boolean jungleEnabled;

    @Override
    public void onEnable() {
        // Save default config if it doesn't exist
        saveDefaultConfig();
        
        // Load config values
        loadConfig();
        
        // Register our event listener
        getServer().getPluginManager().registerEvents(new TreeListener(this), this);
        getLogger().info("TreeMaintainer has been enabled!");
    }

    private void loadConfig() {
        enabled = getConfig().getBoolean("enabled", true);
        debug = getConfig().getBoolean("debug", false);
        
        // Tree Maintenance Settings
        replantingEnabled = getConfig().getBoolean("replanting.enabled", true);
        replantingDelay = getConfig().getInt("replanting.delay", 5);
        
        cleanupEnabled = getConfig().getBoolean("cleanup.enabled", true);
        cleanupMaxDistance = getConfig().getInt("cleanup.max-distance", 10);
        
        requireAxe = getConfig().getBoolean("tools.require-axe", true);
        respectEfficiency = getConfig().getBoolean("tools.respect-efficiency", true);
        
        // Tree Type Settings
        oakEnabled = getConfig().getBoolean("tree-types.oak", true);
        spruceEnabled = getConfig().getBoolean("tree-types.spruce", true);
        birchEnabled = getConfig().getBoolean("tree-types.birch", true);
        acaciaEnabled = getConfig().getBoolean("tree-types.acacia", true);
        darkOakEnabled = getConfig().getBoolean("tree-types.dark_oak", true);
        mangroveEnabled = getConfig().getBoolean("tree-types.mangrove", true);
        cherryEnabled = getConfig().getBoolean("tree-types.cherry", true);
        jungleEnabled = getConfig().getBoolean("tree-types.jungle", false);
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

    // Getters for all config values
    public boolean isPluginEnabled() {
        return enabled;
    }

    public boolean isReplantingEnabled() {
        return replantingEnabled;
    }

    public int getReplantingDelay() {
        return replantingDelay;
    }

    public boolean isCleanupEnabled() {
        return cleanupEnabled;
    }

    public int getCleanupMaxDistance() {
        return cleanupMaxDistance;
    }

    public boolean isRequireAxe() {
        return requireAxe;
    }

    public boolean isRespectEfficiency() {
        return respectEfficiency;
    }

    public boolean isTreeTypeEnabled(String treeType) {
        switch (treeType) {
            case "oak": return oakEnabled;
            case "spruce": return spruceEnabled;
            case "birch": return birchEnabled;
            case "acacia": return acaciaEnabled;
            case "dark_oak": return darkOakEnabled;
            case "mangrove": return mangroveEnabled;
            case "cherry": return cherryEnabled;
            case "jungle": return jungleEnabled;
            default: return false;
        }
    }
} 