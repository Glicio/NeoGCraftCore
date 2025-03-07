# GCraftCore

Core mod for GCraft server

GCraftCore is a comprehensive Minecraft server mod designed to enhance the multiplayer experience with essential features for both players and administrators. This mod serves as the foundation for the GCraft server, providing crucial functionality for chat management, player teleportation, and shop systems.

## Features

- **Advanced Chat System**
  - Global and local chat channels
  - Custom chat formatting with prefixes
  - LuckPerms integration for chat permissions

- **Teleportation System**
  - Server spawn point management
  - Player spawn commands
  - Safe teleportation handling

- **Shop System**
  - Player-owned shops
  - Admin shops with unlimited inventory
  - Administrative controls for shop management
  - Secure trading functionality

- **Permission Management**
  - Integration with LuckPerms
  - Granular permission control
  - Multiple permission levels for different user roles

- **Administrative Tools**
  - Debug commands for server maintenance
  - Server spawn point configuration
  - Shop administration capabilities

## Permissions

This mod uses both Minecraft's built-in permission levels and LuckPerms for managing permissions. Below is a comprehensive list of all permissions:

### Command Permissions

#### Player Commands (Permission Level 0)
- `/spawn` - Teleport to the server spawn point
- `/g` - Switch to global chat
- `/l` - Switch to local chat

#### Operator Commands (Permission Level 2)
- `/setspawn` - Set the server spawn point
- `/debug` - Debug command for database initialization

### Shop Permissions
- `gcraftcore.shop.admin` - Allows players to delete any shop, regardless of ownership
- Shop owners can delete their own shops without this permission

### Chat Permissions
The mod integrates with LuckPerms for chat formatting:
- `prefix.{priority}.{prefix}` - Sets the player's chat prefix
- `color.{color_number}` - Sets the color of the player's prefix

### LuckPerms Integration
The mod requires LuckPerms to be installed on the server for proper functionality. It uses LuckPerms for:
- Chat formatting and prefixes
- Group-based permissions
- Shop administration permissions

### Permission Levels
- Level 0: Regular players
- Level 2: Server operators
- LuckPerms groups: Custom permissions through LuckPerms groups
