package io.mckenz.treemaintainer.services.impl;

import io.mckenz.treemaintainer.TreeMaintainer;
import io.mckenz.treemaintainer.models.TreeType;
import io.mckenz.treemaintainer.services.TreeDetectionService;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.*;
import java.util.logging.Level;

/**
 * Implementation of the TreeDetectionService interface.
 */
public class TreeDetectionServiceImpl implements TreeDetectionService {

    private final TreeMaintainer plugin;
    private static final BlockFace[] HORIZONTAL_FACES = {
            BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST,
            BlockFace.NORTH_EAST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST, BlockFace.NORTH_WEST
    };
    private static final BlockFace[] ALL_FACES = {
            BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST,
            BlockFace.UP, BlockFace.DOWN,
            BlockFace.NORTH_EAST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST, BlockFace.NORTH_WEST
    };

    public TreeDetectionServiceImpl(TreeMaintainer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isTree(Block block) {
        try {
            // Check if the block is a log
            TreeType treeType = TreeType.fromLogMaterial(block.getType());
            if (treeType == null) {
                return false;
            }

            // Check if this tree type is enabled in the config
            if (!plugin.isTreeTypeEnabled(treeType.getConfigName())) {
                return false;
            }

            // Check if the log is connected to the ground
            return isConnectedToGround(block);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking if block is a tree: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if a log block is connected to the ground
     * @param block The log block to check
     * @return True if connected to the ground, false otherwise
     */
    private boolean isConnectedToGround(Block block) {
        try {
            // Check if the block below is dirt, grass, or other valid ground block
            Block below = block.getRelative(BlockFace.DOWN);
            Material belowType = below.getType();
            
            return belowType == Material.DIRT || 
                   belowType == Material.GRASS_BLOCK || 
                   belowType == Material.PODZOL || 
                   belowType == Material.COARSE_DIRT || 
                   belowType == Material.ROOTED_DIRT || 
                   belowType == Material.MOSS_BLOCK || 
                   belowType == Material.MUD;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking if block is connected to ground: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Set<Block> findConnectedLogs(Block startBlock, int maxDistance) {
        try {
            TreeType treeType = TreeType.fromLogMaterial(startBlock.getType());
            if (treeType == null) {
                return Collections.emptySet();
            }

            Set<Block> connectedLogs = new HashSet<>();
            Queue<Block> queue = new LinkedList<>();
            Set<Block> visited = new HashSet<>();
            
            queue.add(startBlock);
            visited.add(startBlock);
            
            while (!queue.isEmpty() && connectedLogs.size() < maxDistance) {
                Block current = queue.poll();
                
                if (current.getType() == treeType.getLogMaterial()) {
                    connectedLogs.add(current);
                    
                    // Check all adjacent blocks
                    for (BlockFace face : ALL_FACES) {
                        Block adjacent = current.getRelative(face);
                        if (!visited.contains(adjacent)) {
                            visited.add(adjacent);
                            if (adjacent.getType() == treeType.getLogMaterial()) {
                                queue.add(adjacent);
                            }
                        }
                    }
                }
            }
            
            return connectedLogs;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error finding connected logs: " + e.getMessage(), e);
            return Collections.emptySet();
        }
    }

    @Override
    public Set<Block> findFloatingLogs(Block startBlock, int maxDistance) {
        try {
            TreeType treeType = TreeType.fromLogMaterial(startBlock.getType());
            if (treeType == null) {
                return Collections.emptySet();
            }

            // Find all connected logs
            Set<Block> allLogs = findConnectedLogs(startBlock, maxDistance);
            Set<Block> floatingLogs = new HashSet<>();
            
            // Check each log to see if it's floating (not connected to the ground)
            for (Block log : allLogs) {
                if (!isConnectedToGround(log) && !isConnectedToGroundedLog(log, allLogs)) {
                    floatingLogs.add(log);
                }
            }
            
            return floatingLogs;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error finding floating logs: " + e.getMessage(), e);
            return Collections.emptySet();
        }
    }

    /**
     * Check if a log is connected to a grounded log
     * @param log The log to check
     * @param allLogs All logs in the tree
     * @return True if connected to a grounded log, false otherwise
     */
    private boolean isConnectedToGroundedLog(Block log, Set<Block> allLogs) {
        try {
            Set<Block> visited = new HashSet<>();
            Queue<Block> queue = new LinkedList<>();
            
            queue.add(log);
            visited.add(log);
            
            while (!queue.isEmpty()) {
                Block current = queue.poll();
                
                // Check if this log is connected to the ground
                if (isConnectedToGround(current)) {
                    return true;
                }
                
                // Check adjacent blocks
                for (BlockFace face : ALL_FACES) {
                    Block adjacent = current.getRelative(face);
                    if (allLogs.contains(adjacent) && !visited.contains(adjacent)) {
                        visited.add(adjacent);
                        queue.add(adjacent);
                    }
                }
            }
            
            return false;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking if log is connected to grounded log: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Set<Block> findFloatingLeaves(Block startBlock, int maxDistance) {
        try {
            TreeType treeType = TreeType.fromLogMaterial(startBlock.getType());
            if (treeType == null) {
                return Collections.emptySet();
            }

            // Find all connected logs
            Set<Block> allLogs = findConnectedLogs(startBlock, maxDistance);
            Set<Block> floatingLeaves = new HashSet<>();
            Set<Block> visited = new HashSet<>();
            
            // Check around each log for leaves
            for (Block log : allLogs) {
                for (BlockFace face : ALL_FACES) {
                    Block adjacent = log.getRelative(face);
                    if (adjacent.getType() == treeType.getLeavesMaterial() && !visited.contains(adjacent)) {
                        // If the leaf is not connected to a non-floating log, it's floating
                        if (!isConnectedToNonFloatingLog(adjacent, allLogs)) {
                            floatingLeaves.add(adjacent);
                        }
                        visited.add(adjacent);
                    }
                }
            }
            
            return floatingLeaves;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error finding floating leaves: " + e.getMessage(), e);
            return Collections.emptySet();
        }
    }

    /**
     * Check if a leaf block is connected to a non-floating log
     * @param leaf The leaf block to check
     * @param allLogs All logs in the tree
     * @return True if connected to a non-floating log, false otherwise
     */
    private boolean isConnectedToNonFloatingLog(Block leaf, Set<Block> allLogs) {
        try {
            for (BlockFace face : ALL_FACES) {
                Block adjacent = leaf.getRelative(face);
                if (allLogs.contains(adjacent) && isConnectedToGround(adjacent)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking if leaf is connected to non-floating log: " + e.getMessage(), e);
            return false;
        }
    }
} 