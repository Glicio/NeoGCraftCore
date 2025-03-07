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

## Official Server

GCraftCore is actively used on our official server, which runs the All the Mods modpack. Since this is a server-side mod, you only need to install the client modpack to join.

### Server Information
- **Server Address:** `mc.glicio.dev`
- **Modpack:** All the Mods 10
- **Current Version:** 2.38
- **Discord:** [Join our community](https://discord.gg/EMjmRh5g)

### How to Join

1. **Install CurseForge:**
   - Download and install the [CurseForge Launcher](https://www.curseforge.com/download/app)
   - Launch the application and navigate to the Minecraft section

2. **Install the Modpack:**
   - Search for "All the Mods 10" in the CurseForge app
   - Install version 2.38 of the modpack
   - Wait for the download and installation to complete

3. **Connect to the Server:**
   - Launch Minecraft through CurseForge
   - Go to Multiplayer
   - Click "Add Server"
   - Enter `mc.glicio.dev` as the server address
   - Click "Done" and join the server

### Server Availability

Please note that this is a self-hosted server running on dedicated hardware with the following specifications:
- CPU: Core i7 processor
- RAM: 32GB
- Operating Hours: The server is online during active play sessions

Due to the self-hosted nature of the server, it may not be available 24/7. For real-time server status updates and to coordinate with other players, we recommend joining our [Discord community](https://discord.gg/EMjmRh5g).