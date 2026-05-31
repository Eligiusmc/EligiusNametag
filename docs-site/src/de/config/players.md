---
title: Spieler Designs
description: Konfigurieren Sie benutzerdefinierte MiniMessage- und Vault-Designs in der players.yml.
---

# Spieler Designs (players.yml)

Hier passiert die visuelle Magie. Die Datei `players.yml` ermöglicht es Ihnen, **unbegrenzte Gruppen** mit der modernen *MiniMessage* Engine zu definieren, interpoliert mit der Vielseitigkeit der *PlaceholderAPI*.

---

## 🎨 MiniMessage verstehen

Anstatt alte und eingeschränkte Farbcodes wie `&c` oder `&l` zu verwenden, können Sie nun hochgradig deskriptive HTML-Tags verwenden.

| Tag | Ergebnis |
|-----|----------|
| `<red>` | Rein roter Text |
| `<#ff00ff>` | Text mit benutzerdefiniertem HEX-Code |
| `<bold>` | **Fetter** Text |
| `<gradient:red:blue>` | Erzeugt einen reibungslosen Übergang zwischen zwei oder mehr Farben |

---

## 👥 Standardformat

Jeder Benutzer, der in keine Ranghierarchie passt, fällt in dieses Design:

```yaml
players:
  default_format:
    - "<yellow>Spieler</yellow>"
    - "<white><PLAYER></white>"
```
*Sie werden feststellen, dass Sie so viele Zeilen erstellen können, wie Sie möchten, indem Sie einfach Bindestriche (`-`) zur Liste hinzufügen.*

---

## 👑 Fortgeschrittene Ränge & Vault

Sie können Nametag-Gruppen erstellen, die exakt Ihrem Rangnamen in **LuckPerms** (oder einem anderen Vault-kompatiblen Berechtigungssystem) entsprechen. Das Plugin liest die *Primary Group* (Hauptgruppe) des Spielers und sucht hier nach deren Entsprechung.

```yaml
  groups:
    vip:
      - "<gradient:gold:yellow>⭐ VIP Benutzer</gradient>"
      - "<white><PLAYER></white>"
    admin:
      - "<red>:rank_dev: Oberster Admin</red>"
      - "<gradient:red:dark_red><bold><PLAYER></bold></gradient>"
```

### 🖼️ Integration mit ItemsAdder / Oraxen

Wenn Sie sich das `admin` Format genau ansehen, werden Sie den Text `:rank_dev:` bemerken. 

Wenn Sie **ItemsAdder** oder ein beliebiges Ressourcenpaket auf Ihrem Server installiert haben, liest der Client des Spielers dies und ersetzt die Doppelpunkte automatisch durch ein **echtes Schriftbild**, das über dem Kopf des Admins gerendert wird.

::: tip 💡 DESIGN-TIPP
Vermeiden Sie es, Nametags für normale Spieler mehr als 3 Zeilen lang zu machen. Übermäßig viele Zeilen können die Sicht im PvP oder in dichten Gebäuden behindern.
:::
