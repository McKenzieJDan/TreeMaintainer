package io.mckenz.treemaintainer.commands;

import io.mckenz.treemaintainer.TreeMaintainer;
import io.mckenz.treemaintainer.utils.UpdateChecker;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Command handler for TreeMaintainer plugin.
 */
public class TreeMaintainerCommand implements CommandExecutor, TabCompleter {

    private final TreeMaintainer plugin;
    private static final List<String> SUBCOMMANDS = Arrays.asList("reload", "enable", "disable", "info", "update");

    public TreeMaintainerCommand(TreeMaintainer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (args.length == 0) {
                sendHelp(sender);
                return true;
            }

            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "reload":
                    if (!sender.hasPermission("treemaintainer.reload")) {
                        sender.sendMessage(ChatColor.RED + "You don't have permission to reload the plugin.");
                        return true;
                    }
                    try {
                        plugin.reloadPluginConfig();
                        sender.sendMessage(ChatColor.GREEN + "TreeMaintainer configuration reloaded!");
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Error reloading configuration: " + e.getMessage(), e);
                        sender.sendMessage(ChatColor.RED + "Error reloading configuration. Check console for details.");
                    }
                    break;

                case "enable":
                    if (!sender.hasPermission("treemaintainer.toggle")) {
                        sender.sendMessage(ChatColor.RED + "You don't have permission to enable the plugin.");
                        return true;
                    }
                    try {
                        plugin.getConfig().set("enabled", true);
                        plugin.saveConfig();
                        plugin.reloadPluginConfig();
                        sender.sendMessage(ChatColor.GREEN + "TreeMaintainer has been enabled!");
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Error enabling plugin: " + e.getMessage(), e);
                        sender.sendMessage(ChatColor.RED + "Error enabling plugin. Check console for details.");
                    }
                    break;

                case "disable":
                    if (!sender.hasPermission("treemaintainer.toggle")) {
                        sender.sendMessage(ChatColor.RED + "You don't have permission to disable the plugin.");
                        return true;
                    }
                    try {
                        plugin.getConfig().set("enabled", false);
                        plugin.saveConfig();
                        plugin.reloadPluginConfig();
                        sender.sendMessage(ChatColor.YELLOW + "TreeMaintainer has been disabled!");
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Error disabling plugin: " + e.getMessage(), e);
                        sender.sendMessage(ChatColor.RED + "Error disabling plugin. Check console for details.");
                    }
                    break;

                case "info":
                    if (!sender.hasPermission("treemaintainer.info")) {
                        sender.sendMessage(ChatColor.RED + "You don't have permission to view plugin info.");
                        return true;
                    }
                    sendInfo(sender);
                    break;
                    
                case "update":
                    if (!sender.hasPermission("treemaintainer.update")) {
                        sender.sendMessage(ChatColor.RED + "You don't have permission to check for updates.");
                        return true;
                    }
                    checkForUpdates(sender);
                    break;

                default:
                    sender.sendMessage(ChatColor.RED + "Unknown command. Use /treemaintainer for help.");
                    break;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error executing command: " + e.getMessage(), e);
            sender.sendMessage(ChatColor.RED + "An error occurred while executing the command. Check console for details.");
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        try {
            sender.sendMessage(ChatColor.GREEN + "=== TreeMaintainer Commands ===");
            sender.sendMessage(ChatColor.YELLOW + "/treemaintainer reload " + ChatColor.WHITE + "- Reload the configuration");
            sender.sendMessage(ChatColor.YELLOW + "/treemaintainer enable " + ChatColor.WHITE + "- Enable the plugin");
            sender.sendMessage(ChatColor.YELLOW + "/treemaintainer disable " + ChatColor.WHITE + "- Disable the plugin");
            sender.sendMessage(ChatColor.YELLOW + "/treemaintainer info " + ChatColor.WHITE + "- Show plugin information");
            sender.sendMessage(ChatColor.YELLOW + "/treemaintainer update " + ChatColor.WHITE + "- Check for updates");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error sending help message: " + e.getMessage(), e);
        }
    }

