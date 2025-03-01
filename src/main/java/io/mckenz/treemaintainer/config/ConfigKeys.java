package io.mckenz.treemaintainer.config;

/**
 * Constants for configuration keys used in the plugin.
 */
public class ConfigKeys {
    // General settings
    public static final String ENABLED = "enabled";
    public static final String DEBUG = "debug";
    
    // Replanting settings
    public static final String REPLANTING_ENABLED = "replanting.enabled";
    public static final String REPLANTING_DELAY = "replanting.delay";
    
    // Cleanup settings
    public static final String CLEANUP_ENABLED = "cleanup.enabled";
    public static final String CLEANUP_MAX_DISTANCE = "cleanup.max-distance";
    public static final String CLEANUP_LARGE_TREES = "cleanup.large-trees";
    
    // Tool settings
    public static final String TOOLS_REQUIRE_AXE = "tools.require-axe";
    public static final String TOOLS_RESPECT_EFFICIENCY = "tools.respect-efficiency";
    
    // Tree type settings
    public static final String TREE_TYPES_PREFIX = "tree-types.";
    
    // Update checker settings
    public static final String UPDATE_CHECKER_ENABLED = "update-checker.enabled";
    public static final String UPDATE_CHECKER_RESOURCE_ID = "update-checker.resource-id";
    public static final String UPDATE_CHECKER_NOTIFY_ADMINS = "update-checker.notify-admins";
}