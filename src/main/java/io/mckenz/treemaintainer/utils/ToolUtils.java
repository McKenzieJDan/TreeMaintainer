package io.mckenz.treemaintainer.utils;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

/**
 * Utility class for tool-related operations.
 */
public class ToolUtils {

    /**
     * Check if a material is an axe
     * @param material The material to check
     * @return True if the material is an axe, false otherwise
     */
    public static boolean isAxe(Material material) {
        return material == Material.WOODEN_AXE ||
               material == Material.STONE_AXE ||
               material == Material.IRON_AXE ||
               material == Material.GOLDEN_AXE ||
               material == Material.DIAMOND_AXE ||
               material == Material.NETHERITE_AXE;
    }

    /**
     * Get the base delay for an axe type
     * @param material The axe material
     * @return The base delay in ticks
     */
    public static int getAxeDelay(Material material) {
        if (material == Material.NETHERITE_AXE) {
            return 10;
        } else if (material == Material.DIAMOND_AXE) {
            return 15;
        } else if (material == Material.IRON_AXE) {
            return 20;
        } else if (material == Material.GOLDEN_AXE) {
            return 25;
        } else if (material == Material.STONE_AXE) {
            return 30;
        } else if (material == Material.WOODEN_AXE) {
            return 40;
        } else {
            return 20; // Default delay
        }
    }

    /**
     * Calculate the adjusted delay based on tool type and enchantments
     * @param tool The tool item stack
     * @param baseDelay The base delay to adjust
     * @param respectEfficiency Whether to respect efficiency enchantments
     * @return The adjusted delay in ticks
     */
    public static int calculateAdjustedDelay(ItemStack tool, int baseDelay, boolean respectEfficiency) {
        if (tool == null || !isAxe(tool.getType())) {
            return baseDelay;
        }

        int delay = getAxeDelay(tool.getType());
        
        if (respectEfficiency) {
            int efficiencyLevel = tool.getEnchantmentLevel(Enchantment.EFFICIENCY);
            delay = Math.max(1, delay - (efficiencyLevel * 2));
        }
        
        return delay;
    }
} 