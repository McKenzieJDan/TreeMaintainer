package io.mckenz.treemaintainer.services.impl;

import io.mckenz.treemaintainer.TreeMaintainer;
import io.mckenz.treemaintainer.services.CleanupService;
import io.mckenz.treemaintainer.services.TreeDetectionService;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.logging.Level;

/**
 * Implementation of the CleanupService interface.
 */
public class CleanupServiceImpl implements CleanupService {

    private final TreeMaintainer plugin;
    private final TreeDetectionService treeDetectionService;

    public CleanupServiceImpl(TreeMaintainer plugin, TreeDetectionService treeDetectionService) {
        this.plugin = plugin;
        this.treeDetectionService = treeDetectionService;
    }

    @Override
    public int cleanupFloatingTreeParts(Block startBlock) {
        try {
            if (!plugin.isCleanupEnabled()) {
                return 0;
            }
            
            int logsRemoved = cleanupFloatingLogs(startBlock);
            int leavesRemoved = cleanupFloatingLeaves(startBlock);
            
            return logsRemoved + leavesRemoved;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error cleaning up floating tree parts: " + e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public int cleanupFloatingLogs(Block startBlock) {
        try {
            if (!plugin.isCleanupEnabled()) {
                return 0;
            }
            
            int maxDistance = plugin.getCleanupMaxDistance();
            Set<Block> floatingLogs = treeDetectionService.findFloatingLogs(startBlock, maxDistance);
            
            int count = 0;
            for (Block log : floatingLogs) {
                try {
                    // Use breakNaturally to respect tool enchantments and maintain original behavior
                    log.breakNaturally();
                    count++;
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error cleaning up floating log at " + log.getLocation() + ": " + e.getMessage(), e);
                }
            }
            
            if (count > 0) {
                plugin.debug("Cleaned up " + count + " floating logs");
            }
            
            return count;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error cleaning up floating logs: " + e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public int cleanupFloatingLeaves(Block startBlock) {
        try {
            if (!plugin.isCleanupEnabled()) {
                return 0;
            }
            
            int maxDistance = plugin.getCleanupMaxDistance();
            Set<Block> floatingLeaves = treeDetectionService.findFloatingLeaves(startBlock, maxDistance);
            
            int count = 0;
            for (Block leaf : floatingLeaves) {
                try {
                    // Use breakNaturally to allow leaves to drop their natural items (saplings, sticks, apples)
                    leaf.breakNaturally();
                    count++;
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error cleaning up floating leaf at " + leaf.getLocation() + ": " + e.getMessage(), e);
                }
            }
            
            if (count > 0) {
                plugin.debug("Cleaned up " + count + " floating leaves");
            }
            
            return count;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error cleaning up floating leaves: " + e.getMessage(), e);
            return 0;
        }
    }
} 