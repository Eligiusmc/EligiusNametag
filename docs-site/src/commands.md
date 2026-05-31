---
title: Commands & Permissions
description: View all available commands and permissions for EligiusNametag.
---

# Commands & Permissions

All default commands are based on `/enametag` (or its customizable aliases `/eligiusnametag`).

| Command | Description | Required Permission |
|---------|-------------|---------------------|
| `/enametag` | Displays the installed plugin version and author logo. | *None* |
| `/enametag help` | Displays the help menu with all available commands. | `eligiusnametag.admin` |
| `/enametag reload` | Reloads all configuration and YAML files in real-time, without restarting the server. | `eligiusnametag.admin` |
| `/enametag lang <language>` | Dynamically changes the global plugin language and reloads languages instantly. | `eligiusnametag.admin` |
| `/enametag pets` | Globally toggles the display of text holograms above pets. | `eligiusnametag.admin` |
| `/enametag me` | Personally toggles the display of your own nametag (Auto-saved to DB). | `eligiusnametag.viewself` |

## Additional Permission Details

- `eligiusnametag.*`: Grants full access to all administration commands.
- *Note:* If you use LuckPerms, don't forget to assign the main permission to your *default* or *admin* group so that formatting variables sync correctly.
