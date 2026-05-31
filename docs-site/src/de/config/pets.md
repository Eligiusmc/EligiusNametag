---
title: Holografische Haustiere
description: So konfigurieren Sie Haustier-Nametags in der pets.yml.
---

# Holografische Haustiere (pets.yml)

Das Haustiersystem ist eine der innovativsten Funktionen von EligiusNametag. Damit erhält jeder Wolf, Papagei oder Katze, der von einem Spieler gezähmt wurde, ein schwebendes holografisches Nametag, das dem Haustier reibungslos folgt.

### Basis-Konfiguration

```yaml
pets:
  enabled: true
  show_unnamed: false 
```

- Wenn `show_unnamed` auf `true` steht, erhält selbst ein generischer gezähmter Wolf ein Text-Hologramm. 
- Wenn es auf `false` gesetzt ist, aktivieren nur Haustiere, die der Spieler mit einem **Amboss** (Vanilla Name Tag) umbenennt, das holografische System, um einen Vanilla-ähnlicheren Stil auf dem allgemeinen Server beizubehalten.

### Zeilendesign
Verwenden Sie `<DISPLAYNAME>`, um den Namen anzuzeigen, den der Spieler ihm im Amboss gegeben hat, und `<PLAYER>`, um den Besitzer zu zitieren.

```yaml
  default_format:
    - "<gray>Haustier von <PLAYER></gray>"
    - "<white><DISPLAYNAME></white>"
```

### 🐾 Das größte Geheimnis!
Haustiere **erben** das Vault-Rangdesign ihres Besitzers.
Das bedeutet: Wenn der Besitzer einer Katze den Rang "Admin" in der `players.yml` hat, und der Admin-Rang roten Text und ein Feuersymbol aufweist, **zeigt die Katze ebenfalls den roten Text und das Feuersymbol** in ihrer Gruppenvariable (wenn Sie dies so konfigurieren, indem Sie PAPI-Variablen oder LuckPerms-Berechtigungen auf der Ebene des `<PLAYER>` Knotens kreuzen).
