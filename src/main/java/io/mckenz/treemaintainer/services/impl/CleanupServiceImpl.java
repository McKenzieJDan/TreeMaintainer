package io.mckenz.treemaintainer.services.impl;

import io.mckenz.treemaintainer.TreeMaintainer;
import io.mckenz.treemaintainer.models.TreeType;
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
            
            // First pass: clean up floating logs
            int logsRemoved = cleanupFloatingLogs(startBlock);
            
            // Second pass: clean up floating leaves
            int leavesRemoved = cleanupFloatingLeaves(startBlock);
            
            // For oak trees, do an additional pass to catch any missed logs
            TreeType treeType = TreeType.fromLogMaterial(startBlock.getType());
            if (treeType == TreeType.OAK && plugin.isCleanupLargeTrees()) {
                // Wait a tick to let the first pass complete
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    int additionalLogs = cleanupFloatingLogs(startBlock);
                    if (additionalLogs > 0) {
                        plugin.debug("Second pass removed " + additionalLogs + " additional floating oak logs");
                    }
                }, 1L);
            }
            
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