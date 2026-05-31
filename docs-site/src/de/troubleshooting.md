---
title: Fehlerbehebung
description: Häufige Probleme und Lösungen bei der Verwendung von EligiusNametag.
---

# Fehlerbehebung (Troubleshooting)

Haben Sie Probleme mit EligiusNametag? Überprüfen Sie diese häufigen Lösungen:

### 1. Das Nametag erscheint nicht über dem Spieler
- **Haben Sie ProtocolLib installiert?** Unser Plugin ist strikt auf Paketinjektion angewiesen, damit Nametags reibungslos schweben, ohne Lags zu verursachen.
- **Haben Sie PlaceholderAPI installiert?** Wenn Sie Variablen wie `%vault_rank%` verwenden und keine PAPI haben, kann der Text möglicherweise nicht gerendert werden.

### 2. "Database Connection Closed" Fehler
- Dies wurde in den **neuesten Versionen behoben**. Stellen Sie sicher, dass Sie die neueste Version `v1.x.x` oder höher verwenden. Wenn Sie **MySQL** verwenden, überprüfen Sie, ob die IP und die Anmeldeinformationen in der `config.yml` korrekt sind. Verwenden Sie andernfalls lokales `SQLITE`.

### 3. ViaVersion-Warnungen in der Konsole
Wenn Sie ViaVersion verwenden, um Spielern auf 1.20 den Beitritt zu Ihrem 1.21+ Server zu ermöglichen, sehen Sie möglicherweise Meldungen über *Metadata* und *Protocol Versions*.
- **Ursache:** Dem alten Client (z. B. 1.20.2) fehlt der native Code, um große `TEXT_DISPLAY` Einheiten richtig zu verarbeiten.
- **Lösung:** Wir empfehlen dringend, 1.21+ Versionen für die perfekte Hologramm-Visualisierung vorauszusetzen, oder ignorieren Sie einfach die Warnung, wenn beim Spieler keine visuellen Abstürze auftreten.

### 4. /enametag lang aktualisiert die Hilfemeldung nicht
Wenn Sie `/enametag lang de` verwenden, liest das Plugin aus `lang/de.yml`. Wenn Sie bemerken, dass eine Übersetzung leer ist oder sich ein Befehl seltsam verhält, können Sie den `lang/` Ordner in Ihrem Plugin-Verzeichnis löschen, damit EligiusNametag ihn mit den neuesten Standards sauber neu generieren kann.
