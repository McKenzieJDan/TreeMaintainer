package io.mckenz.treemaintainer.services;

import org.bukkit.block.Block;

import java.util.Set;

/**
 * Service interface for tree detection operations.
 */
public interface TreeDetectionService {

    /**
     * Check if a block is part of a tree
     * @param block The block to check
     * @return True if the block is part of a tree, false otherwise
     */
    boolean isTree(Block block);
    
    /**
     * Find all connected log blocks that are part of the same tree
     * @param startBlock The starting log block
     * @param maxDistance The maximum distance to search
     * @return A set of connected log blocks
     */
    Set<Block> findConnectedLogs(Block startBlock, int maxDistance);
    
    /**
     * Find all floating log blocks after a tree has been cut
     * @param startBlock The block where the tree was cut
     * @param maxDistance The maximum distance to search
     * @return A set of floating log blocks
     */
    Set<Block> findFloatingLogs(Block startBlock, int maxDistance);
    
    /**
     * Find all floating leaf blocks after a tree has been cut
     * @param startBlock The block where the tree was cut
     * @param maxDistance The maximum distance to search
     * @return A set of floating leaf blocks
     */
    Set<Block> findFloatingLeaves(Block startBlock, int maxDistance);
} 