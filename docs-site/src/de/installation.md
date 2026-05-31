---
title: Installation und Anforderungen
description: Erfahren Sie, wie Sie EligiusNametag auf Ihrem Paper- oder Folia-Server installieren.
---

# Installation und Anforderungen

EligiusNametag ist ein modernes und leistungsstarkes Plugin. Da es die neuesten Funktionen der Minecraft Text-API und Multi-Threading nutzt, erfordert es eine aktuelle Umgebung.

## 📋 Systemanforderungen

| Anforderung | Mindestversion | Grund |
|-------------|----------------|-------|
| **Plattform** | Paper 1.21 - 26.1.2+ oder Folia 1.21 - 26.1.2+ | Strikte Verwendung von `TEXT_DISPLAY` Komponenten und Brigadier-Befehlen. |
| **Java** | Java 21 LTS | Quellcode im JDK 21-Format kompiliert. |

## 📦 Abhängigkeiten

Sie müssen die folgenden Plugins in Ihrem `plugins/` Ordner installiert haben:

1. **Vault** *(Optional)*: Wenn Sie spezifische Nametag-Designs abhängig vom Rang des Benutzers (Admin, VIP usw.) vergeben möchten.
2. **PlaceholderAPI** *(Optional)*: Um externe Variablen wie Statistiken, Geld oder Clans in den Nametags zu parsen.
3. **ItemsAdder** *(Optional)*: Um benutzerdefinierte Emojis in Namen anzuzeigen.

## 🚀 Installationsschritte

1. Laden Sie die Datei `EligiusNametag-1.x.x.jar` aus dem **Releases** Tab herunter.
2. Kopieren Sie die Datei in das `plugins/` Verzeichnis Ihres Servers.
3. Starten Sie Ihren Server.
4. Sie sehen die Startanimation in der Konsole mit der Eligius MC Katze, die bestätigt, dass sich das Plugin erfolgreich verbunden hat.
5. Fertig! Sie können alles in `plugins/EligiusNametag/` konfigurieren.
