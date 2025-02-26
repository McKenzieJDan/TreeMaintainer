# TreeMaintainer

A Minecraft Spigot plugin that automatically maintains trees by replanting saplings and removing floating parts, creating a more natural and tidy environment.

[![SpigotMC](https://img.shields.io/badge/SpigotMC-TreeMaintainer-orange)](https://www.spigotmc.org/resources/)
[![Donate](https://img.shields.io/badge/Donate-PayPal-blue.svg)](https://www.paypal.com/paypalme/mckenzio)

## Features

- 🌱 Automatically replants saplings after tree cutting
- 🪓 Tool-aware (only works with axes)
- ⚡ Respects tool efficiency enchantments
- 🍃 Natural leaf decay simulation
- 🌲 Supports all vanilla trees
- 🧹 Removes floating logs and leaves
- ⚙️ Fully configurable behavior
- 🔄 Per-tree type settings

## Installation

1. Download the latest release from [Spigot](https://www.spigotmc.org/resources/) or [GitHub Releases](https://github.com/McKenzieJDan/Tree-Maintainer/releases)
2. Place the JAR file in your server's `plugins` folder
3. Restart your server
4. Configure the plugin in the `config.yml` file

## Usage

The plugin works automatically when players cut down trees with axes. No player action or commands are required.

When a player cuts a tree with an axe:
1. The plugin detects the tree type
2. After the tree is cut, a sapling is automatically replanted
3. Any floating logs or leaves are cleaned up naturally

## Configuration

The plugin's configuration file (`config.yml`) is organized into logical sections:

```yaml
# Master switch to enable or disable the plugin
enabled: true

# Auto-replant settings
replanting:
  # Enable automatic sapling replanting
  enabled: true
  
  # Delay in ticks before replanting (20 ticks = 1 second)
  delay: 5

# Floating tree parts cleanup
cleanup:
  # Enable removal of floating logs and leaves
  enabled: true
  
  # Maximum distance to check for connected blocks
  max-distance: 10

# Tool settings
tools:
  # Only trigger the plugin when trees are cut with axes
  require-axe: true
  
  # Respect tool efficiency enchantments
  respect-efficiency: true

# Enable/disable specific tree types
tree-types:
  oak: true
  spruce: true
  birch: true
  acacia: true
  dark_oak: true
  mangrove: true
  cherry: true
  jungle: false

# Debug mode for troubleshooting
debug: false
```

## Requirements

- Spigot/Paper 1.21.4
- Java 21+

## Support

If you find this plugin helpful, consider [buying me a coffee](https://www.paypal.com/paypalme/mckenzio) ☕

## License

[MIT License](LICENSE)

Made with ❤️ by [McKenzieJDan](https://github.com/McKenzieJDan)