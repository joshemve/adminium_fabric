# Adminium Mod Functionality

This document outlines the features and functionality of the Adminium mod. The goal is to recreate this mod for **Forge 1.20.1 (47.4.0)**. The existing codebase is for NeoForge, so some classes and methods will need to be ported.

## Overview

Adminium is a server administration mod designed to give server operators fine-grained control over various aspects of gameplay. It includes features for managing PvP, freezing the server state, protecting players from death, a comprehensive team system, and the ability to disable specific items.

---

## Features

### 1. PvP Management

*   **Functionality**: Allows server operators to enable or disable Player vs. Player (PvP) combat across the entire server.
*   **Command**: `/pvp <on|off>`
    *   Requires permission level 2 (op).
    *   `/pvp on`: Enables PvP.
    *   `/pvp off`: Disables PvP.
*   **Implementation**:
    *   `command/PvpCommand.java`: Registers and handles the `/pvp` command.
    *   `manager/PvpManager.java`: A simple class holding a static boolean `pvpEnabled`.
    *   `Adminium.java`: The `onAttackEntity` event handler in the `ForgeEvents` inner class checks `PvpManager.isPvpEnabled()`. If disabled, it cancels the `AttackEntityEvent` when a player attacks another player.

### 2. Server Freeze

*   **Functionality**: Allows operators to "freeze" the server. When frozen, non-operator players and all non-player entities are prevented from moving or interacting with the world.
*   **Command**: `/freeze <on|off>`
    *   Requires permission level 2 (op).
    *   `/freeze on`: Freezes the server and broadcasts a message.
    *   `/freeze off`: Unfreezes the server and broadcasts a message.
*   **Features**:
    *   **Enhanced Server Compatibility**: Improved freeze system that works reliably on dedicated servers.
    *   **Position Locking**: Stores and enforces frozen positions to prevent any movement.
    *   **Client Synchronization**: Regular position sync packets prevent client-side desync issues.
    *   **Comprehensive Movement Prevention**: Blocks all forms of player movement including impulse and hurt knockback.
*   **Implementation**:
    *   `command/FreezeCommand.java`: Registers and handles the `/freeze` command.
    *   `manager/FreezeManager.java`: Enhanced manager with position storage and tracking for frozen players.
    *   `Adminium.java`: Multiple event handlers ensure complete freeze functionality including position enforcement and client sync.

### 3. Safe Mode

*   **Functionality**: A per-player "safe mode" that prevents them from dying. If a player in safe mode takes fatal damage, their health is set to half a heart instead.
*   **Command**: `/safe <on|off> [player]`
    *   Requires permission level 2 (op).
    *   Toggles safe mode for the executing operator or a specified player.
*   **Implementation**:
    *   `command/SafeCommand.java`: Registers and handles the `/safe` command.
    *   `manager/SafeManager.java`: Manages a `Set` of UUIDs for players currently in safe mode.
    *   `Adminium.java`: The `onLivingIncomingDamage` event in `ForgeEvents` checks if the player is in the safe set. If so, it intercepts fatal damage and prevents the player's death.

### 4. Team System

*   **Functionality**: A comprehensive team system allowing players to form groups. Teams have names, colors, members, and operators. Team names and colors are displayed as a prefix in the player tab list.
*   **Commands**:
    *   `/ateam create <name>`: Creates a team.
    *   `/ateam invite <player>`: Invites a player to a team (team op required).
    *   `/ateam join <name>`: Accepts a team invite.
    *   `/ateam leave`: Leaves the current team.
    *   `/ateam remove <player>`: Kicks a player from a team (team op required).
    *   `/ateam op <player>`: Promotes a team member to operator (team op required).
    *   `/ateam color <color>`: Sets the team color (team op required).
    *   `/ateam list`: Lists all teams and their members.
    *   `/tm <message>`: Sends a message to all team members.
*   **Team Messaging**:
    *   Messages are prefixed with "[TEAM]" and the team's colored name.
    *   Only team members can see team messages.
    *   Available to all team members (no special permissions required).
