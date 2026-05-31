---
title: Troubleshooting
description: Common issues and solutions when using EligiusNametag.
---

# Troubleshooting

Having issues with EligiusNametag? Check these common solutions:

### 1. The Nametag doesn't appear above the player
- **Do you have PlaceholderAPI installed?** If you use variables like `%vault_rank%` and don't have PAPI, the text might fail to render.

### 2. "Database Connection Closed" Error
- This has been **fixed in recent versions**. Ensure you are running the latest version `v1.x.x` or higher. If you use **MySQL**, verify that the IP and credentials in `config.yml` are correct. Otherwise, use local `SQLITE`.

### 3. ViaVersion Warnings in Console
If you use ViaVersion to allow players on 1.20 to join your 1.21 - 26.1.2+ server, you might see messages about *Metadata* and *Protocol Versions*.
- **Cause:** The old client (e.g., 1.20.2) lacks the native code to properly process large `TEXT_DISPLAY` entities.
- **Solution:** We highly recommend requiring 1.21 - 26.1.2+ versions for perfect hologram visualization, or simply ignore the warning if the player experiences no visual crashes.

### 4. /enametag lang doesn't update the help message
When using `/enametag lang en`, the plugin reads from `lang/en.yml`. If you notice a translation is empty or a command acts weird, you can delete the `lang/` folder in your plugins directory so EligiusNametag can regenerate it cleanly with the latest standards.
