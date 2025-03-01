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

            // Schedule cleanup if enabled
            if (plugin.isCleanupEnabled()) {
                // Use a slightly longer delay for cleanup to ensure all blocks are broken first
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