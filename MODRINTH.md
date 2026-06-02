<div align="center">
  <img src="https://raw.githubusercontent.com/Eligiusmc/EligiusNametag/master/docs-site/src/public/assets/readme.png" alt="EligiusNametag Logo" width="100%" />

  # ⚡ EligiusNametag
  **The ultimate, zero-lag holographic nametag plugin for modern servers.**

  [![Paper API](https://img.shields.io/badge/Paper-1.21--26.1.2+-333333?style=flat-square&logo=paper)](https://papermc.io/)
  [![Folia Compatible](https://img.shields.io/badge/Folia-Compatible-ff5e00?style=flat-square&logo=fastapi)](https://papermc.io/software/folia)
  [![Java 21](https://img.shields.io/badge/Java-21_LTS-007396?style=flat-square&logo=openjdk)](https://adoptium.net/)
</div>

---

## 🛑 Why another nametag plugin?
Most nametag plugins rely on legacy scoreboards (which conflict with your sidebar) or physical armor stands (which destroy your server's TPS). 

**EligiusNametag** takes a revolutionary approach: It uses **Native `TextDisplay` entities** handled purely by the Paper API. 

**What does this mean for you?**
* 📉 **Zero Server Lag:** No physical entities taking up memory.
* 🛡️ **Zero Dependencies:** No fragile ProtocolLib packet injections that break every update.
* 🚀 **Future-Proof:** Runs natively on **1.21 up to 26.1.2+** out of the box.

---

## ✨ Features that stand out

### 🎨 Unlimited Creative Freedom
* **Infinite Lines:** Add as many lines above or below the player's name as you want.
* **MiniMessage & Gradients:** Say goodbye to legacy `&` colors. Enjoy full RGB gradient support! `<gradient:blue:aqua>Admin</gradient>`.
* **ItemsAdder & Oraxen:** Display custom emojis and icons natively right inside the nametag.

### 👑 Hierarchy & Permissions
- 👑 **Vault Hierarchy Integration:** Automatically assigns nametag formats based on the user's primary Vault rank (e.g., admin, vip, default).
- 🐾 **Holographic Pets:** Tamed wolves, cats, and parrots inherit their owner's rank formatting!
- 🖼️ **ItemsAdder Ready:** Fully supports font-images and custom emojis like `:rank_dev:` rendering flawlessly above players' heads.
- 🌐 **Global i18n:** In-game messages are completely customizable via language files with hot-swapping support.
- 📊 **bStats Integration:** Anonymous usage metrics help guide the plugin's development. Opt-out anytime in your global bStats config.

### ⚡ Enterprise Scalability
* **Folia Ready:** Built completely asynchronous.
* **Multi-Server Database:** Connect multiple servers using our highly optimized **HikariCP MySQL/MariaDB** adapter, or keep it lightweight with SQLite.

---

## 🚀 What's New in v1.3.0
* **Tactical Invisibility Engine:** Eliminated the native Minecraft "fly-in" visual interpolation bug! Now, even if you are flying with Elytras at lightspeed, your nametag will spawn instantly above your head without dragging from behind.
* **Zero Ghost Entities:** Fixed a critical PaperMC memory leak with non-persistent `TextDisplays` by implementing explicit chunk unload garbage collection.
* **Smart UI Feedback:** The `/enametag me` command now features Minecraft XP Orb audio cues.
* **Robust Encoding:** Full UTF-8 support across 6 global languages out of the box, with an improved in-game update notifier.

---

## 📚 Multi-Language Wiki
We believe in accessible documentation. Our Official Wiki is fully translated into **6 languages** (English, Spanish, French, German, Portuguese, and Russian) so your entire administration team can configure it without friction.

<div align="center">

[![Read the Wiki](https://img.shields.io/badge/Read_The_Official_Wiki-blueviolet?style=for-the-badge&logo=gitbook)](https://eligiusmc.github.io/EligiusNametag/)
[![Report an Issue](https://img.shields.io/badge/Report_An_Issue-black?style=for-the-badge&logo=github)](https://github.com/Eligiusmc/EligiusNametag/issues)
[![Join Discord](https://img.shields.io/badge/Join_Our_Discord-5865F2?style=for-the-badge&logo=discord)](https://discord.gg/8NAW2M7KGq)

</div>

---
**Requirements:** Paper or Folia (1.21 - 26.1.2+) running Java 21 LTS. No third-party APIs required!
