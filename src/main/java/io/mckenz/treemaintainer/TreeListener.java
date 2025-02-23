package io.mckenz.treemaintainer;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class TreeListener implements Listener {
    private final TreeMaintainer plugin;
    private final List<Material> LOGS = new ArrayList<>();
    private final List<Material> LEAVES = new ArrayList<>();

    public TreeListener(TreeMaintainer plugin) {
        this.plugin = plugin;
        // Initialize log types
        LOGS.add(Material.OAK_LOG);
        LOGS.add(Material.BIRCH_LOG);
        LOGS.add(Material.SPRUCE_LOG);
        LOGS.add(Material.JUNGLE_LOG);
        LOGS.add(Material.ACACIA_LOG);
        LOGS.add(Material.DARK_OAK_LOG);
        LOGS.add(Material.MANGROVE_LOG);
        LOGS.add(Material.CHERRY_LOG);
        
        // Initialize leaf types
        LEAVES.add(Material.OAK_LEAVES);
        LEAVES.add(Material.BIRCH_LEAVES);
        LEAVES.add(Material.SPRUCE_LEAVES);
        LEAVES.add(Material.JUNGLE_LEAVES);
        LEAVES.add(Material.ACACIA_LEAVES);
        LEAVES.add(Material.DARK_OAK_LEAVES);
        LEAVES.add(Material.MANGROVE_LEAVES);
        LEAVES.add(Material.CHERRY_LEAVES);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        
        // Check if the broken block is a log
        if (!LOGS.contains(block.getType())) {
            return;
        }
        plugin.getLogger().info("Log block broken: " + block.getType());

        // Find the bottom-most log of this tree
        Block bottom = findBottomLog(block);
        
        // Check if it's part of a tree (has leaves nearby)
        if (!isPartOfTree(block)) {
            plugin.getLogger().info("Not part of tree - no leaves found nearby");
            return;
        }
        plugin.getLogger().info("Tree detected - replanting sapling at bottom");

        // Schedule the replanting task
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            replantSapling(bottom);
            removeFloatingTree(block);
        }, 2L);
    }

    private Block findBottomLog(Block start) {
        Block current = start;
        while (LOGS.contains(current.getRelative(BlockFace.DOWN).getType())) {
            current = current.getRelative(BlockFace.DOWN);
        }
        return current;
    }

    private boolean isPartOfTree(Block block) {
        // Check in a radius around the block for leaves
        for (int y = -2; y <= 4; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    Block relative = block.getRelative(x, y, z);
                    if (LEAVES.contains(relative.getType())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void replantSapling(Block block) {
        Block ground = block.getRelative(BlockFace.DOWN);
        if (ground.getType() == Material.DIRT || ground.getType() == Material.GRASS_BLOCK 
            || ground.getType() == Material.MUD || ground.getType() == Material.MUDDY_MANGROVE_ROOTS) {
            Material sapling;
            switch (block.getType()) {
                case OAK_LOG:
                    sapling = Material.OAK_SAPLING;
                    break;
                case BIRCH_LOG:
                    sapling = Material.BIRCH_SAPLING;
                    break;
                case SPRUCE_LOG:
                    sapling = Material.SPRUCE_SAPLING;
                    break;
                case JUNGLE_LOG:
                    sapling = Material.JUNGLE_SAPLING;
                    break;
                case ACACIA_LOG:
                    sapling = Material.ACACIA_SAPLING;
                    break;
                case DARK_OAK_LOG:
                    sapling = Material.DARK_OAK_SAPLING;
                    break;
                case MANGROVE_LOG:
                    sapling = Material.MANGROVE_PROPAGULE;
                    break;
                case CHERRY_LOG:
                    sapling = Material.CHERRY_SAPLING;
                    break;
                default:
                    return;
            }
            block.setType(sapling);
        }
    }

    private void removeFloatingTree(Block startBlock) {
        Queue<Block> blocksToCheck = new LinkedList<>();
        Set<Block> checkedBlocks = new HashSet<>();
        blocksToCheck.add(startBlock);

        while (!blocksToCheck.isEmpty()) {
            Block current = blocksToCheck.poll();
            if (checkedBlocks.contains(current)) {
                continue;
            }
            checkedBlocks.add(current);

            // If it's not a log or leaves, skip
            if (!LOGS.contains(current.getType()) && !LEAVES.contains(current.getType())) {
                continue;
            }

            // Check if this is a floating block
            if (isFloating(current)) {
                current.breakNaturally();
            }

            // Add adjacent blocks to check
            for (BlockFace face : new BlockFace[]{BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                Block relative = current.getRelative(face);
                if (!checkedBlocks.contains(relative)) {
                    blocksToCheck.add(relative);
                }
            }
        }
    }

    private boolean isFloating(Block block) {
        // If it's a log, check if it's connected to the ground
        if (LOGS.contains(block.getType())) {
            Block below = block.getRelative(BlockFace.DOWN);
            while (LOGS.contains(below.getType())) {
                below = below.getRelative(BlockFace.DOWN);
            }
            return !(below.getType() == Material.DIRT || below.getType() == Material.GRASS_BLOCK);
        }
        
        // If it's leaves, check if it's too far from any log
        if (LEAVES.contains(block.getType())) {
            return !hasNearbyLog(block, 5);
        }
        
        return false;
    }

    private boolean hasNearbyLog(Block block, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block relative = block.getRelative(x, y, z);
                    if (LOGS.contains(relative.getType())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
} 