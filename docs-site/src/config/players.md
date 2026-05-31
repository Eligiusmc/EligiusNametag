---
title: Player Designs
description: Configure custom MiniMessage and Vault designs in players.yml.
---

# Player Designs (players.yml)

This is where the visual magic happens. The `players.yml` file allows you to define **unlimited groups** using the modern *MiniMessage* engine, interpolated with the versatility of *PlaceholderAPI*.

---

## 🎨 Understanding MiniMessage

Instead of using old and limited color codes like `&c` or `&l`, you can now use highly descriptive HTML tags.

| Tag | Result |
|-----|--------|
| `<red>` | Pure red text |
| `<#ff00ff>` | Text using custom HEX code |
| `<bold>` | **Bold** text |
| `<gradient:red:blue>` | Generates a smooth transition between two or more colors |

---

## 👥 Default Format

Any user who does not fit into a rank hierarchy will fall into this design:

```yaml
players:
  default_format:
    - "<yellow>Player</yellow>"
    - "<white><PLAYER></white>"
```
*You will notice that you can create as many lines as you want simply by adding hyphens (`-`) to the list.*

---

## 👑 Advanced Ranks & Vault

You can create nametag groups that exactly match your rank name in **LuckPerms** (or any other Vault-compatible permission system). The plugin reads the player's *Primary Group* and looks for its equivalent here.

```yaml
  groups:
    vip:
      - "<gradient:gold:yellow>⭐ VIP User</gradient>"
      - "<white><PLAYER></white>"
    admin:
      - "<red>:rank_dev: Supreme Admin</red>"
      - "<gradient:red:dark_red><bold><PLAYER></bold></gradient>"
```

### 🖼️ Integration with ItemsAdder / Oraxen

If you look closely at the `admin` format, you'll notice the text `:rank_dev:`. 

If you have **ItemsAdder** or any Resource Pack installed on your server, the player's client will read this and automatically replace the colons with an **actual font image** rendered above the admin's head.

::: tip 💡 DESIGN TIP
Avoid making Nametags more than 3 lines long for regular players. Excessive lines can obstruct vision in PvP or dense builds.
:::
