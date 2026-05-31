---
title: Holographic Pets
description: How to configure pet nametags in pets.yml.
---

# Holographic Pets (pets.yml)

The pet system is one of the most innovative features of EligiusNametag. With it, any wolf, parrot, or cat tamed by a player will receive a floating holographic nametag that smoothly follows the pet.

### Base Configuration

```yaml
pets:
  enabled: true
  show_unnamed: false 
```

- If `show_unnamed` is `true`, even a generic tamed wolf will have a text hologram. 
- If set to `false`, only pets that the player renames using an **Anvil** (vanilla Name Tag) will activate the holographic system to preserve a more Vanilla style on the general server.

### Line Design
Use `<DISPLAYNAME>` to show the name the player gave it in the anvil, and `<PLAYER>` to quote the owner.

```yaml
  default_format:
    - "<gray><PLAYER>'s Pet</gray>"
    - "<white><DISPLAYNAME></white>"
```

### 🐾 The Biggest Secret!
Pets **inherit** their owner's Vault rank design.
This means that if a cat's owner has the "Admin" rank in `players.yml`, and the Admin rank has red text and a fire icon, **the cat will also show the red text and fire icon** in its group variable (if you configure it this way by crossing PAPI variables or LuckPerms permissions at the `<PLAYER>` node level).
