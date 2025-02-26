package io.mckenz.treemaintainer;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;

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
        // Check if plugin is enabled
        if (!plugin.isPluginEnabled()) {
            return;
        }
        
        Block block = event.getBlock();
        
        // Check if the broken block is a log
        if (!LOGS.contains(block.getType())) {
            return;
        }

        // Check if this tree type is enabled
        String treeType = getTreeType(block.getType());
        if (!plugin.isTreeTypeEnabled(treeType)) {
            plugin.debug(treeType + " tree type is disabled - skipping processing");
            return;
        }
        
        // Check if player is using an axe
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        if (plugin.isRequireAxe() && !isAxe(tool.getType())) {
            plugin.debug("Not using an axe - skipping tree processing");
            return;
        }

        // Store the log type before it's broken
        Material logType = block.getType();
        plugin.debug("Log block broken with " + tool.getType() + " at " + block.getLocation());

        // Calculate delay based on axe type
        int baseDelay = getAxeDelay(tool.getType());
        int efficiencyLevel = tool.getEnchantmentLevel(Enchantment.EFFICIENCY);
        baseDelay = Math.max(1, baseDelay - (efficiencyLevel * 2));

        // Store the location for replanting
        Location plantLocation = block.getLocation();

        // Only schedule replanting if enabled
        if (plugin.isReplantingEnabled()) {
            // Use the configured delay (already in ticks)
            int plantCheckDelay = plugin.getReplantingDelay();
            plugin.debug("Will check for saplings in " + (plantCheckDelay/20.0) + " seconds");

            // Schedule the replanting task
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                checkAndReplant(plantLocation, logType);
            }, plantCheckDelay);
        }

        // Handle floating tree parts immediately
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            checkFloatingTreeParts(block.getLocation(), tool);
        }, 2L);
    }

    private Block findBottomLog(Block start) {
        Block current = start;
        Block below = current.getRelative(BlockFace.DOWN);
        
        plugin.debug("Starting at block: " + start.getType() + " at " + start.getLocation());
        plugin.debug("Block below is: " + below.getType());
        
        // Keep going down as long as we find logs that still exist
        while (below != null && LOGS.contains(below.getType())) {
            current = below;
            below = current.getRelative(BlockFace.DOWN);
            plugin.debug("Found log below, moving down. Now at: " + current.getType());
            plugin.debug("New block below is: " + below.getType());
        }
        
        // Check if the block below is valid ground
        Material belowType = below.getType();
        plugin.debug("Final ground block is: " + belowType);
        
        if (belowType == Material.DIRT || belowType == Material.GRASS_BLOCK 
            || belowType == Material.MUD || belowType == Material.MUDDY_MANGROVE_ROOTS
            || belowType == Material.PODZOL || belowType == Material.COARSE_DIRT
            || belowType == Material.ROOTED_DIRT) {
            plugin.debug("Found valid ground block: " + belowType);
            return current;
        }
        
        plugin.debug("No valid ground found, found: " + belowType);
        return null;  // Return null if not connected to valid ground
    }

    private boolean isPartOfTree(Block block) {
        // For spruce trees, check higher up for leaves
        int maxHeight = block.getType() == Material.SPRUCE_LOG ? 15 : 4;
        int radius = block.getType() == Material.SPRUCE_LOG ? 3 : 2;
        
        // Check in a radius around the block for leaves
        for (int y = -2; y <= maxHeight; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    Block relative = block.getRelative(x, y, z);
                    if (LEAVES.contains(relative.getType())) {
                        plugin.debug("Found leaves at relative position: " + x + "," + y + "," + z);
                        return true;
                    }
                }
            }
        }
        plugin.debug("No leaves found within radius " + radius + " and height " + maxHeight);
        return false;
    }

    private void replantSapling(Location location, Material logType) {
        Block block = location.getBlock();
        Block ground = block.getRelative(BlockFace.DOWN);
        plugin.debug("Attempting to plant sapling. Ground block is: " + ground.getType());
        
        if (ground.getType() == Material.DIRT 
            || ground.getType() == Material.GRASS_BLOCK 
            || ground.getType() == Material.MUD 
            || ground.getType() == Material.MUDDY_MANGROVE_ROOTS
            || ground.getType() == Material.PODZOL 
            || ground.getType() == Material.COARSE_DIRT
            || ground.getType() == Material.ROOTED_DIRT) {
            
            Material sapling;
            switch (logType) {  // Use the stored log type instead of current block type
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
                    plugin.debug("Unknown log type: " + logType);
                    return;
            }
            plugin.debug("Setting block to sapling type: " + sapling);
            block.setType(sapling);
            plugin.debug("Block is now: " + block.getType());
        } else {
            plugin.debug("Invalid ground type for sapling: " + ground.getType());
        }
    }

    private Set<Block> findConnectedTreeBlocks(Block startBlock) {
        Set<Block> treeBlocks = new HashSet<>();
        Queue<Block> toCheck = new LinkedList<>();
        toCheck.add(startBlock);
        
        // Find all connected logs and leaves
        while (!toCheck.isEmpty()) {
            Block current = toCheck.poll();
            if (treeBlocks.contains(current)) continue;
            
            if (LOGS.contains(current.getType()) || LEAVES.contains(current.getType())) {
                treeBlocks.add(current);
                
                // Check adjacent blocks (including diagonals for leaves)
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            if (x == 0 && y == 0 && z == 0) continue;
                            Block relative = current.getRelative(x, y, z);
                            if (!treeBlocks.contains(relative)) {
                                if (LOGS.contains(relative.getType())) {
                                    toCheck.add(relative);
                                } else if (LEAVES.contains(relative.getType()) && 
                                         (LOGS.contains(current.getType()) || Math.abs(x) + Math.abs(y) + Math.abs(z) <= 2)) {
                                    // Only add leaves that are directly connected to logs
                                    // or closely connected to other leaves
                                    toCheck.add(relative);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return treeBlocks;
    }

    private boolean isFloating(Block block) {
        // If it's a log, check if it's connected to the ground
        if (LOGS.contains(block.getType())) {
            Block below = block.getRelative(BlockFace.DOWN);
            Block current = block;
            plugin.debug("Checking if log is floating at: " + block.getLocation());
            
            while (LOGS.contains(below.getType())) {
                current = below;
                below = current.getRelative(BlockFace.DOWN);
                plugin.debug("Found log below, checking deeper...");
            }
            
            Material belowType = below.getType();
            plugin.debug("Bottom-most block under log chain is: " + belowType);
            
            boolean isFloating = !(belowType == Material.DIRT 
                || belowType == Material.GRASS_BLOCK 
                || belowType == Material.MUD 
                || belowType == Material.MUDDY_MANGROVE_ROOTS
                || belowType == Material.PODZOL 
                || belowType == Material.COARSE_DIRT
                || belowType == Material.ROOTED_DIRT);
            
            if (isFloating) {
                plugin.debug("Log is floating and will be removed!");
            }
            return isFloating;
        }
        
        return LEAVES.contains(block.getType());
    }

    private void removeFloatingTree(Block startBlock) {
        // First find all blocks that are part of this tree
        Set<Block> treeBlocks = findConnectedTreeBlocks(startBlock);
        plugin.debug("Found " + treeBlocks.size() + " blocks in this tree at " + startBlock.getLocation());
        
        // Then check which ones are floating
        for (Block block : treeBlocks) {
            if (isFloating(block)) {
                if (LOGS.contains(block.getType())) {
                    plugin.debug("Breaking floating log at: " + block.getLocation());
                    block.breakNaturally();
                } else if (LEAVES.contains(block.getType())) {
                    int randomDelay = 5 + (int)(Math.random() * 35);
                    plugin.debug("Scheduling leaf decay at: " + block.getLocation() + " in " + (randomDelay/20.0) + " seconds");
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (LEAVES.contains(block.getType()) && treeBlocks.contains(block)) {
                            block.breakNaturally();
                        }
                    }, randomDelay);
                }
            }
        }
    }

    private void checkFloatingTreeParts(Location brokenBlockLoc, ItemStack tool) {
        Set<Block> checkedBlocks = new HashSet<>();
        Queue<Block> toCheck = new LinkedList<>();
        
        // Determine if this might be a jungle tree (they're much wider)
        boolean isJungleTree = false;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block nearby = brokenBlockLoc.getBlock().getRelative(x, 0, z);
                if (nearby.getType() == Material.JUNGLE_LOG) {
                    isJungleTree = true;
                    break;
                }
            }
        }
        
        // Use larger radius for jungle trees
        int radius = isJungleTree ? 6 : 3;
        int height = isJungleTree ? 30 : 20;
        
        plugin.debug("Checking for " + (isJungleTree ? "jungle" : "normal") + " tree parts in radius " + radius + " up to height " + height);
        
        // Check all blocks in a radius around the broken block
        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= height; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = brokenBlockLoc.getBlock().getRelative(x, y, z);
                    if (LOGS.contains(block.getType())) {
                        toCheck.add(block);
                        plugin.debug("Found log to check at relative position: " + x + "," + y + "," + z);
                    }
                }
            }
        }

        plugin.debug("Found " + toCheck.size() + " logs to check near " + brokenBlockLoc);

        // Process each log
        while (!toCheck.isEmpty()) {
            Block current = toCheck.poll();
            if (checkedBlocks.contains(current)) continue;
            checkedBlocks.add(current);

            if (!isConnectedToGround(current)) {
                plugin.debug("Breaking floating log at " + current.getLocation());
                
                // Calculate break delay based on axe type and enchantments
                int breakDelay = getAxeDelay(tool.getType());
                int efficiencyLevel = tool.getEnchantmentLevel(Enchantment.EFFICIENCY);
                breakDelay = Math.max(1, breakDelay - (efficiencyLevel * 2));
                
                // Schedule the break with the calculated delay
                final Block blockToBreak = current;
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    blockToBreak.breakNaturally(tool); // Use the tool to break for proper drops
                }, breakDelay);

                // Handle leaves with slightly longer delay
                for (int x = -2; x <= 2; x++) {
                    for (int y = -2; y <= 2; y++) {
                        for (int z = -2; z <= 2; z++) {
                            Block nearby = current.getRelative(x, y, z);
                            if (LEAVES.contains(nearby.getType())) {
                                int leafDelay = breakDelay + 5 + (int)(Math.random() * 15);
                                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                    nearby.breakNaturally();
                                }, leafDelay);
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isConnectedToGround(Block log) {
        Set<Block> checkedBlocks = new HashSet<>();
        Queue<Block> toCheck = new LinkedList<>();
        toCheck.add(log);
        
        // For jungle trees, also check the other potential trunk blocks
        if (log.getType() == Material.JUNGLE_LOG) {
            for (int x = -1; x <= 0; x++) {
                for (int z = -1; z <= 0; z++) {
                    Block trunk = log.getRelative(x, 0, z);
                    if (trunk.getType() == Material.JUNGLE_LOG) {
                        toCheck.add(trunk);
                    }
                }
            }
        }

        while (!toCheck.isEmpty()) {
            Block current = toCheck.poll();
            if (checkedBlocks.contains(current)) continue;
            checkedBlocks.add(current);

            // Check if this log is on valid ground
            Block below = current.getRelative(BlockFace.DOWN);
            Material belowType = below.getType();
            
            if (belowType == Material.DIRT 
                || belowType == Material.GRASS_BLOCK 
                || belowType == Material.MUD 
                || belowType == Material.MUDDY_MANGROVE_ROOTS
                || belowType == Material.PODZOL 
                || belowType == Material.COARSE_DIRT
                || belowType == Material.ROOTED_DIRT) {
                return true;
            }

            // If below is a log, add it to check
            if (LOGS.contains(belowType)) {
                toCheck.add(below);
            }

            // Also check adjacent logs (for diagonal connections)
            for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                Block adjacent = current.getRelative(face);
                if (LOGS.contains(adjacent.getType())) {
                    toCheck.add(adjacent);
                }
            }
        }

        return false;
    }

    private boolean isAxe(Material material) {
        return material == Material.WOODEN_AXE
            || material == Material.STONE_AXE
            || material == Material.IRON_AXE
            || material == Material.GOLDEN_AXE
            || material == Material.DIAMOND_AXE
            || material == Material.NETHERITE_AXE;
    }

    private int getAxeDelay(Material axeType) {
        switch (axeType) {
            case NETHERITE_AXE: return 2;
            case DIAMOND_AXE: return 3;
            case IRON_AXE: return 4;
            case STONE_AXE: return 5;
            case GOLDEN_AXE: return 5;
            case WOODEN_AXE: return 6;
            default: return 10;
        }
    }

    private boolean isSapling(Material material) {
        return material == Material.OAK_SAPLING
            || material == Material.BIRCH_SAPLING
            || material == Material.SPRUCE_SAPLING
            || material == Material.JUNGLE_SAPLING
            || material == Material.ACACIA_SAPLING
            || material == Material.DARK_OAK_SAPLING
            || material == Material.MANGROVE_PROPAGULE
            || material == Material.CHERRY_SAPLING;
    }

    private void checkAndReplant(Location location, Material logType) {
        Block checkBlock = location.getBlock();
        
        // For jungle trees, we need to check and plant in a 2x2 pattern
        if (logType == Material.JUNGLE_LOG) {
            // Find the northwest corner of the 2x2 trunk
            Block nwCorner = null;
            for (int x = -1; x <= 0; x++) {
                for (int z = -1; z <= 0; z++) {
                    Block potential = checkBlock.getRelative(x, 0, z);
                    if (potential.getType() == Material.AIR) {
                        boolean is2x2Corner = true;
                        // Check if this is part of a 2x2 pattern
                        for (int dx = 0; dx <= 1; dx++) {
                            for (int dz = 0; dz <= 1; dz++) {
                                if (potential.getRelative(dx, 0, dz).getType() != Material.AIR) {
                                    is2x2Corner = false;
                                    break;
                                }
                            }
                        }
                        if (is2x2Corner) {
                            nwCorner = potential;
                            break;
                        }
                    }
                }
            }

            if (nwCorner != null) {
                // Check ground blocks for all 4 positions
                boolean validGround = true;
                for (int x = 0; x <= 1; x++) {
                    for (int z = 0; z <= 1; z++) {
                        Block ground = nwCorner.getRelative(x, -1, z);
                        if (!isValidGround(ground.getType())) {
                            validGround = false;
                            break;
                        }
                    }
                }

                if (validGround) {
                    boolean saplingFound = false;
                    // Check for dropped saplings in the 2x2 area
                    for (Entity entity : nwCorner.getWorld().getNearbyEntities(nwCorner.getLocation().add(0.5, 0, 0.5), 1.5, 1, 1.5)) {
                        if (entity instanceof Item) {
                            ItemStack stack = ((Item) entity).getItemStack();
                            if (stack.getType() == Material.JUNGLE_SAPLING) {
                                saplingFound = true;
                                // Remove 4 saplings if possible
                                if (stack.getAmount() >= 4) {
                                    stack.setAmount(stack.getAmount() - 4);
                                    ((Item) entity).setItemStack(stack);
                                } else {
                                    entity.remove();
                                }
                                break;
                            }
                        }
                    }

                    // Plant 2x2 jungle saplings
                    if (!saplingFound) {
                        plugin.debug("Planting 2x2 jungle saplings");
                        for (int x = 0; x <= 1; x++) {
                            for (int z = 0; z <= 1; z++) {
                                nwCorner.getRelative(x, 0, z).setType(Material.JUNGLE_SAPLING);
                            }
                        }
                    }
                }
            }
        } else {
            // Original code for other tree types
            if (checkBlock.getType() == Material.AIR) {
                // Check for items in a small radius
                boolean saplingFound = false;
                Item foundSapling = null;
                
                for (Entity entity : checkBlock.getWorld().getNearbyEntities(location, 1, 1, 1)) {
                    if (entity instanceof Item) {
                        Item item = (Item) entity;
                        ItemStack stack = item.getItemStack();
                        if (isSapling(stack.getType())) {
                            saplingFound = true;
                            foundSapling = item;
                            break;
                        }
                    }
                }
                
                if (!saplingFound) {
                    plugin.debug("No sapling found, planting one");
                    replantSapling(location, logType);
                } else {
                    // Use the found sapling instead of creating a new one
                    plugin.debug("Using dropped sapling for replanting");
                    ItemStack stack = foundSapling.getItemStack();
                    if (stack.getAmount() > 1) {
                        stack.setAmount(stack.getAmount() - 1);
                        foundSapling.setItemStack(stack);
                    } else {
                        foundSapling.remove();
                    }
                    checkBlock.setType(getSaplingType(logType));
                }
            } else {
                plugin.debug("Block is now " + checkBlock.getType() + ", skipping replant");
            }
        }
    }

    private Material getSaplingType(Material logType) {
        switch (logType) {
            case OAK_LOG: return Material.OAK_SAPLING;
            case BIRCH_LOG: return Material.BIRCH_SAPLING;
            case SPRUCE_LOG: return Material.SPRUCE_SAPLING;
            case JUNGLE_LOG: return Material.JUNGLE_SAPLING;
            case ACACIA_LOG: return Material.ACACIA_SAPLING;
            case DARK_OAK_LOG: return Material.DARK_OAK_SAPLING;
            case MANGROVE_LOG: return Material.MANGROVE_PROPAGULE;
            case CHERRY_LOG: return Material.CHERRY_SAPLING;
            default: return null;
        }
    }

    private boolean isValidGround(Material type) {
        return type == Material.DIRT 
            || type == Material.GRASS_BLOCK 
            || type == Material.MUD 
            || type == Material.MUDDY_MANGROVE_ROOTS
            || type == Material.PODZOL 
            || type == Material.COARSE_DIRT
            || type == Material.ROOTED_DIRT;
    }

    private String getTreeType(Material logType) {
        switch (logType) {
            case OAK_LOG: return "oak";
            case BIRCH_LOG: return "birch";
            case SPRUCE_LOG: return "spruce";
            case JUNGLE_LOG: return "jungle";
            case ACACIA_LOG: return "acacia";
            case DARK_OAK_LOG: return "dark_oak";
            case MANGROVE_LOG: return "mangrove";
            case CHERRY_LOG: return "cherry";
            default: return null;
        }
    }
} 