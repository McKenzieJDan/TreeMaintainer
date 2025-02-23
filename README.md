# TreeMaintainer

A Minecraft Spigot plugin that automatically maintains trees by replanting saplings and removing floating tree parts.

## Features

- Automatically replants saplings when trees are cut down
- Removes floating logs and leaves after tree cutting
- Supports all vanilla tree types (including Mangrove and Cherry trees)
- Only replants if the tree was actually a tree (had leaves above it)
- Cleans up floating leaves that are too far from logs

## Installation

1. Download the latest release from the releases page
2. Place the .jar file in your server's `plugins` folder
3. Restart your server

## Usage

The plugin works automatically once installed. When players break logs that are part of trees:
- A sapling of the same type will be planted where the bottom log was broken
- Any disconnected logs or leaves will be removed
- Leaves that are too far from logs will decay naturally

## Requirements

- Spigot/Paper 1.21.4
- Java 17 or higher

## Building

To build the plugin yourself:

1. Clone the repository
2. Run `mvn clean package`
3. Find the built jar in the `target` folder

## License

MIT License 