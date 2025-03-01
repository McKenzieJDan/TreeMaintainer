package io.mckenz.treemaintainer.services.impl;

import io.mckenz.treemaintainer.TreeMaintainer;
import io.mckenz.treemaintainer.models.TreeType;
import io.mckenz.treemaintainer.services.ReplantingService;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import java.util.logging.Level;

/**
 * Implementation of the ReplantingService interface.
 */
public class ReplantingServiceImpl implements ReplantingService {

    private final TreeMaintainer plugin;

    public ReplantingServiceImpl(TreeMaintainer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean canPlantSapling(Location location, Material logType) {
        try {
            TreeType treeType = TreeType.fromLogMaterial(logType);
            if (treeType == null) {
                return false;
            }

            Block block = location.getBlock();
            
            // Check if the block is air (can be replaced)
            if (block.getType() != Material.AIR) {
                plugin.debug("Cannot plant sapling: Block is not air");
                return false;
            }
            
            // Check if the block below is a valid planting surface
            Block below = block.getRelative(BlockFace.DOWN);
            Material belowType = below.getType();
            
            boolean validSurface = belowType == Material.DIRT || 
                                   belowType == Material.GRASS_BLOCK || 
                                   belowType == Material.PODZOL || 
                                   belowType == Material.COARSE_DIRT || 
                                   belowType == Material.ROOTED_DIRT || 
                                   belowType == Material.MOSS_BLOCK || 
                                   belowType == Material.MUD;
            
            if (!validSurface) {
                plugin.debug("Cannot plant sapling: Invalid surface below - " + belowType);
                return false;
            }
            
            // Special case for mangrove propagules which can be planted in water
            if (treeType == TreeType.MANGROVE && belowType == Material.WATER) {
                return true;
            }
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking if sapling can be planted: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean plantSapling(Location location, Material logType) {
        try {
            TreeType treeType = TreeType.fromLogMaterial(logType);
            if (treeType == null) {
                plugin.debug("Cannot plant sapling: Unknown tree type for " + logType);
                return false;
            }
            
            if (!canPlantSapling(location, logType)) {
                return false;
            }
            
            // Check for existing dropped saplings in the area
            Item foundSapling = findDroppedSapling(location, treeType.getSaplingMaterial());
            Block block = location.getBlock();
            
            if (foundSapling != null) {
                // Use the found sapling instead of creating a new one
                plugin.debug("Using dropped sapling for replanting: " + treeType.getSaplingMaterial());
                ItemStack stack = foundSapling.getItemStack();
                if (stack.getAmount() > 1) {
                    stack.setAmount(stack.getAmount() - 1);
                    foundSapling.setItemStack(stack);
                } else {
                    foundSapling.remove();
                }
                block.setType(treeType.getSaplingMaterial());
            } else {
                // No sapling found, plant a new one
                plugin.debug("No sapling found, planting new " + treeType.getSaplingMaterial());
                block.setType(treeType.getSaplingMaterial());
            }
            
            plugin.debug("Planted " + treeType.getSaplingMaterial() + " at " + location);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error planting sapling: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Find a dropped sapling in the area
     * @param location The location to check
     * @param saplingType The type of sapling to find
     * @return The found sapling item, or null if none found
     */
    private Item findDroppedSapling(Location location, Material saplingType) {
        try {
            // Look for dropped sapling items within 1 block of the planting location
            for (org.bukkit.entity.Entity entity : location.getWorld().getNearbyEntities(location, 1, 1, 1)) {
                if (entity instanceof Item item) {
                    if (item.getItemStack().getType() == saplingType) {
                        plugin.debug("Found dropped sapling item: " + saplingType);
                        return item;
                    }
                }
            }
            return null;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error finding dropped saplings: " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void scheduleReplanting(Location location, Material logType, int delay) {
        try {
            if (!plugin.isReplantingEnabled()) {
                return;
            }
            
            plugin.debug("Scheduling replanting of " + logType + " at " + location + " with delay " + delay);
            
            // Schedule the replanting task
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                try {
                    plantSapling(location, logType);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in scheduled replanting task: " + e.getMessage(), e);
                }
            }, delay);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error scheduling replanting: " + e.getMessage(), e);
        }
    }
} 