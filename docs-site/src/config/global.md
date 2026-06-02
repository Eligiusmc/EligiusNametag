---
title: Global Configuration
description: Learn how to configure EligiusNametag globally via config.yml.
---

# Global Configuration (config.yml)

The `config.yml` file is the brain of the plugin. It controls how the plugin interacts with your server on a macro level, affecting all players equally.

---

## 🛠️ Interface Variables

```yaml
# Hologram height above the player's head
# Recommended: 0.35 for humans, 0.50 if using skins with large hats.
y_offset: 0.35

# Maximum view distance in blocks
# Lower values can improve client performance.
view_distance: 64

# Main command aliases
# All of these will execute the help menu or nested commands.
command_aliases:
  - "enametag"
```

---

## 💾 Database Connection (HikariCP)

Unlike basic plugins that save preferences in text files, EligiusNametag allows seamless concurrent flow using the industry standard `HikariCP` to prevent performance drops (Lag Spikes) during massive saves on huge Folia networks.

### 🔹 Option A: SQLite (Local)
Ideal for single servers (isolated Survival, Skyblock). Requires no external configuration. A `database.db` file will automatically be created in the plugin folder.

```yaml
database:
  type: "SQLITE" 
```

### 🔹 Option B: MySQL (Velocity/Bungee Networks)
Ideal if you own multiple connected servers and want the `/enametag me` command preference (show or hide your nametag) to travel with the player from one server to another.

```yaml
database:
  type: "mysql"
  host: "127.0.0.1"
  port: 3306
  database: "eligius_network"
  username: "admin"
  password: "super_secure_password"
```

::: warning ⚠️ IMPORTANT REGARDING MYSQL
Ensure that the `eligius_network` database already exists on your MySQL/MariaDB server before turning on the plugin. The plugin will create the tables internally, but cannot create the root database for security reasons.
:::

---

## 🔴 Redis Pub/Sub Synchronization

If you are running a multi-server proxy network (BungeeCord, Velocity), MySQL polling is not enough for real-time visual updates. By enabling Redis, if a player hides their nametag in the Lobby, the change will instantly replicate to the Survival server in under 5 milliseconds.

```yaml
redis:
  enabled: true
  host: "127.0.0.1"
  port: 6379
  password: "my_secure_redis_password"
```
