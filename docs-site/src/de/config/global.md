---
title: Globale Konfiguration
description: Erfahren Sie, wie Sie EligiusNametag global über die config.yml konfigurieren.
---

# Globale Konfiguration (config.yml)

Die `config.yml` Datei ist das Gehirn des Plugins. Sie steuert, wie das Plugin auf Makroebene mit Ihrem Server interagiert, was sich auf alle Spieler gleichermaßen auswirkt.

---

## 🛠️ Schnittstellen-Variablen

```yaml
# Hologrammhöhe über dem Kopf des Spielers
# Empfohlen: 0.35 für Menschen, 0.50 bei der Verwendung von Skins mit großen Hüten.
y_offset: 0.35

# Maximale Sichtweite in Blöcken
# Niedrigere Werte können die Client-Performance verbessern.
view_distance: 64

# Hauptbefehls-Aliase
# Alle diese führen das Hilfemenü oder verschachtelte Befehle aus.
command_aliases:
  - "enametag"
```

---

## 💾 Datenbankverbindung (HikariCP)

Im Gegensatz zu einfachen Plugins, die Einstellungen in Textdateien speichern, ermöglicht EligiusNametag einen reibungslosen parallelen Fluss unter Verwendung des Industriestandards `HikariCP`, um Leistungseinbrüche (Lag Spikes) während massiver Speicherungen in riesigen Folia-Netzwerken zu verhindern.

### 🔹 Option A: SQLite (Lokal)
Ideal für Einzelserver (isoliertes Survival, Skyblock). Erfordert keine externe Konfiguration. Eine `database.db` Datei wird automatisch im Plugin-Ordner erstellt.

```yaml
database:
  type: "SQLITE" 
```

### 🔹 Option B: MySQL (Velocity/Bungee Netzwerke)
Ideal, wenn Sie mehrere verbundene Server besitzen und möchten, dass die Einstellung des `/enametag me` Befehls (Anzeigen oder Ausblenden Ihres Nametags) mit dem Spieler von einem Server zum anderen reist.

```yaml
database:
  type: "MYSQL"
  mysql:
    host: "127.0.0.1"
    port: 3306
    database: "eligius_network"
    username: "admin"
    password: "super_secure_password"
```

::: warning ⚠️ WICHTIG BEZÜGLICH MYSQL
Stellen Sie sicher, dass die Datenbank `eligius_network` bereits auf Ihrem MySQL/MariaDB-Server existiert, bevor Sie das Plugin aktivieren. Das Plugin erstellt die Tabellen intern, kann aber aus Sicherheitsgründen die Stammdatenbank nicht erstellen.
:::


---

## 🔴 Redis Pub/Sub Synchronisation

Wenn Sie ein Proxy-Netzwerk (BungeeCord, Velocity) betreiben, reicht MySQL-Polling für visuelle Aktualisierungen in Echtzeit nicht aus. Wenn Redis aktiviert ist und ein Spieler sein Nametag in der Lobby ausblendet, wird diese Änderung in weniger als 5 Millisekunden sofort auf dem Survival-Server repliziert.

`yaml
redis:
  enabled: true
  host: '127.0.0.1'
  port: 6379
  password: 'my_secure_redis_password'
`
