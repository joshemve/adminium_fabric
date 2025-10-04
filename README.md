# Adminium Mod for Forge 1.20.1

A comprehensive server administration mod for Minecraft Forge 1.20.1, providing server operators with powerful tools to manage gameplay and server state.

## Features

### 1. PvP Management
Control Player vs. Player combat across the entire server.
- **Command**: `/pvp <on|off>`
- **Permission**: Requires operator level 2
- Prevents all PvP damage when disabled

### 2. Server Freeze
Freeze the entire server, preventing non-operator players and all entities from moving or interacting.
- **Command**: `/freeze <on|off>`
- **Permission**: Requires operator level 2
- Freezes all mobs, animals, and projectiles
- Disables AI for all entities
- Operators can still move and interact normally

### 3. Safe Mode
Protect specific players from death by enabling safe mode.
- **Command**: `/safe <on|off> [player]`
- **Permission**: Requires operator level 2
- Players in safe mode cannot die - health stops at 0.5 hearts
- Can be toggled for self or other players

### 4. Team System
A comprehensive team management system with colors and prefixes.
- **Commands**:
  - `/team create <name>` - Create a new team
  - `/team invite <player>` - Invite a player to your team (team op required)
  - `/team join <name>` - Accept a team invitation
  - `/team leave` - Leave your current team
  - `/team remove <player>` - Remove a player from the team (team op required)
  - `/team op <player>` - Promote a member to team operator (team op required)
  - `/team color <color>` - Set team color (team op required)
- Team names and colors are displayed in the tab list

### 5. Disabled Items Management
Prevent specific items from being crafted.
- Items can be disabled via configuration
- Disabled items list is saved in `config/adminium_disabled_items.json`
- **Note**: GUI implementation is pending

## Installation

1. Download the latest release from the [Releases](https://github.com/joshemve/adminium_forge/releases) page
2. Place the `.jar` file in your `mods` folder
3. Ensure you have Minecraft Forge 1.20.1 (version 47.4.0 or higher) installed
4. Launch the game

## Building from Source

1. Clone the repository:
   ```bash
   git clone https://github.com/joshemve/adminium_forge.git
   cd adminium_forge
   ```

2. Build the mod:
   ```bash
   ./gradlew build
   ```

3. The built jar will be in `build/libs/`

## Configuration

- Team data is stored in `config/adminium_teams.json`
- Disabled items are stored in `config/adminium_disabled_items.json`

## Requirements

- Minecraft 1.20.1
- Minecraft Forge 47.4.0 or higher

## License

All Rights Reserved

## Contributing

Issues and pull requests are welcome! Please ensure your code follows the existing style and includes appropriate documentation.

## Known Issues

- The item management GUI (`/items` command) is not yet implemented
- Proper client-server networking for the GUI needs to be added 