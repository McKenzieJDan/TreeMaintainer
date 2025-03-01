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

            // For large oak trees and 2x2 trees, we need a more thorough search
            boolean isOak = treeType == TreeType.OAK;
            boolean isJungle = treeType == TreeType.JUNGLE;
            boolean is2x2Capable = treeType.canGrowAs2x2();
            
            // Use a larger effective distance for oak trees and 2x2 trees
            // Jungle trees can be extremely tall, so use an even larger distance
            int effectiveMaxDistance = maxDistance;
            if (isOak) {
                effectiveMaxDistance = Math.max(maxDistance * 2, 100);
            } else if (isJungle) {
                effectiveMaxDistance = Math.max(maxDistance * 3, 200); // Jungle trees can be very tall
            } else if (is2x2Capable) {
                effectiveMaxDistance = Math.max(maxDistance * 2, 100);
            }
            
            Set<Block> connectedLogs = new HashSet<>();
            Queue<Block> queue = new LinkedList<>();
            Set<Block> visited = new HashSet<>();
            
            // For 2x2 trees, check if this is part of a 2x2 trunk and add all trunk blocks
            if (is2x2Capable) {
                Set<Block> trunkBlocks = find2x2TrunkBlocks(startBlock, treeType);
                for (Block trunkBlock : trunkBlocks) {
                    queue.add(trunkBlock);
                    visited.add(trunkBlock);
                }
                plugin.debug("Found " + trunkBlocks.size() + " trunk blocks for potential 2x2 " + treeType.getConfigName() + " tree");
            } else {
                queue.add(startBlock);
                visited.add(startBlock);
            }
            
            // For jungle trees, also check a wider area above the starting block
            if (isJungle) {
                // Check up to 30 blocks above for jungle trees
                for (int y = 1; y <= 30; y++) {
                    Block above = startBlock.getRelative(0, y, 0);
                    if (TreeType.fromLogMaterial(above.getType()) == treeType) {
                        if (!visited.contains(above)) {
                            visited.add(above);
                            queue.add(above);
                            plugin.debug("Added jungle log at height +" + y + " to search queue");
                        }
                    } else if (y > 5) {
                        // If we haven't found a log for 5 blocks, stop searching upward
                        boolean foundLog = false;
                        for (int i = 1; i <= 5; i++) {
                            if (TreeType.fromLogMaterial(startBlock.getRelative(0, y-i, 0).getType()) == treeType) {
                                foundLog = true;
                                break;
                            }
                        }
                        if (!foundLog) break;
                    }
                }
            }
            
            while (!queue.isEmpty() && connectedLogs.size() < effectiveMaxDistance) {
                Block current = queue.poll();
                
                if (TreeType.fromLogMaterial(current.getType()) == treeType) {
                    connectedLogs.add(current);
                    
                    // Check all adjacent blocks
                    for (BlockFace face : ALL_FACES) {
                        Block adjacent = current.getRelative(face);
                        if (!visited.contains(adjacent)) {
                            visited.add(adjacent);
                            if (TreeType.fromLogMaterial(adjacent.getType()) == treeType) {
                                queue.add(adjacent);
                            }
                        }
                    }
                    
                    // For oak trees, also check diagonal up blocks (for branches)
                    if (isOak) {
                        for (BlockFace horizontalFace : HORIZONTAL_FACES) {
                            Block diagonalUp = current.getRelative(horizontalFace).getRelative(BlockFace.UP);
                            if (!visited.contains(diagonalUp)) {
                                visited.add(diagonalUp);
                                if (TreeType.fromLogMaterial(diagonalUp.getType()) == treeType) {
                                    queue.add(diagonalUp);
                                }
                            }
                        }
                    }
                    
                    // For 2x2 capable trees, check diagonal blocks in all directions
                    if (is2x2Capable) {
                        for (BlockFace horizontalFace : HORIZONTAL_FACES) {
                            // Check diagonal blocks at same level
                            Block diagonal = current.getRelative(horizontalFace);
                            if (!visited.contains(diagonal)) {
                                visited.add(diagonal);
                                if (TreeType.fromLogMaterial(diagonal.getType()) == treeType) {
                                    queue.add(diagonal);
                                }
                            }
                            
                            // Check diagonal blocks above
                            Block diagonalUp = current.getRelative(horizontalFace).getRelative(BlockFace.UP);
                            if (!visited.contains(diagonalUp)) {
                                visited.add(diagonalUp);
                                if (TreeType.fromLogMaterial(diagonalUp.getType()) == treeType) {
                                    queue.add(diagonalUp);
                                }
                            }
                            
                            // Check diagonal blocks below
                            Block diagonalDown = current.getRelative(horizontalFace).getRelative(BlockFace.DOWN);
                            if (!visited.contains(diagonalDown)) {
                                visited.add(diagonalDown);
                                if (TreeType.fromLogMaterial(diagonalDown.getType()) == treeType) {
                                    queue.add(diagonalDown);
                                }
                            }
                        }
                        
                        // For jungle trees, check even further in all directions
                        if (isJungle) {
                            // Check two blocks out in each direction
                            for (BlockFace face : ALL_FACES) {
                                Block twoAway = current.getRelative(face).getRelative(face);
                                if (!visited.contains(twoAway)) {
                                    visited.add(twoAway);
                                    if (TreeType.fromLogMaterial(twoAway.getType()) == treeType) {
                                        queue.add(twoAway);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // If we hit the max distance and this is a jungle tree, log a warning
            if (connectedLogs.size() >= effectiveMaxDistance && isJungle) {
                plugin.getLogger().warning("Hit maximum search distance for jungle tree. Some logs may not be detected. Consider increasing cleanup_max_distance in config.");
            }
            
            plugin.debug("Found " + connectedLogs.size() + " connected logs for " + treeType.getConfigName() + " tree");
            return connectedLogs;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error finding connected logs: " + e.getMessage(), e);
            return Collections.emptySet();
        }
    }

    /**
     * Find all blocks that are part of a 2x2 tree trunk
     * @param startBlock One of the trunk blocks
     * @param treeType The tree type
     * @return Set of blocks that form the 2x2 trunk
     */
    private Set<Block> find2x2TrunkBlocks(Block startBlock, TreeType treeType) {
        Set<Block> trunkBlocks = new HashSet<>();
        trunkBlocks.add(startBlock);
        
        // For dark oak and jungle, we need to be more thorough in our search
        boolean isDarkOak = treeType == TreeType.DARK_OAK;
        boolean isJungle = treeType == TreeType.JUNGLE;
        boolean needsThoroughSearch = isDarkOak || isJungle;
        
        // Check all horizontal adjacent blocks
        for (BlockFace face : HORIZONTAL_FACES) {
            Block adjacent = startBlock.getRelative(face);
            if (TreeType.fromLogMaterial(adjacent.getType()) == treeType) {
                trunkBlocks.add(adjacent);
                
                // If we found a diagonal block, we need to check the other two blocks to form a 2x2 square
                if (face == BlockFace.NORTH_EAST || face == BlockFace.SOUTH_EAST || 
                    face == BlockFace.SOUTH_WEST || face == BlockFace.NORTH_WEST) {
                    
                    // Determine the two cardinal directions from the diagonal
                    BlockFace[] cardinalFaces = getCardinalFacesFromDiagonal(face);
                    
                    // Check both cardinal blocks
                    for (BlockFace cardinalFace : cardinalFaces) {
                        Block cardinalBlock = startBlock.getRelative(cardinalFace);
                        if (TreeType.fromLogMaterial(cardinalBlock.getType()) == treeType) {
                            trunkBlocks.add(cardinalBlock);
                            
                            // Check the fourth block that would complete the 2x2 square
                            Block fourthBlock = cardinalBlock.getRelative(face);
                            if (TreeType.fromLogMaterial(fourthBlock.getType()) == treeType) {
                                trunkBlocks.add(fourthBlock);
                                
                                // For dark oak and jungle, also check blocks above and below to ensure we get the full trunk
                                if (needsThoroughSearch) {
                                    // Check one block above and below each trunk block
                                    for (Block trunkBlock : new HashSet<>(trunkBlocks)) {
                                        Block above = trunkBlock.getRelative(BlockFace.UP);
                                        Block below = trunkBlock.getRelative(BlockFace.DOWN);
                                        
                                        if (TreeType.fromLogMaterial(above.getType()) == treeType) {
                                            trunkBlocks.add(above);
                                        }
                                        
                                        if (TreeType.fromLogMaterial(below.getType()) == treeType) {
                                            trunkBlocks.add(below);
                                        }
                                    }
                                }
                                
                                return trunkBlocks; // We found a complete 2x2 trunk
                            }
                        }
                    }
                }
            }
        }
        
        // If we didn't find a complete 2x2 trunk but this is dark oak or jungle,
        // check cardinal directions more thoroughly
        if (needsThoroughSearch && trunkBlocks.size() < 4) {
            // Try to find a 2x2 trunk by checking cardinal directions
            if (hasCardinal2x2Trunk(startBlock, treeType, trunkBlocks)) {
                return trunkBlocks;
            }
        }
        
        return trunkBlocks;
    }
    
    /**
     * Check if there's a 2x2 trunk by examining cardinal directions
     * @param startBlock The starting block
     * @param treeType The tree type
     * @param trunkBlocks Set to add trunk blocks to
     * @return True if a 2x2 trunk was found, false otherwise
     */
    private boolean hasCardinal2x2Trunk(Block startBlock, TreeType treeType, Set<Block> trunkBlocks) {
        // Check if we can form a 2x2 trunk with this block as any of the 4 corners
        // Format: {x1, y1, z1, x2, y2, z2, x3, y3, z3} for the three other blocks
        int[][] offsets = {
            {1, 0, 0, 0, 0, 1, 1, 0, 1},  // This block as NW corner
            {-1, 0, 0, 0, 0, 1, -1, 0, 1}, // This block as NE corner
            {1, 0, 0, 0, 0, -1, 1, 0, -1}, // This block as SW corner
            {-1, 0, 0, 0, 0, -1, -1, 0, -1} // This block as SE corner
        };
        
        for (int[] offset : offsets) {
            Block b1 = startBlock.getRelative(offset[0], offset[1], offset[2]);
            Block b2 = startBlock.getRelative(offset[3], offset[4], offset[5]);
            Block b3 = startBlock.getRelative(offset[6], offset[7], offset[8]);
            
            if (TreeType.fromLogMaterial(b1.getType()) == treeType &&
                TreeType.fromLogMaterial(b2.getType()) == treeType &&
                TreeType.fromLogMaterial(b3.getType()) == treeType) {
                
                trunkBlocks.add(b1);
                trunkBlocks.add(b2);
                trunkBlocks.add(b3);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get the two cardinal BlockFaces that make up a diagonal BlockFace
     * @param diagonalFace The diagonal BlockFace
     * @return Array of the two cardinal BlockFaces
     */
    private BlockFace[] getCardinalFacesFromDiagonal(BlockFace diagonalFace) {
        switch (diagonalFace) {
            case NORTH_EAST:
                return new BlockFace[]{BlockFace.NORTH, BlockFace.EAST};
            case SOUTH_EAST:
                return new BlockFace[]{BlockFace.SOUTH, BlockFace.EAST};
            case SOUTH_WEST:
                return new BlockFace[]{BlockFace.SOUTH, BlockFace.WEST};
            case NORTH_WEST:
                return new BlockFace[]{BlockFace.NORTH, BlockFace.WEST};
            default:
                return new BlockFace[]{BlockFace.NORTH, BlockFace.EAST}; // Default fallback
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