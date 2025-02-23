package io.mckenz.treemaintainer; 

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.List;

public class TreeListener {
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
} 