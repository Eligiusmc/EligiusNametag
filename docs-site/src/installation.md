---
title: Installation & Requirements
description: Learn how to install EligiusNametag on your Paper or Folia server.
---

# Installation & Requirements

EligiusNametag is a modern and powerful plugin. By leveraging the latest features of the Minecraft Text API and multi-threading, it requires an updated environment.

## 📋 System Requirements

| Requirement | Minimum Version | Reason |
|-------------|-----------------|--------|
| **Platform** | Paper 1.21 or Folia 1.21+ | Strict use of `TEXT_DISPLAY` components and Brigadier commands. |
| **Java** | Java 21 LTS | Source code compiled in JDK 21 format. |

## 📦 Dependencies

You must have the following plugins installed in your `plugins/` folder:

1. **ProtocolLib** *(Required)*: Used to send hologram packets invisibly without consuming physical TPS.
2. **Vault** *(Optional)*: If you want to grant specific nametag designs depending on the user's rank (Admin, VIP, etc.).
3. **PlaceholderAPI** *(Optional)*: To parse external variables such as stats, money, or clans in the nametags.
4. **ItemsAdder** *(Optional)*: To display custom emojis in names.

## 🚀 Installation Steps

1. Download the `EligiusNametag-1.x.x.jar` file from the **Releases** tab.
2. Copy the file into your server's `plugins/` directory.
3. Make sure you have **ProtocolLib** installed.
4. Start your server.
5. You will see the startup animation in the console with the Eligius MC cat confirming the plugin has hooked successfully.
6. Done! You can configure everything from `plugins/EligiusNametag/`.
