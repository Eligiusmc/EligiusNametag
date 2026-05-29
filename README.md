# EligiusNametag

A packet-based holographic nametag engine for **Paper 1.21+** servers. Replaces Vanilla nametags with fully customizable, multi-line `TEXT_DISPLAY` entities rendered via **ProtocolLib**. Supports players and tamed mobs, ItemsAdder font images, PlaceholderAPI, Vault groups, and persistent player preferences via SQLite.

---

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [File Structure](#file-structure)
- [Dependencies](#dependencies)
- [Configuration Reference](#configuration-reference)
- [Commands & Permissions](#commands--permissions)
- [Database](#database)
- [Technical Details](#technical-details)
- [Building](#building)
- [Installation](#installation)

---

## Features

- **Multi-line holographic nametags** for players and tamed mobs using `TEXT_DISPLAY` entities.
- **Vault group overrides**: Different nametag formats per permission group (admin, vip, etc.).
- **PlaceholderAPI**: Full placeholder support in nametag lines (e.g. `%luckperms_prefix%`).
- **ItemsAdder font images**: Regex scanner converts `:emoji_name:` → `%img_emoji_name%` → rendered glyph.
- **MiniMessage formatting**: Native support for `<red>`, `<bold>`, `<gradient>`, etc.
- **Legacy color codes**: Automatic conversion of `&c`, `&l`, etc. to MiniMessage tags.
- **Dynamic y_offset**: Hologram height is configurable and updates live on `/eltag reload`.
- **YAML list formats**: Each nametag line is a separate YAML list item for readability. Legacy `\n` strings are also supported via dual-mode parsing.
- **Self-view toggle**: Players can toggle visibility of their own nametag with `/eltag me`.
- **SQLite persistence**: Self-view preferences survive server restarts. Future-ready for MySQL migration.
- **Command aliases**: The base command and its aliases are fully configurable from `config.yml`.
- **Tamed mob nametags**: Holographic nametags for wolves, cats, parrots, etc. with owner-based Vault group overrides.
- **Automatic Vanilla suppression**: `setCustomNameVisible(false)` is called on entities receiving a hologram to prevent text overlap.

---

## Architecture

The plugin follows **Hexagonal Architecture** (Ports & Adapters):

```
┌──────────────────────────────────────────────────┐
│                  DOMAIN LAYER                    │
│                                                  │
│  ┌──────────────┐    ┌────────────────────────┐  │
│  │  ConfigPort   │    │  NametagRendererPort   │  │
│  │  DatabasePort │    │  PlatformPort          │  │
│  └──────┬───────┘    └────────┬───────────────┘  │
│         │     NametagService  │                   │
│         └─────────┬───────────┘                   │
└───────────────────┼──────────────────────────────┘
                    │
┌───────────────────┼──────────────────────────────┐
│              ADAPTER LAYER                       │
│                                                  │
│  YamlConfigAdapter    ProtocolLibNametagRenderer  │
│  DatabaseAdapter      PaperPlatformAdapter        │
└──────────────────────────────────────────────────┘
```

### Domain Layer (`domain/`)
- **`port/ConfigPort.java`** — Interface for configuration access. Returns `List<String>` for nametag templates, `double` for y_offset/lineSpacing/interval, `List<String>` for command aliases, and `String` for messages.
- **`port/DatabasePort.java`** — Interface for persistent player data (SQLite). Methods: `initialize()`, `close()`, `getPlayerViewSelf(UUID)`, `setPlayerViewSelf(UUID, boolean)`, `getAllPlayersWithViewSelf()`.
- **`port/NametagRendererPort.java`** — Interface for the packet-based renderer. `renderNametag(UUID, List<String>, List<UUID>, float yOffset)`, `hideNametag()`, `destroyAll()`.
- **`port/PlatformPort.java`** — Interface for Bukkit/Paper API calls. Player lists, world checks, permission checks, placeholder parsing, Vault group resolution, Vanilla nametag suppression.
- **`service/NametagService.java`** — Core orchestrator. Runs every `interval` seconds. For each online player and tamed mob, determines valid/invalid viewers, resolves templates per Vault group, parses placeholders per-line, and delegates rendering. Manages the `selfViewers` in-memory cache (backed by SQLite).

### Adapter Layer (`adapter/`)
- **`config/YamlConfigAdapter.java`** — Reads `config.yml` with Bukkit's `YamlConfiguration`. Supports **dual-mode template parsing**: if a path is a YAML list, returns it directly; if it's a legacy string with `\n`, splits it into a list. Handles auto-migration from older config versions (< 6).
- **`database/DatabaseAdapter.java`** — JDBC connection to a local SQLite file (`database.db`). Creates `player_settings` table on init. Uses `ON CONFLICT` upsert for thread-safe writes. All DB writes from `NametagService` are dispatched asynchronously via `Bukkit.getScheduler().runTaskAsynchronously()`.
- **`renderer/ProtocolLibNametagRenderer.java`** — The packet engine. Spawns virtual `TEXT_DISPLAY` entities (IDs from `Integer.MAX_VALUE/2` downward) and mounts them as passengers on the target entity. Tracks which viewers have received spawn packets via `lineSpawnedViewers`. Mount packets are sent **every cycle** (not just on first spawn) to handle chunk-loading race conditions on reconnect. Supports dynamic `yOffset` passed per render call.
- **`platform/PaperPlatformAdapter.java`** — Bukkit/Paper implementation. Resolves Vault groups via `net.milkbowl.vault.permission.Permission`. Parses `PlaceholderAPI` placeholders. Includes an **ItemsAdder regex scanner** that converts `:name:` patterns to `%img_name%` PAPI placeholders, resolving them to font image glyphs. Handles `disableVanillaNametag()` and `hasPermission()`.

### Entry Points
- **`EligiusNametag.java`** — Main plugin class. Initializes all adapters, registers the Brigadier command tree, starts the repeating update task, and registers a `PlayerQuitEvent` listener that calls `clearViewer()` on the renderer to ensure fresh spawn packets on reconnect.
- **`EligiusNametagCommand.java`** — Brigadier command registration. Reads `command_aliases` from config and registers them all. Sub-commands: `reload` (admin), `me` (self-view toggle with MiniMessage feedback).

---

## File Structure

```
src/main/
├── java/com/makrozai/eligiusnametag/
│   ├── EligiusNametag.java              # Plugin main class + PlayerQuitEvent listener
│   ├── EligiusNametagCommand.java       # Brigadier commands (reload, me, aliases)
│   ├── adapter/
│   │   ├── config/
│   │   │   └── YamlConfigAdapter.java   # YAML config reader (dual list/string mode)
│   │   ├── database/
│   │   │   └── DatabaseAdapter.java     # SQLite JDBC adapter
│   │   ├── platform/
│   │   │   └── PaperPlatformAdapter.java # Bukkit API, Vault, PAPI, ItemsAdder regex
│   │   └── renderer/
│   │       └── ProtocolLibNametagRenderer.java # Packet engine (TEXT_DISPLAY + mount)
│   └── domain/
│       ├── port/
│       │   ├── ConfigPort.java          # Config interface
│       │   ├── DatabasePort.java        # Database interface
│       │   ├── NametagRendererPort.java  # Renderer interface
│       │   └── PlatformPort.java        # Platform interface
│       └── service/
│           └── NametagService.java      # Core logic + selfViewers cache
└── resources/
    ├── config.yml                       # Default configuration (V6)
    └── paper-plugin.yml                 # Paper plugin descriptor
```

---

## Dependencies

| Plugin | Required | Purpose |
|---|---|---|
| **ProtocolLib** | ✅ Yes | Packet-level entity spawning (TEXT_DISPLAY) |
| **PlaceholderAPI** | ❌ Optional | Placeholder resolution (`%luckperms_prefix%`, etc.) |
| **Vault** | ❌ Optional | Permission group resolution for format overrides |
| **ItemsAdder** | ❌ Optional | Font image glyphs (`:rank_dev:` → rendered icon) |
| **LuckPerms** | ❌ Optional | Recommended permissions plugin (works with Vault bridge) |

**Server Requirements:**
- Paper 1.21+ (uses Brigadier command API and `TEXT_DISPLAY` entity type)
- Java 21+

---

## Configuration Reference

**File:** `plugins/EligiusNametag/config.yml` (auto-generated on first run)

```yaml
config_version: 6

# Update interval in seconds (0.5 = every 10 ticks)
interval: 0.5

# Max distance in blocks at which nametags are visible
view_distance: 64

# Vertical spacing between hologram lines
line_spacing: 0.275

# Base height offset above the entity's head (updates live on reload)
y_offset: 0.35

# Command aliases (all point to the same command tree)
command_aliases:
  - "eltag"
  - "enametag"

# Translatable messages (MiniMessage format)
messages:
  toggled_self_on: "<green>You can now see your own nametag."
  toggled_self_off: "<red>You can no longer see your own nametag."
  no_permission: "<red>You don't have permission."

# Player nametag formats (YAML lists, one line per list item)
players:
  default_format:
    - "<yellow>Player</yellow>"
    - "<white><PLAYER></white>"
  groups:
    admin:
      - "<red>:rank_dev: Admin</red>"
      - "<white><PLAYER></white>"

# Tamed mob nametag formats
pets:
  enabled: true
  show_unnamed: false    # false = only mobs with a Name Tag item get holograms
  default_format:
    - "<gray>Pet of <PLAYER></gray>"
    - "<white><DISPLAYNAME></white>"
  groups:
    admin:
      - "<red>Guardian Pet of <PLAYER></red>"
      - "<white><DISPLAYNAME></white>"
```

### Template Variables

| Variable | Context | Description |
|---|---|---|
| `<PLAYER>` | Players & Pets | Player name (for pets: owner name) |
| `<DISPLAYNAME>` | Pets only | Custom name from Name Tag item, or species name |
| `:emoji_name:` | Both | ItemsAdder font image (auto-converted to `%img_emoji_name%`) |
| `%placeholder%` | Both | Any PlaceholderAPI placeholder (resolved against the player/owner) |

### Format Modes (Dual Support)
The config supports both formats:

**YAML List (recommended):**
```yaml
default_format:
  - "<red>Line 1</red>"
  - "<white>Line 2</white>"
```

**Legacy String (backwards compatible):**
```yaml
default_format: "<red>Line 1</red>\n<white>Line 2</white>"
```

---

## Commands & Permissions

### Commands

| Command | Description | Permission |
|---|---|---|
| `/eltag` | Shows plugin version | None |
| `/eltag reload` | Reloads config (y_offset updates live) | `eligiusnametag.admin` |
| `/eltag me` | Toggles self-view of own nametag | `eligiusnametag.viewself` |

All aliases defined in `command_aliases` work identically (e.g. `/enametag reload`).

### Permissions

| Permission | Default | Description |
|---|---|---|
| `eligiusnametag.admin` | OP | Access to reload command |
| `eligiusnametag.viewself` | false | Ability to use `/eltag me` |

---

## Database

**Engine:** SQLite (via JDBC, bundled in Paper's JVM)  
**File:** `plugins/EligiusNametag/database.db` (auto-created)

### Schema
```sql
CREATE TABLE IF NOT EXISTS player_settings (
    uuid VARCHAR(36) PRIMARY KEY,
    view_self BOOLEAN NOT NULL
);
```

- Records are created when a player first uses `/eltag me`.
- All database writes are **asynchronous** (`runTaskAsynchronously`).
- On startup, all records with `view_self = 1` are loaded into an in-memory `ConcurrentHashMap.newKeySet()` cache for zero-latency lookups during the render loop.
- **Future MySQL migration:** The architecture uses a `DatabasePort` interface, so swapping SQLite for MySQL requires only a new adapter implementation.

---

## Technical Details

### Rendering Pipeline
1. Every `interval` seconds, `NametagService.updateAllNametags()` iterates all online players and tamed mobs.
2. For each target, it determines which viewers should see the nametag (same world, not hidden, visibility checks).
3. Self-view is checked against the `selfViewers` in-memory cache (not DB on every tick).
4. Templates are fetched from config as `List<String>` (Vault group override → default fallback).
5. Each line is parsed through `PaperPlatformAdapter.parsePlaceholders()`:
   - `<PLAYER>` and `<DISPLAYNAME>` are replaced.
   - PlaceholderAPI resolves `%...%` placeholders.
   - ItemsAdder regex scanner converts `:name:` → `%img_name%` → glyph.
6. `ProtocolLibNametagRenderer.renderNametag()` spawns/updates virtual `TEXT_DISPLAY` entities:
   - **Spawn packet** (only on first encounter per viewer).
   - **Metadata packet** (every cycle): billboard mode, view distance, translation offset, text component.
   - **Mount packet** (every cycle): ensures the text display stays mounted on the target, even after chunk reload.
7. When a viewer should NOT see a nametag, `hideNametag()` sends a destroy packet.

### Reconnect Handling
- `PlayerQuitEvent` → `clearViewer(uuid)` removes the player from all spawn tracking maps.
- On reconnect, fresh spawn packets are sent. Mount packets are sent every cycle to handle the race condition where the client hasn't loaded the target entity's chunk yet.

### Nametag Suppression
- When a hologram is rendered on an entity, `setCustomNameVisible(false)` is called to hide the Vanilla nametag and prevent text overlap.

### Entity ID Management
- Virtual entity IDs count downward from `Integer.MAX_VALUE / 2` to avoid collisions with real server entities.
- Each target entity has a list of virtual IDs in `activeEntities` (one per hologram line).

---

## Building

**Requirements:** Java 21 JDK

```bash
# Using the Gradle wrapper
./gradlew build

# Windows with local JDK
.\build_with_java21.bat
```

Output JAR: `build/libs/EligiusNametag-<version>.jar`

---

## Installation

1. Place the JAR in your server's `plugins/` folder.
2. Ensure **ProtocolLib** is installed.
3. Start the server. The plugin will generate:
   - `plugins/EligiusNametag/config.yml` — Main configuration
   - `plugins/EligiusNametag/database.db` — SQLite database (auto-created)
4. Edit `config.yml` to customize nametag formats.
5. Use `/eltag reload` to apply changes (y_offset updates live).

### Installed File Structure
```
plugins/EligiusNametag/
├── config.yml       # User-editable configuration
├── config.old.yml   # Backup from auto-migration (if applicable)
├── database.db      # SQLite binary (DO NOT edit manually)
└── #defaults/       # Internal reference copy of default config
```

---

## License

MIT License — see [LICENSE](LICENSE) for details.