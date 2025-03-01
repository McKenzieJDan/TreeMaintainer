package io.mckenz.treemaintainer;

import io.mckenz.treemaintainer.commands.TreeMaintainerCommand;
import io.mckenz.treemaintainer.listeners.TreeBreakListener;
import io.mckenz.treemaintainer.models.TreeType;
import io.mckenz.treemaintainer.services.CleanupService;
import io.mckenz.treemaintainer.services.ReplantingService;
import io.mckenz.treemaintainer.services.TreeDetectionService;
import io.mckenz.treemaintainer.services.impl.CleanupServiceImpl;
import io.mckenz.treemaintainer.services.impl.ReplantingServiceImpl;
import io.mckenz.treemaintainer.services.impl.TreeDetectionServiceImpl;
import io.mckenz.treemaintainer.utils.UpdateChecker;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Main plugin class for TreeMaintainer.
 */
public class TreeMaintainer extends JavaPlugin {
    private boolean enabled;
    private boolean debug;
    private boolean replantingEnabled;
    private int replantingDelay;
    private boolean cleanupEnabled;
    private int cleanupMaxDistance;
    private boolean cleanupLargeTrees;
    private boolean requireAxe;
    private boolean respectEfficiency;
    private Map<String, Boolean> enabledTreeTypes;
    
    // Update checker settings
    private boolean updateCheckerEnabled;
    private int updateCheckerResourceId;
    private boolean updateCheckerNotifyAdmins;
    
    // Services
    private TreeDetectionService treeDetectionService;
    private ReplantingService replantingService;
    private CleanupService cleanupService;
    private UpdateChecker updateChecker;

    @Override
    public void onEnable() {
        try {
            // Save default config if it doesn't exist
            saveDefaultConfig();
            
            // Load config values
            loadConfig();
            
            // Initialize services
            initializeServices();
            
            // Register our event listener
            registerListeners();
            
            // Register commands
            registerCommands();
            
            // Initialize update checker
            initializeUpdateChecker();
            
            getLogger().info("TreeMaintainer has been enabled!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error enabling TreeMaintainer: " + e.getMessage(), e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void initializeServices() {
        try {
            treeDetectionService = new TreeDetectionServiceImpl(this);
            replantingService = new ReplantingServiceImpl(this);
            cleanupService = new CleanupServiceImpl(this, treeDetectionService);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error initializing services: " + e.getMessage(), e);
            throw e; // Re-throw to be caught by onEnable
        }
    }
    
    private void registerListeners() {
        try {
            getServer().getPluginManager().registerEvents(
                new TreeBreakListener(this, treeDetectionService, replantingService, cleanupService), 
                this
            );
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error registering event listeners: " + e.getMessage(), e);
            throw e; // Re-throw to be caught by onEnable
        }
    }
    
    private void registerCommands() {
        try {
            TreeMaintainerCommand command = new TreeMaintainerCommand(this);
            getCommand("treemaintainer").setExecutor(command);
            getCommand("treemaintainer").setTabCompleter(command);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error registering commands: " + e.getMessage(), e);
            throw e; // Re-throw to be caught by onEnable
        }
    }
    
    private void initializeUpdateChecker() {
        try {
            if (updateCheckerEnabled) {
                updateChecker = new UpdateChecker(this, updateCheckerResourceId);
                updateChecker.checkForUpdates();
            }
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Error initializing update checker: " + e.getMessage(), e);
            // Don't re-throw, as update checker is not critical
        }
    }

    private void loadConfig() {
        try {
            enabled = getConfig().getBoolean("enabled", true);
            debug = getConfig().getBoolean("debug", false);
            
            // Tree Maintenance Settings
            replantingEnabled = getConfig().getBoolean("replanting.enabled", true);
            replantingDelay = getConfig().getInt("replanting.delay", 5);
            
            cleanupEnabled = getConfig().getBoolean("cleanup.enabled", true);
            cleanupMaxDistance = getConfig().getInt("cleanup.max-distance", 10);
            cleanupLargeTrees = getConfig().getBoolean("cleanup.large-trees", true);
            
            requireAxe = getConfig().getBoolean("tools.require-axe", true);
            respectEfficiency = getConfig().getBoolean("tools.respect-efficiency", true);
            
            // Update Checker Settings
            updateCheckerEnabled = getConfig().getBoolean("update-checker.enabled", true);
            updateCheckerResourceId = getConfig().getInt("update-checker.resource-id", 122862);
            updateCheckerNotifyAdmins = getConfig().getBoolean("update-checker.notify-admins", true);
            
            // Tree Type Settings
            enabledTreeTypes = new HashMap<>();
            for (TreeType treeType : TreeType.values()) {
                String configName = treeType.getConfigName();
                boolean defaultValue = configName.equals("jungle") ? false : true;
                enabledTreeTypes.put(configName, getConfig().getBoolean("tree-types." + configName, defaultValue));
            }
            
            getLogger().info("Configuration loaded successfully");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error loading configuration: " + e.getMessage(), e);
            throw e; // Re-throw to be caught by onEnable
        }
    }
    
    /**
     * Reload the plugin configuration
     */
    public void reloadPluginConfig() {
        try {
            reloadConfig();
            loadConfig();
            
            // Re-initialize update checker if needed
            if (updateCheckerEnabled && (updateChecker == null)) {
                initializeUpdateChecker();
            }
            
            getLogger().info("Configuration reloaded!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error reloading configuration: " + e.getMessage(), e);
            throw e; // Re-throw to be caught by command handler
        }
    }

    @Override
    public void onDisable() {
        try {
            // Perform any cleanup if needed
            getLogger().info("TreeMaintainer has been disabled!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error disabling TreeMaintainer: " + e.getMessage(), e);
        }
    }

    /**
     * Log a debug message if debug mode is enabled
     * @param message The message to log
     */
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

    public boolean isCleanupLargeTrees() {
        return cleanupLargeTrees;
    }

    public boolean isRequireAxe() {
        return requireAxe;
    }

    public boolean isRespectEfficiency() {
        return respectEfficiency;
    }

    public boolean isTreeTypeEnabled(String treeType) {
        return enabledTreeTypes.getOrDefault(treeType, false);
    }
    
    public boolean isUpdateCheckerEnabled() {
        return updateCheckerEnabled;
    }
    
    public boolean isUpdateCheckerNotifyAdmins() {
        return updateCheckerNotifyAdmins;
    }
    
    // Service getters
    public TreeDetectionService getTreeDetectionService() {
        return treeDetectionService;
    }
    
    public ReplantingService getReplantingService() {
        return replantingService;
    }
    
    public CleanupService getCleanupService() {
        return cleanupService;
    }
    
    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }
} 