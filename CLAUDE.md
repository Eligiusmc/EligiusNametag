# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build the plugin jar (output: build/libs/EligiusNametag-<version>.jar)
./gradlew build

# Run tests only
./gradlew test

# Run a single test class
./gradlew test --tests "com.makrozai.eligiusnametag.domain.service.NametagServiceTest"

# Clean build
./gradlew clean build

# Docs site (VitePress)
cd docs-site && npm install && npm run docs:dev
```

**Java 21 LTS is required.** The `gradle.properties` forces `org.gradle.java.home` to a local JDK 21 path — update this path if it differs on your machine.

## Architecture: Hexagonal (Ports & Adapters)

The plugin is strictly structured around ports (interfaces in `domain/port/`) and adapters (implementations in `adapter/`). The domain layer has zero Bukkit imports.

```
domain/port/          ← Interfaces only (ConfigPort, DatabasePort, NametagRendererPort, PlatformPort, SyncPort)
domain/service/       ← NametagService (orchestrator), AnimationManager, UpdateChecker
adapter/config/       ← YamlConfigAdapter    (implements ConfigPort)
adapter/database/     ← DatabaseAdapter      (implements DatabasePort — SQLite/MySQL via HikariCP)
adapter/renderer/     ← BukkitNametagRenderer (implements NametagRendererPort — native TextDisplay API)
adapter/platform/     ← PaperPlatformAdapter, FoliaSchedulerAdapter (implements PlatformPort)
adapter/network/      ← RedisAdapter         (implements SyncPort — Redis Pub/Sub)
```

**Adding new functionality:** Add the contract to the relevant Port interface first, implement it in the adapter, and keep all logic inside the domain service. The main plugin class (`EligiusNametag.java`) is only a wiring class.

## Key Behavioral Details

### TextDisplay Rendering Pipeline
`BukkitNametagRenderer` spawns non-persistent `TextDisplay` entities mounted as passengers on the target entity. Visibility is managed per-player via `player.showEntity()` / `player.hideEntity()` — no packet manipulation.

**Fly-in fix:** New displays are spawned with `setTextOpacity((byte) 0)` and revealed 2 ticks later via `runTaskLater`. Do not remove this delay — it prevents a visible "fly-in" interpolation artifact.

**Memory leak guard:** `renderNametag()` runs `displays.removeIf(display -> !display.isValid() || display.isDead())` at the start of every tick. Non-persistent TextDisplays are silently discarded by Bukkit on chunk unload; without this check they ghost in the maps.

### Folia Compatibility
Folia is detected at runtime via `Class.forName("io.papermc.paper.threadedregions.RegionizedServer")`. This check appears in `EligiusNametag.java`, `NametagService.java`, and `RedisAdapter.java`. When Folia is active, entity-bound work is dispatched to `entity.getScheduler()` and async work to `Bukkit.getAsyncScheduler()`. Do not use `Bukkit.getScheduler()` for new code without wrapping it in the same detection pattern.

### Visibility Check for Mobs
`canViewerSeeTarget` uses a 64-block squared distance check for non-player entities instead of `getTrackedBy()`. This is intentional — Paper's tracking API produced false negatives with ViaVersion.

### Config Parsing
`YamlConfigAdapter` supports two nametag line formats for backward compatibility:
- **List (current):** YAML list of strings
- **String (legacy):** Single string with `\n` or `\\n` separators

Both are normalized in `getTemplateAsList()`.

### Animation Syntax
Templates use `<anim:name>text</anim:name>` tags. `AnimationManager` loads `.yml` files from `plugins/EligiusNametag/animations/` and resolves these at parse time inside `parsePlaceholders`.

### Attribute Compatibility
`PaperPlatformAdapter` resolves `MAX_HEALTH` vs `GENERIC_MAX_HEALTH` via reflection at class-load time to support both 1.21.1 and 1.21.2+ APIs.

## Git Workflow

- **`master`** — production, never push directly, updated by Release Please bot only
- **`develop`** — integration branch, base for all PRs
- **`feature/<name>`** — branch from `develop`, merge back to `develop`

**Conventional Commits are mandatory** — `feat:`, `fix:`, `docs:`, `chore:`, `refactor:`. These drive automated semantic versioning via Release Please. A `BREAKING CHANGE:` footer bumps the major version.

Promotion from `develop` to `master` is done via the **"Promote Develop to Master"** GitHub Action, not a manual PR.

## Wiki (VitePress)

Documentation lives in `docs-site/src/`. Six languages: `src/` (EN), `src/es/`, `src/fr/`, `src/de/`, `src/pt/`, `src/ru/`.

When adding a new wiki page:
1. Create it under `src/` with required frontmatter (`title`, `description`).
2. Copy and translate to all language subdirectories.
3. Register the link in `docs-site/.vitepress/config.mts` under each locale's sidebar.
