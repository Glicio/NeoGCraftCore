# GCraftCore

Core mod for GCraft server

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