    private void sendInfo(CommandSender sender) {
        try {
            sender.sendMessage(ChatColor.GREEN + "=== TreeMaintainer Info ===");
            sender.sendMessage(ChatColor.YELLOW + "Version: " + ChatColor.WHITE + plugin.getDescription().getVersion());
            sender.sendMessage(ChatColor.YELLOW + "Status: " + (plugin.isPluginEnabled() ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"));
            sender.sendMessage(ChatColor.YELLOW + "Replanting: " + (plugin.isReplantingEnabled() ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"));
            sender.sendMessage(ChatColor.YELLOW + "Cleanup: " + (plugin.isCleanupEnabled() ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"));
            sender.sendMessage(ChatColor.YELLOW + "Require Axe: " + (plugin.isRequireAxe() ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));
            
            // Show update information if available
            if (plugin.isUpdateCheckerEnabled() && plugin.getUpdateChecker() != null) {
                UpdateChecker updateChecker = plugin.getUpdateChecker();
                if (updateChecker.isUpdateAvailable()) {
                    sender.sendMessage(ChatColor.YELLOW + "Update Available: " + ChatColor.GREEN + "Yes " + 
                                      ChatColor.YELLOW + "(" + ChatColor.WHITE + updateChecker.getLatestVersion() + ChatColor.YELLOW + ")");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error sending info message: " + e.getMessage(), e);
        }
    }
    
    private void checkForUpdates(CommandSender sender) {
        try {
            if (!plugin.isUpdateCheckerEnabled()) {
                sender.sendMessage(ChatColor.YELLOW + "Update checker is disabled in the configuration.");
                return;
            }
            
            UpdateChecker updateChecker = plugin.getUpdateChecker();
            if (updateChecker == null) {
                sender.sendMessage(ChatColor.YELLOW + "Update checker is not initialized. Please reload the plugin.");
                return;
            }
            
            // Force a new update check
            sender.sendMessage(ChatColor.YELLOW + "Checking for updates...");
            updateChecker.checkForUpdates();
            
            // Wait a moment for the check to complete
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (updateChecker.isUpdateAvailable()) {
                    sender.sendMessage(ChatColor.GREEN + "A new update is available: " + 
                                      ChatColor.WHITE + updateChecker.getLatestVersion() + 
                                      ChatColor.GREEN + " (Current: " + 
                                      ChatColor.WHITE + plugin.getDescription().getVersion() + 
                                      ChatColor.GREEN + ")");
                    sender.sendMessage(ChatColor.GREEN + "Download it at: " + 
                                      ChatColor.WHITE + "https://www.spigotmc.org/resources/" + 
                                      plugin.getConfig().getInt("update-checker.resource-id"));
                } else {
                    sender.sendMessage(ChatColor.GREEN + "You are running the latest version: " + 
                                      ChatColor.WHITE + plugin.getDescription().getVersion());
                }
            }, 40L); // Wait 2 seconds for the async task to complete
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking for updates: " + e.getMessage(), e);
            sender.sendMessage(ChatColor.RED + "Error checking for updates. Check console for details.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        try {
            if (args.length == 1) {
                String partialCommand = args[0].toLowerCase();
                return SUBCOMMANDS.stream()
                        .filter(cmd -> cmd.startsWith(partialCommand))
                        .filter(cmd -> hasPermissionForCommand(sender, cmd))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in tab completion: " + e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    private boolean hasPermissionForCommand(CommandSender sender, String command) {
        try {
            switch (command) {
                case "reload":
                    return sender.hasPermission("treemaintainer.reload");
                case "enable":
                case "disable":
                    return sender.hasPermission("treemaintainer.toggle");
                case "info":
                    return sender.hasPermission("treemaintainer.info");
                case "update":
                    return sender.hasPermission("treemaintainer.update");
                default:
                    return true;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking permission: " + e.getMessage(), e);
            return false;
        }
    }
} 