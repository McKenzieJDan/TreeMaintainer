package io.mckenz.treemaintainer.services;

import org.bukkit.Location;
import org.bukkit.Material;

/**
 * Service interface for tree replanting operations.
 */
public interface ReplantingService {

    /**
     * Check if a sapling can be planted at the given location
     * @param location The location to check
     * @param logType The type of log that was broken
     * @return True if a sapling can be planted, false otherwise
     */
    boolean canPlantSapling(Location location, Material logType);
    
    /**
     * Plant a sapling at the given location
     * @param location The location to plant the sapling
     * @param logType The type of log that was broken
     * @return True if the sapling was planted, false otherwise
     */
    boolean plantSapling(Location location, Material logType);
    
    /**
     * Schedule a replanting task for the given location
     * @param location The location to replant
     * @param logType The type of log that was broken
     * @param delay The delay in ticks before replanting
     */
    void scheduleReplanting(Location location, Material logType, int delay);
} 