<div align="center">
  <img src="docs-site/src/public/assets/readme.png" alt="EligiusNametag Logo" width="100%" />

  # EligiusNametag

  *The ultimate, ultra-efficient holographic nametag plugin for Bukkit, Spigot, Paper, Purpur & Folia.*

  [![Paper & Spigot API](https://img.shields.io/badge/Bukkit_|_Spigot_|_Paper_|_Purpur-1.21+-333333?style=flat-square&logo=paper)](https://papermc.io/)
  [![Folia Compatible](https://img.shields.io/badge/Folia-Compatible-ff5e00?style=flat-square&logo=fastapi)](https://papermc.io/software/folia)
  [![Java 21](https://img.shields.io/badge/Java-21_LTS-007396?style=flat-square&logo=openjdk)](https://adoptium.net/)
  [![License](https://img.shields.io/github/license/Eligiusmc/EligiusNametag?style=flat-square&color=blue)](LICENSE)
  [![Release](https://img.shields.io/github/v/release/Eligiusmc/EligiusNametag?style=flat-square&color=success)](https://github.com/Eligiusmc/EligiusNametag/releases)
  [![Wiki](https://img.shields.io/badge/Wiki-Multi--Language-blueviolet?style=flat-square&logo=gitbook)](https://eligiusmc.github.io/EligiusNametag/)

  [📚 **Read the Official Wiki (6 Languages)**](https://eligiusmc.github.io/EligiusNametag/) •
  [🐛 **Report an Issue**](https://github.com/Eligiusmc/EligiusNametag/issues)

</div>

---

## 🌟 Overview

**EligiusNametag** is a next-generation nametag plugin built for Minecraft 1.21 - 26.1.2+ servers running Bukkit, Spigot, Paper, Purpur or Folia. Instead of relying on legacy physical entities or scoreboards that clutter your server, it uses pure `TextDisplays` handled by the Native Paper API engine. This means:

* 📉 **Zero Server Lag:** No physical entities taking up memory.
* 🛡️ **Zero Dependencies:** No fragile ProtocolLib packet injections that break every update.
* 🚀 **Future-Proof & Universal:** Runs natively on **Bukkit, Spigot, Paper, Purpur, and Folia 1.21 - 26.1.2+** out of the box.

### ✨ Key Features

- ⚡ **True Folia Support:** Built from the ground up with asynchronous, thread-safe architecture to fully leverage Folia's multi-threading capabilities.
- 💾 **Smart Data Persistence:** Local SQLite or remote MySQL powered by **HikariCP** for safe, high-performance database pooling across proxy networks.
- 🔴 **Native Redis Pub/Sub:** Zero-polling real-time cross-server synchronization. Toggling your nametag on a lobby instantly updates the survival server in less than 5ms!
- 🎨 **Modern MiniMessage & Gradients:** Say goodbye to legacy `&` colors. Enjoy full RGB gradient support! `<gradient:blue:aqua>Admin</gradient>`.
- ✨ **Global Animation Engine:** Add moving frames with `<anim:rainbow>` or `<anim:pulse>` natively without lagging the server.
- 📦 **ItemsAdder & Oraxen:** Display custom emojis and icons natively right inside the nametag.
- 👑 **Vault Hierarchy Integration:** Automatically assigns nametag formats based on the user's primary Vault rank (e.g., admin, vip, default).
- 🐾 **Holographic Pets:** Tamed wolves, cats, and parrots inherit their owner's rank formatting!
- 🌐 **Global i18n:** In-game messages are completely customizable via language files with hot-swapping support.
- 📊 **bStats Integration:** Anonymous usage metrics help guide the plugin's development. Opt-out anytime in your global bStats config.

---

## 🚀 Quick Start

1. Download the latest `EligiusNametag-x.x.x.jar` from the [Releases](https://github.com/Eligiusmc/EligiusNametag/releases) page.
2. Drop it into your `plugins/` directory.
3. Start your server!
4. Visit the **[Wiki](https://eligiusmc.github.io/EligiusNametag/)** to learn how to configure groups, databases, and more.

---

## 👨‍💻 For Developers & Contributors

We love open-source contributions! If you want to dive into the codebase, fix bugs, or understand our Hexagonal Architecture:

👉 **[Read the Development Guidelines (DEVELOPMENT.md)](docs/DEVELOPMENT.md)**

### Building from Source

This project enforces **Java 21 LTS** and targets **Bukkit, Spigot, Paper, Purpur, and Folia 1.21 - 26.1.2+**.

```bash
# Clone the repository
git clone https://github.com/Eligiusmc/EligiusNametag.git
cd EligiusNametag

# Build the jar using Gradle
./gradlew build
```
*The compiled `.jar` will be available in `build/libs/`.*

### Contributing Best Practices

- **Branching:** Never push directly to `master`. All work should stem from `develop` into `feature/<name>` branches.
- **Commits:** We strictly enforce [Conventional Commits](https://www.conventionalcommits.org/) (e.g., `feat:`, `fix:`, `docs:`). This powers our automated Release Please pipelines.
- **Documentation:** If you add a new feature, please update the multilingual Wiki (`docs-site/src/`). 

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
