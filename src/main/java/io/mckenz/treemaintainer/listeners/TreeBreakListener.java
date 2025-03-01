package io.mckenz.treemaintainer.listeners;

import io.mckenz.treemaintainer.TreeMaintainer;
import io.mckenz.treemaintainer.models.TreeType;
import io.mckenz.treemaintainer.services.CleanupService;
import io.mckenz.treemaintainer.services.ReplantingService;
import io.mckenz.treemaintainer.services.TreeDetectionService;
import io.mckenz.treemaintainer.utils.ToolUtils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.logging.Level;

/**
 * Listener for tree breaking events.
 */
public class TreeBreakListener implements Listener {

    private final TreeMaintainer plugin;
    private final TreeDetectionService treeDetectionService;
    private final ReplantingService replantingService;
    private final CleanupService cleanupService;

    public TreeBreakListener(
            TreeMaintainer plugin,
            TreeDetectionService treeDetectionService,
            ReplantingService replantingService,
            CleanupService cleanupService) {
        this.plugin = plugin;
        this.treeDetectionService = treeDetectionService;
        this.replantingService = replantingService;
        this.cleanupService = cleanupService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        try {
            // Check if plugin is enabled
            if (!plugin.isPluginEnabled()) {
                return;
            }
            
            Block block = event.getBlock();
            
            // Check if the broken block is a log
            TreeType treeType = TreeType.fromLogMaterial(block.getType());
            if (treeType == null) {
                return;
            }

            // Check if this tree type is enabled
            if (!plugin.isTreeTypeEnabled(treeType.getConfigName())) {
                plugin.debug(treeType.getConfigName() + " tree type is disabled - skipping processing");
                return;
            }
            
            // Check if player is using an axe
            ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
            if (plugin.isRequireAxe() && !ToolUtils.isAxe(tool.getType())) {
                plugin.debug("Not using an axe - skipping tree processing");
                return;
            }

            // Store the log type before it's broken
            Material logType = block.getType();
            plugin.debug("Log block broken with " + tool.getType() + " at " + block.getLocation());

            // Check if the block is part of a tree
            if (!treeDetectionService.isTree(block)) {
                plugin.debug("Block is not part of a tree - skipping processing");
                return;
            }

            // Calculate delay based on axe type and enchantments
            int delay = ToolUtils.calculateAdjustedDelay(
                tool, 
                plugin.getReplantingDelay(), 
                plugin.isRespectEfficiency()
            );

            // Store the location for replanting
            Location plantLocation = block.getLocation();

            // Schedule replanting if enabled
            if (plugin.isReplantingEnabled()) {
                plugin.debug("Will check for saplings in " + (delay/20.0) + " seconds");
                replantingService.scheduleReplanting(plantLocation, logType, delay);
            }

            // Immediately break all connected logs if cleanup is enabled
            if (plugin.isCleanupEnabled()) {
                // Get all connected logs before they're broken
                Set<Block> connectedLogs = treeDetectionService.findConnectedLogs(block, plugin.getCleanupMaxDistance());
                plugin.debug("Found " + connectedLogs.size() + " connected logs to break");
                
                // Break all logs except the one that was just broken
                for (Block log : connectedLogs) {
                    if (!log.getLocation().equals(block.getLocation())) {
                        log.breakNaturally(tool);
                    }
                }
                
                // For oak trees, do a second pass after a short delay to catch any missed logs
                if (treeType == TreeType.OAK && plugin.isCleanupLargeTrees()) {
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        try {
                            // Find any remaining logs that might have been missed
                            Set<Block> remainingLogs = treeDetectionService.findConnectedLogs(block, plugin.getCleanupMaxDistance());
                            if (!remainingLogs.isEmpty()) {
                                plugin.debug("Second pass found " + remainingLogs.size() + " additional oak logs to break");
                                for (Block log : remainingLogs) {
                                    log.breakNaturally(tool);
                                }
                            }
                        } catch (Exception e) {
                            plugin.getLogger().log(Level.WARNING, "Error in second pass log breaking: " + e.getMessage(), e);
                        }
                    }, 2L); // Short delay (2 ticks) to let the first pass complete
                }
                
                // Schedule cleanup for any remaining floating parts
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    try {
                        cleanupService.cleanupFloatingTreeParts(block);
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Error in scheduled cleanup task: " + e.getMessage(), e);
                    }
                }, delay + 5);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error processing block break event: " + e.getMessage(), e);
        }
    }
} 