*   **Implementation**:
    *   `command/TeamCommand.java`: Registers and handles all subcommands for `/ateam` (renamed from `/team` to avoid conflicts).
    *   `command/TeamMsgCommand.java`: Handles team messaging functionality.
    *   `team/Team.java`: A data class representing a single team, its members, operators, and color.
    *   `team/TeamManager.java`: Handles all the logic for creating, managing, and interacting with teams, including invitations.
    *   `Adminium.java`: The `onTabListNameFormat` event in `ForgeEvents` adds the team prefix to players' names in the tab list.

### 5. Portal Control

*   **Functionality**: Allows operators to disable nether and end portals server-wide.
*   **Commands**:
    *   `/nether <on|off>`: Enables or disables nether portal access.
    *   `/end <on|off>`: Enables or disables end portal access.
*   **Implementation**:
    *   `command/NetherCommand.java` and `command/EndCommand.java`: Handle the respective commands.
    *   `manager/PortalManager.java`: Manages portal states.
    *   `Adminium.java`: The `onEntityTravelToDimension` event blocks dimension travel when portals are disabled.

### 6. Announcement System

*   **Functionality**: Allows operators to display announcements as titles/subtitles on all players' screens.
*   **Commands**:
    *   `/announce "title" "description"`: Shows white text announcement.
    *   `/announce <color> "title" <color> "description"`: Shows colored announcement.
*   **Implementation**:
    *   `command/AnnounceCommand.java`: Parses colors and sends title packets to all players.

### 7. Role System

*   **Functionality**: Assigns roles (Fighter, Farmer, Miner) to players with custom icons displayed in chat.
*   **Commands**:
    *   `/roles`: Opens the role management GUI.
*   **GUI Features**:
    *   Edit role icons (select any item including modded items).
    *   Remove all roles button.
    *   Auto-assign button (30% fighters, 35% farmers, 35% miners).
*   **Chat Integration**:
    *   Role icons appear in chat messages as Unicode symbols only (no text).
    *   Symbols are chosen based on the selected item type (e.g., ⚔ for swords, ⛏ for pickaxes).
    *   Hovering over the symbol shows the full item tooltip.
    *   Format: `[Symbol] [Team] PlayerName: message`
*   **Implementation**:
    *   `roles/Role.java`: Enum defining roles with colors and icon management.
    *   `roles/RoleManager.java`: Manages role assignments and persistence.
    *   `client/gui/RolesScreen.java`: Main GUI for role management.
    *   `client/gui/ItemSelectorScreen.java`: Item selection interface.
    *   `network/`: Packets for client-server synchronization.
    *   `Adminium.java`: Event handlers restore role display names on player login and respawn.

### 8. Disabled Items Management

*   **Functionality**: Allows for disabling certain items, preventing them from being crafted. A GUI is intended for managing this list.
*   **Commands**: 
    *   `/items`: Opens the item management GUI.
*   **Implementation**:
    *   `manager/DisabledItemsManager.java`:
        *   Manages a `Set` of `ResourceLocation`s for disabled items.
        *   Loads/Saves this list from `config/adminium_disabled_items.json`.
    *   `Adminium.java`:
        *   `onServerStarting` event calls `DisabledItemsManager.load()`.
        *   `onItemCrafted` event prevents crafting of disabled items.
        *   `onPlayerLoggedIn` event removes recipes for disabled items.
    *   `client/gui/ItemsScreen.java`:
        *   Client-side GUI that displays all items with enable/disable buttons.
        *   Properly networked to send enable/disable requests to the server.

### 9. Vanish System

*   **Functionality**: Allows operators to become completely invisible to non-operator players. Vanished players are hidden from the tab list and cannot be seen or attacked by non-ops.
*   **Commands**:
    *   `/vanish`: Toggles vanish mode for the executing operator.
    *   `/vanish <player>`: Toggles vanish mode for a specified player.
*   **Features**:
    *   Vanished players are completely invisible to non-operators.
    *   Removed from tab list for non-operators.
    *   Cannot be attacked by non-operators.
    *   Held items, armor, and equipment are hidden from non-operators.
    *   Equipment changes while vanished remain hidden from non-operators.
    *   Operators can see all players regardless of vanish state.
    *   Proper handling of player join/leave events.
    *   Real-time equipment hiding when players change items while vanished.
    *   **Fixed Unvanish Bug**: Properly restores player visibility when turning vanish off (no more relog required).
    *   **Complete Player Resync**: Forces full player synchronization to prevent visibility issues.
