package io.mckenz.treemaintainer.services;

import org.bukkit.block.Block;

/**
 * Service interface for tree cleanup operations.
 */
public interface CleanupService {

    /**
     * Clean up floating logs and leaves after a tree has been cut
     * @param startBlock The block where the tree was cut
     * @return The number of blocks cleaned up
     */
    int cleanupFloatingTreeParts(Block startBlock);
    
    /**
     * Clean up floating logs after a tree has been cut
     * @param startBlock The block where the tree was cut
     * @return The number of logs cleaned up
     */
    int cleanupFloatingLogs(Block startBlock);
    
    /**
     * Clean up floating leaves after a tree has been cut
     * @param startBlock The block where the tree was cut
     * @return The number of leaves cleaned up
     */
    int cleanupFloatingLeaves(Block startBlock);
} 