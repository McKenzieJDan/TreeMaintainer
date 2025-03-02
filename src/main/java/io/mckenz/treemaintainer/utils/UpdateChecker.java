package io.mckenz.treemaintainer.utils;

import io.mckenz.treemaintainer.TreeMaintainer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.logging.Level;
import java.net.URISyntaxException;

/**
 * Utility class for checking for plugin updates.
 */
public class UpdateChecker implements Listener {

    private final TreeMaintainer plugin;
    private final int resourceId;
    private String latestVersion;
    private boolean updateAvailable = false;

    /**
     * Create a new update checker
     * @param plugin The plugin instance
     * @param resourceId The SpigotMC resource ID
     */
    public UpdateChecker(TreeMaintainer plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
        
        // Register the join event listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Check for updates
     */
    public void checkForUpdates() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String currentVersion = plugin.getDescription().getVersion();
                latestVersion = fetchLatestVersion();
                
                if (latestVersion == null) {
                    plugin.getLogger().warning("Failed to check for updates.");
                    return;
                }
                
                // Normalize versions for comparison
                String normalizedCurrent = normalizeVersion(currentVersion);
                String normalizedLatest = normalizeVersion(latestVersion);
                
                if (!normalizedCurrent.equalsIgnoreCase(normalizedLatest)) {
                    updateAvailable = true;
                    plugin.getLogger().info("A new update is available: v" + latestVersion);
                    plugin.getLogger().info("You are currently running: v" + currentVersion);
                    plugin.getLogger().info("Download the latest version from: https://www.spigotmc.org/resources/" + resourceId);
                } else {
                    plugin.getLogger().info("You are running the latest version: v" + currentVersion);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to check for updates: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Fetch the latest version from SpigotMC API
     * @return The latest version string or null if the check failed
     */
    private String fetchLatestVersion() {
        try {
            URI uri = new URI("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId);
            URL url = uri.toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    return reader.readLine();
                }
            } else {
                plugin.getLogger().warning("Failed to check for updates: HTTP response code " + responseCode);
            }
        } catch (URISyntaxException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to create URI for update check", e);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to check for updates", e);
        }
        return null;
    }
    
    /**
     * Normalize a version string for comparison
     * @param version The version string to normalize
     * @return The normalized version string
     */
    private String normalizeVersion(String version) {
        // Remove 'v' prefix if present
        if (version.startsWith("v")) {
            version = version.substring(1);
        }
        
        // Remove any suffixes like -RELEASE, -SNAPSHOT, etc.
        int dashIndex = version.indexOf('-');
        if (dashIndex > 0) {
            version = version.substring(0, dashIndex);
        }
        
        return version.trim();
    }

    /**
     * Check if an update is available
     * @return True if an update is available, false otherwise
     */
    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    /**
     * Get the latest version
     * @return The latest version string
     */
    public String getLatestVersion() {
        return latestVersion;
    }

    /**
     * Notify admins when they join if an update is available
     * @param event The player join event
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Only notify players with permission
        if (updateAvailable && player.hasPermission("treemaintainer.update")) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage(ChatColor.GREEN + "[TreeMaintainer] " + ChatColor.YELLOW + "A new update is available: " + 
                                  ChatColor.WHITE + "v" + latestVersion + ChatColor.YELLOW + " (Current: " + 
                                  ChatColor.WHITE + "v" + plugin.getDescription().getVersion() + ChatColor.YELLOW + ")");
                player.sendMessage(ChatColor.GREEN + "[TreeMaintainer] " + ChatColor.YELLOW + "Download it at: " + 
                                  ChatColor.WHITE + "https://www.spigotmc.org/resources/" + resourceId);
            }, 40L); // Delay for 2 seconds after join
        }
    }
} 