*   **Implementation**:
    *   `command/VanishCommand.java`: Registers and handles the `/vanish` command.
    *   `manager/VanishManager.java`: Manages vanish state with improved visibility restoration and complete player resync functionality.
    *   `Adminium.java`: Event handlers for player join/leave, attack prevention, and equipment changes.

### 10. Inventory Inspection

*   **Functionality**: Allows operators to view other players' inventories and ender chests for administrative purposes.
*   **Commands**:
    *   `/invsee <player>`: Opens a read-only view of the specified player's inventory (36 slots).
    *   `/endchest <player>`: Opens a read-only view of the specified player's ender chest (27 slots).
*   **Features**:
    *   View player inventories in a chest-like interface.
    *   View player ender chests in a standard chest interface.
    *   Read-only access - operators can see but not modify items.
    *   Prevents operators from viewing their own inventory/ender chest with these commands.
    *   Real-time snapshot of player's current inventory state.
*   **Implementation**:
    *   `command/InvseeCommand.java`: Handles the `/invsee` command and creates inventory view.
    *   `command/EndchestCommand.java`: Handles the `/endchest` command and creates ender chest view.
    *   Uses `SimpleContainer` to create read-only copies of player inventories.

### 11. Warp System

*   **Functionality**: Allows operators to create and teleport to named locations across all dimensions for easier server administration.
*   **Commands**:
    *   `/setwarp <name>`: Creates a warp point at the operator's current location.
    *   `/warp [name]`: Teleports to a warp point, or lists all warps if no name is provided.
    *   `/delwarp <name>`: Deletes an existing warp point.
*   **Features**:
    *   Cross-dimensional teleportation (Overworld, Nether, End, and modded dimensions).
    *   Preserves exact position and rotation when teleporting.
    *   Persistent storage - warps survive server restarts.
    *   Tab completion for warp names in `/warp` and `/delwarp` commands.
    *   Warp name validation (alphanumeric, underscores, hyphens only, max 32 characters).
    *   Detailed location information displayed when creating or using warps.
    *   Automatic warp updating when setting a warp with an existing name.
*   **Implementation**:
    *   `manager/WarpManager.java`: Handles warp storage, persistence, and teleportation logic.
    *   `command/SetwarpCommand.java`: Creates new warp points with validation.
    *   `command/WarpCommand.java`: Teleports to warps and lists available warps.
    *   `command/DelwarpCommand.java`: Deletes existing warp points.
    *   Warps are saved to `config/adminium_warps.json` with full location and dimension data.

### 12. Ban Management System

*   **Functionality**: Provides a comprehensive GUI-based ban management system for operators to view and manage banned players with special hardcore mode support.
*   **Commands**:
    *   `/bans`: Opens the ban management GUI interface.
*   **Features**:
    *   Visual ban list with search functionality.
    *   View ban details including player name, reason, date, and who issued the ban.
    *   Pardon players directly from the GUI.
    *   Special hardcore death handling - ability to pardon and revert players from hardcore to survival mode.
    *   Live updates without server restart required.
    *   Persistent ban storage with automatic save/load.
    *   Scrollable interface for managing large numbers of bans.
    *   Real-time ban list refresh capability.
*   **GUI Interface**:
    *   Search box to filter banned players by name or reason.
    *   Scrollable list showing ban entries with details.
    *   "Pardon" button for standard ban removal.
    *   "Pardon & Revert" button for hardcore deaths (automatically sets survival mode on next login).
    *   Refresh button to update the ban list.
    *   Total ban count display.
    *   **Fixed Layout**: Resolved button overlap issues with scroll arrows.
*   **Implementation**:
    *   `manager/BanManager.java`: Handles ban storage, persistence, and hardcore mode reverts.
    *   `command/BansCommand.java`: Opens the ban management GUI.
    *   `client/gui/BanManagementScreen.java`: Provides the visual ban management interface.
    *   `network/`: Packets for client-server communication (RequestBanListPacket, BanListPacket, BanActionPacket).
    *   Bans are saved to `config/adminium_bans.json` with full ban information.

### 13. InstaBan System

