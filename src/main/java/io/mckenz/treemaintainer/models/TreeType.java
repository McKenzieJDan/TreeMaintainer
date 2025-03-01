package io.mckenz.treemaintainer.models;

import org.bukkit.Material;

/**
 * Enum representing different types of trees in Minecraft.
 * Each tree type has associated log and leaf materials, and sapling type.
 */
public enum TreeType {
    OAK("oak", Material.OAK_LOG, Material.OAK_LEAVES, Material.OAK_SAPLING, false),
    SPRUCE("spruce", Material.SPRUCE_LOG, Material.SPRUCE_LEAVES, Material.SPRUCE_SAPLING, true),
    BIRCH("birch", Material.BIRCH_LOG, Material.BIRCH_LEAVES, Material.BIRCH_SAPLING, false),
    JUNGLE("jungle", Material.JUNGLE_LOG, Material.JUNGLE_LEAVES, Material.JUNGLE_SAPLING, true),
    ACACIA("acacia", Material.ACACIA_LOG, Material.ACACIA_LEAVES, Material.ACACIA_SAPLING, false),
    DARK_OAK("dark_oak", Material.DARK_OAK_LOG, Material.DARK_OAK_LEAVES, Material.DARK_OAK_SAPLING, true),
    MANGROVE("mangrove", Material.MANGROVE_LOG, Material.MANGROVE_LEAVES, Material.MANGROVE_PROPAGULE, false),
    CHERRY("cherry", Material.CHERRY_LOG, Material.CHERRY_LEAVES, Material.CHERRY_SAPLING, false);

    private final String configName;
    private final Material logMaterial;
    private final Material leavesMaterial;
    private final Material saplingMaterial;
    private final boolean canGrowAs2x2;

    TreeType(String configName, Material logMaterial, Material leavesMaterial, Material saplingMaterial, boolean canGrowAs2x2) {
        this.configName = configName;
        this.logMaterial = logMaterial;
        this.leavesMaterial = leavesMaterial;
        this.saplingMaterial = saplingMaterial;
        this.canGrowAs2x2 = canGrowAs2x2;
    }

    /**
     * Get the configuration name used in config.yml
     * @return The configuration name
     */
    public String getConfigName() {
        return configName;
    }

    /**
     * Get the log material for this tree type
     * @return The log material
     */
    public Material getLogMaterial() {
        return logMaterial;
    }

    /**
     * Get the leaves material for this tree type
     * @return The leaves material
     */
    public Material getLeavesMaterial() {
        return leavesMaterial;
    }

    /**
     * Get the sapling material for this tree type
     * @return The sapling material
     */
    public Material getSaplingMaterial() {
        return saplingMaterial;
    }
    
    /**
     * Check if this tree type can grow as a 2x2 tree
     * @return True if this tree type can grow as a 2x2 tree (jungle, dark oak, spruce)
     */
    public boolean canGrowAs2x2() {
        return canGrowAs2x2;
    }

    /**
     * Find a TreeType by its log material
     * @param material The log material to search for
     * @return The matching TreeType or null if not found
     */
    public static TreeType fromLogMaterial(Material material) {
        for (TreeType type : values()) {
            if (type.getLogMaterial() == material) {
                return type;
            }
        }
        return null;
    }

    /**
     * Find a TreeType by its config name
     * @param configName The config name to search for
     * @return The matching TreeType or null if not found
     */
    public static TreeType fromConfigName(String configName) {
        for (TreeType type : values()) {
            if (type.getConfigName().equals(configName)) {
                return type;
            }
        }
        return null;
    }
} 