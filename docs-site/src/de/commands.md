---
title: Befehle und Berechtigungen
description: Zeigen Sie alle verfügbaren Befehle und Berechtigungen für EligiusNametag an.
---

# Befehle und Berechtigungen

Alle Standardbefehle basieren auf `/enametag` (oder den anpassbaren Aliasen `/eltag` und `/eligiusnametag`).

| Befehl | Beschreibung | Erforderliche Berechtigung |
|--------|--------------|----------------------------|
| `/enametag` | Zeigt die installierte Plugin-Version und das Autorenlogo an. | *Keine* |
| `/enametag reload` | Lädt alle Konfigurations- und YAML-Dateien in Echtzeit neu, ohne den Server neu zu starten. | `eligiusnametag.admin` |
| `/enametag lang <sprache>` | Ändert die globale Plugin-Sprache dynamisch und lädt die Sprachen sofort neu. | `eligiusnametag.admin` |
| `/enametag pets` | Schaltet global die Anzeige von Text-Hologrammen über Haustieren um. | `eligiusnametag.admin` |
| `/enametag me` | Schaltet die Anzeige des eigenen Nametags persönlich ein oder aus (Automatisches Speichern in der DB). | `eligiusnametag.viewself` |

## Zusätzliche Berechtigungsdetails

- `eligiusnametag.*`: Gewährt vollen Zugriff auf alle Verwaltungsbefehle.
- *Hinweis:* Wenn Sie LuckPerms verwenden, vergessen Sie nicht, Ihrem *default* oder *admin* Rang die Hauptberechtigung zuzuweisen, damit die Formatierungsvariablen korrekt synchronisiert werden.