*   **Functionality**: Automatically bans players who die in hardcore mode instead of allowing them to enter spectator mode, perfect for simulation events.
*   **Commands**:
    *   `/instaban on`: Enables InstaBan mode - players who die in hardcore are instantly banned.
    *   `/instaban off`: Disables InstaBan mode - players who die use normal hardcore behavior (spectator mode).
    *   `/instaban`: Shows current InstaBan status and world mode.
*   **Features**:
    *   Automatic banning on actual death (not knockdown) in hardcore mode.
    *   **Operator Protection**: Operators (level 2+) are never banned, even when InstaBan is enabled.
    *   **Spectator Mode Prevention**: Non-operators cannot enter spectator mode when InstaBan is enabled.
    *   **Automatic Survival Mode**: Any pardoned player automatically spawns in survival mode.
    *   Immediate disconnection with informative message.
    *   Integration with ban management system (marked as hardcore deaths).
    *   Operator broadcast notifications when toggled.
    *   Status checking with world mode detection.
    *   **PlayerRevive Mod Integration**: Automatically detects and integrates with PlayerRevive mod - players can be knocked down and revived, but are banned only when they actually bleed out.
    *   **Fallback Compatibility**: Works with or without PlayerRevive mod - adapts behavior based on mod presence.
*   **Event Integration**:
    *   Perfect for 100-player simulation events in hardcore mode.
    *   Players who die are immediately removed from the event.
    *   Operators can use the ban management GUI to selectively allow players back.
    *   **Complete Spectator Prevention**: No way for non-operators to bypass into spectator mode.
    *   **Simplified Pardon System**: Single "Pardon" button automatically handles survival mode.
*   **Implementation**:
    *   `manager/InstaBanManager.java`: Handles automatic banning logic with operator protection.
    *   `command/InstaBanCommand.java`: Provides the toggle command with status display.
    *   `manager/PlayerReviveIntegration.java`: Handles PlayerRevive mod integration using reflection for optional compatibility.
    *   `Adminium.java`: Uses `LivingDeathEvent` for death detection and `PlayerBleedOutEvent` (when PlayerRevive is present) for actual deaths.
    *   Integrates with existing BanManager for consistent ban handling.

### 14. Heal Command

*   **Functionality**: Allows operators to restore health and hunger for themselves or other players.
*   **Commands**:
    *   `/heal`: Restores the operator's health and hunger to full.
    *   `/heal <player>`: Restores the specified player's health and hunger to full.
*   **Features**:
    *   Restores health to 20 HP (full health).
    *   Restores hunger to 20 food level with 20 saturation (full hunger).
    *   Works on any player (online or offline players will be healed when they join).
    *   Provides feedback messages to both the operator and the target player.
    *   Operator-only command (requires level 2 permissions).
*   **Implementation**:
    *   `command/HealCommand.java`: Handles the `/heal` command with optional player targeting.
    *   Direct manipulation of player health and food stats.
    *   Immediate effect with confirmation messages.

### 15. God Mode

*   **Functionality**: Allows operators to toggle invulnerability to all damage for themselves or other players.
*   **Commands**:
    *   `/god`: Toggles god mode for the operator (invulnerability on/off).
    *   `/god <player>`: Toggles god mode for the specified player.
*   **Features**:
    *   Complete immunity to all damage sources (fall damage, fire, drowning, player attacks, etc.).
    *   Persistent across server restarts and player reconnections.
    *   Clear feedback messages when toggling god mode on/off.
    *   Works on any online player.
    *   Operator-only command (requires level 2 permissions).
    *   Takes priority over safe mode (god mode = no damage at all, safe mode = prevent death only).
*   **Implementation**:
    *   `command/GodCommand.java`: Handles the `/god` command with optional player targeting.
    *   `manager/GodManager.java`: Manages god mode state with persistent JSON storage.
    *   `Adminium.java`: Integrates with `LivingHurtEvent` to cancel all damage for god mode players.
    *   Saves god mode status to `config/adminium_god_mode.json`.

## Technical Notes

*   **Networking**: All GUI interactions are properly networked between client and server.
*   **Persistence**: Team data, role assignments, and disabled items are saved as JSON files.
*   **Permissions**: All administrative commands require operator level 2.
*   **Compatibility**: Built for Forge 1.20.1 (version 47.4.0).
*   **Unicode Symbols**: Role icons use Unicode symbols for server-side compatibility (no client mod required). 