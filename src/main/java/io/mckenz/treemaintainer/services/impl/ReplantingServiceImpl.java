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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
            
            // For 2x2 trees, we need to check if this was part of a 2x2 trunk
            if (treeType.canGrowAs2x2()) {
                return plant2x2Saplings(location, treeType);
            }
            
            // Regular single sapling planting
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
     * Plant saplings in a 2x2 pattern for large trees
     * @param location The location of one of the trunk blocks
     * @param treeType The tree type
     * @return True if at least one sapling was planted, false otherwise
     */
    private boolean plant2x2Saplings(Location location, TreeType treeType) {
        try {
            plugin.debug("Attempting to plant 2x2 saplings for " + treeType.getConfigName() + " at " + location);
            
            // Store the original location for reference
            Location originalLocation = location.clone();
            
            // For dark oak and jungle trees, we need to be extremely precise about placement
            boolean isDarkOak = treeType == TreeType.DARK_OAK;
            boolean isJungle = treeType == TreeType.JUNGLE;
            boolean requiresExactPlacement = isDarkOak || isJungle;
            
            // First, try to find the exact 2x2 grid by checking for remaining logs
            Location exactCorner = findExact2x2Corner(location, treeType);
            if (exactCorner != null) {
                plugin.debug("Found exact 2x2 corner at " + exactCorner);
                location = exactCorner;
            } else {
                // If we couldn't find the exact corner, try to round to the nearest block grid
                location = roundToNearestGrid(location);
                plugin.debug("Rounded to nearest grid at " + location);
            }
            
            // Try to plant saplings in a 2x2 pattern
            boolean planted = false;
            int saplingCount = 0;
            
            // Collect all dropped saplings in the area
            List<Item> droppedSaplings = findAllDroppedSaplings(location, treeType.getSaplingMaterial(), 3);
            int availableSaplings = droppedSaplings.size();
            
            plugin.debug("Found " + availableSaplings + " dropped saplings for 2x2 planting");
            
            // Define the 2x2 grid positions
            int[][] positions = {{0, 0}, {0, 1}, {1, 0}, {1, 1}};
            
            // Check if we can plant all 4 saplings (required for dark oak and jungle)
            boolean canPlantAll = true;
            for (int[] pos : positions) {
                Location saplingLoc = location.clone().add(pos[0], 0, pos[1]);
                if (!canPlantSapling(saplingLoc, treeType.getLogMaterial())) {
                    canPlantAll = false;
                    break;
                }
            }
            
            // If we can't plant all 4 saplings, try alternative positions
            if (!canPlantAll) {
                plugin.debug("Cannot plant all 4 saplings at " + location + ", trying alternative positions");
                
                // Try all possible 2x2 grid alignments
                Location bestCorner = findBestPlantingCorner(originalLocation, treeType);
                if (bestCorner != null) {
                    location = bestCorner;
                    plugin.debug("Found better corner at " + location);
                    
                    // Recheck with new corner
                    canPlantAll = true;
                    for (int[] pos : positions) {
                        Location saplingLoc = location.clone().add(pos[0], 0, pos[1]);
                        if (!canPlantSapling(saplingLoc, treeType.getLogMaterial())) {
                            canPlantAll = false;
                            break;
                        }
                    }
                }
                
                // If we still can't plant all 4 and this is dark oak or jungle, we can't proceed with 2x2 planting
                if (!canPlantAll && requiresExactPlacement) {
                    plugin.debug("Cannot find a valid 2x2 grid for " + treeType.getConfigName() + " saplings, falling back to single sapling");
                    return canPlantSapling(originalLocation, treeType.getLogMaterial()) && 
                           plantSingleSapling(originalLocation, treeType);
                }
            }
            
            plugin.debug("Final planting location: " + location);
            
            // Plant the saplings
            for (int[] pos : positions) {
                Location saplingLoc = location.clone().add(pos[0], 0, pos[1]);
                if (canPlantSapling(saplingLoc, treeType.getLogMaterial())) {
                    // Try to use a dropped sapling first
                    if (!droppedSaplings.isEmpty()) {
                        Item sapling = droppedSaplings.remove(0);
                        ItemStack stack = sapling.getItemStack();
                        if (stack.getAmount() > 1) {
                            stack.setAmount(stack.getAmount() - 1);
                            sapling.setItemStack(stack);
                        } else {
                            sapling.remove();
                        }
                        saplingLoc.getBlock().setType(treeType.getSaplingMaterial());
                        saplingCount++;
                        planted = true;
                    } else {
                        // Create a new sapling
                        saplingLoc.getBlock().setType(treeType.getSaplingMaterial());
                        saplingCount++;
                        planted = true;
                    }
                }
            }
            
            plugin.debug("Planted " + saplingCount + " saplings in 2x2 pattern for " + treeType.getConfigName());
            return planted;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error planting 2x2 saplings: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Find the exact corner of a 2x2 grid by checking for remaining logs
     * @param location The starting location
     * @param treeType The tree type
     * @return The exact corner location, or null if not found
     */
    private Location findExact2x2Corner(Location location, TreeType treeType) {
        try {
            Material logMaterial = treeType.getLogMaterial();
            Block startBlock = location.getBlock();
            
            // Check for logs in a 3x3 area around the starting location
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    Block checkBlock = startBlock.getRelative(x, 0, z);
                    
                    // If this block is a log of the right type, check if it's part of a 2x2 pattern
                    if (checkBlock.getType() == logMaterial) {
                        // Check if this could be the northwest corner of a 2x2
                        Block east = checkBlock.getRelative(1, 0, 0);
                        Block south = checkBlock.getRelative(0, 0, 1);
                        Block southeast = checkBlock.getRelative(1, 0, 1);
                        
                        boolean hasEast = east.getType() == logMaterial;
                        boolean hasSouth = south.getType() == logMaterial;
                        boolean hasSoutheast = southeast.getType() == logMaterial;
                        
                        // If we have at least 2 of the 3 possible logs, this is likely our corner
                        if ((hasEast && hasSouth) || (hasEast && hasSoutheast) || (hasSouth && hasSoutheast)) {
                            return checkBlock.getLocation().clone();
                        }
                    }
                }
            }
            
            // If we couldn't find a clear 2x2 pattern, check for any remaining logs
            // and try to infer the grid from them
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    Block checkBlock = startBlock.getRelative(x, 0, z);
                    
                    if (checkBlock.getType() == logMaterial) {
                        // Check blocks above and below to confirm it's part of the trunk
                        Block above = checkBlock.getRelative(0, 1, 0);
                        Block below = checkBlock.getRelative(0, -1, 0);
                        
                        boolean hasLogAbove = above.getType() == logMaterial;
                        boolean hasLogBelow = below.getType() == logMaterial;
                        
                        if (hasLogAbove || hasLogBelow) {
                            // This is likely part of the trunk, calculate the northwest corner
                            int cornerX = (int) Math.floor(checkBlock.getX() / 2.0) * 2;
                            int cornerZ = (int) Math.floor(checkBlock.getZ() / 2.0) * 2;
                            
                            Location cornerLoc = checkBlock.getLocation().clone();
                            cornerLoc.setX(cornerX);
                            cornerLoc.setZ(cornerZ);
                            
                            return cornerLoc;
                        }
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error finding exact 2x2 corner: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Round a location to the nearest 2x2 grid
     * @param location The location to round
     * @return The rounded location
     */
    private Location roundToNearestGrid(Location location) {
        try {
            // Round to the nearest even coordinates for X and Z
            int x = (int) Math.floor(location.getBlockX() / 2.0) * 2;
            int z = (int) Math.floor(location.getBlockZ() / 2.0) * 2;
            
            Location rounded = location.clone();
            rounded.setX(x);
            rounded.setZ(z);
            
            return rounded;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error rounding to nearest grid: " + e.getMessage(), e);
            return location;
        }
    }
    
    /**
     * Find the best corner for planting a 2x2 pattern
     * @param location The starting location
     * @param treeType The tree type
     * @return The best corner location, or null if none found
     */
    private Location findBestPlantingCorner(Location location, TreeType treeType) {
        try {
            // Try all possible 2x2 grid alignments within a 2-block radius
            for (int xOffset = -2; xOffset <= 0; xOffset++) {
                for (int zOffset = -2; zOffset <= 0; zOffset++) {
                    Location cornerCandidate = location.clone().add(xOffset, 0, zOffset);
                    
                    // Check if we can plant in a 2x2 pattern at this corner
                    boolean canPlantHere = true;
                    for (int x = 0; x < 2; x++) {
                        for (int z = 0; z < 2; z++) {
                            Location saplingLoc = cornerCandidate.clone().add(x, 0, z);
                            if (!canPlantSapling(saplingLoc, treeType.getLogMaterial())) {
                                canPlantHere = false;
                                break;
                            }
                        }
                        if (!canPlantHere) break;
                    }
                    
                    if (canPlantHere) {
                        return cornerCandidate;
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error finding best planting corner: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Find all dropped saplings in a larger area
     * @param location The center location
     * @param saplingType The sapling material type
     * @param radius The search radius
     * @return List of found sapling items
     */
    private List<Item> findAllDroppedSaplings(Location location, Material saplingType, double radius) {
        List<Item> saplings = new ArrayList<>();
        try {
            // Look for dropped sapling items within the specified radius
            for (org.bukkit.entity.Entity entity : location.getWorld().getNearbyEntities(location, radius, radius, radius)) {
                if (entity instanceof Item item) {
                    if (item.getItemStack().getType() == saplingType) {
                        saplings.add(item);
                    }
                }
            }
            return saplings;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error finding all dropped saplings: " + e.getMessage(), e);
            return saplings;
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

    /**
     * Find the original locations of the 2x2 trunk blocks
     * @param location One of the trunk block locations
     * @param treeType The tree type
     * @return Set of the original trunk block locations
     */
    private Set<Location> findOriginalTrunkLocations(Location location, TreeType treeType) {
        Set<Location> trunkLocations = new HashSet<>();
        try {
            // Add the current location
            trunkLocations.add(location.clone());
            
            // Check all horizontal adjacent blocks for signs of where the trunk was
            for (BlockFace face : new BlockFace[]{
                    BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST,
                    BlockFace.NORTH_EAST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST, BlockFace.NORTH_WEST}) {
                
                // Check if there's a log block or air (where a log might have been)
                Block adjacent = location.getBlock().getRelative(face);
                if (adjacent.getType() == Material.AIR || TreeType.fromLogMaterial(adjacent.getType()) == treeType) {
                    // Check if there are logs above or below this position
                    Block above = adjacent.getRelative(BlockFace.UP);
                    Block below = adjacent.getRelative(BlockFace.DOWN);
                    
                    boolean hasLogAbove = TreeType.fromLogMaterial(above.getType()) == treeType;
                    boolean hasLogBelow = TreeType.fromLogMaterial(below.getType()) == treeType;
                    
                    // If there's a log above or below, this was likely part of the trunk
                    if (hasLogAbove || hasLogBelow) {
                        trunkLocations.add(adjacent.getLocation().clone());
                    }
                }
            }
            
            return trunkLocations;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error finding original trunk locations: " + e.getMessage(), e);
            return trunkLocations;
        }
    }
    
    /**
     * Find the best corner location from a set of trunk locations
     * @param trunkLocations The set of trunk locations
     * @return The best corner location
     */
    private Location findBestCornerFromTrunkLocations(Set<Location> trunkLocations) {
        try {
            if (trunkLocations.isEmpty()) {
                return null;
            }
            
            // Find the minimum X and Z coordinates
            double minX = Double.MAX_VALUE;
            double minZ = Double.MAX_VALUE;
            
            for (Location loc : trunkLocations) {
                minX = Math.min(minX, loc.getX());
                minZ = Math.min(minZ, loc.getZ());
            }
            
            // Create a location at the minimum X and Z (northwest corner)
            Location cornerLoc = trunkLocations.iterator().next().clone();
            cornerLoc.setX(minX);
            cornerLoc.setZ(minZ);
            
            return cornerLoc;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error finding best corner from trunk locations: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Plant a single sapling at the specified location
     * @param location The location to plant at
     * @param treeType The tree type
     * @return True if planted successfully, false otherwise
     */
    private boolean plantSingleSapling(Location location, TreeType treeType) {
        try {
            Block block = location.getBlock();
            
            // Check for existing dropped saplings in the area
            Item foundSapling = findDroppedSapling(location, treeType.getSaplingMaterial());
            
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
            
            plugin.debug("Planted single " + treeType.getSaplingMaterial() + " at " + location);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error planting single sapling: " + e.getMessage(), e);
            return false;
        }
    }
} 