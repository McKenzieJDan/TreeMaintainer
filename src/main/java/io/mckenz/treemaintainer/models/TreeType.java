package io.mckenz.treemaintainer.models;

import org.bukkit.Material;

/**
 * Enum representing different types of trees in Minecraft.
 * Each tree type has associated log and leaf materials, and sapling type.
 */
public enum TreeType {
    OAK("oak", Material.OAK_LOG, Material.OAK_LEAVES, Material.OAK_SAPLING),
    SPRUCE("spruce", Material.SPRUCE_LOG, Material.SPRUCE_LEAVES, Material.SPRUCE_SAPLING),
    BIRCH("birch", Material.BIRCH_LOG, Material.BIRCH_LEAVES, Material.BIRCH_SAPLING),
    JUNGLE("jungle", Material.JUNGLE_LOG, Material.JUNGLE_LEAVES, Material.JUNGLE_SAPLING),
    ACACIA("acacia", Material.ACACIA_LOG, Material.ACACIA_LEAVES, Material.ACACIA_SAPLING),
    DARK_OAK("dark_oak", Material.DARK_OAK_LOG, Material.DARK_OAK_LEAVES, Material.DARK_OAK_SAPLING),
    MANGROVE("mangrove", Material.MANGROVE_LOG, Material.MANGROVE_LEAVES, Material.MANGROVE_PROPAGULE),
    CHERRY("cherry", Material.CHERRY_LOG, Material.CHERRY_LEAVES, Material.CHERRY_SAPLING);

    private final String configName;
    private final Material logMaterial;
    private final Material leavesMaterial;
    private final Material saplingMaterial;

    TreeType(String configName, Material logMaterial, Material leavesMaterial, Material saplingMaterial) {
        this.configName = configName;
        this.logMaterial = logMaterial;
        this.leavesMaterial = leavesMaterial;
        this.saplingMaterial = saplingMaterial;
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