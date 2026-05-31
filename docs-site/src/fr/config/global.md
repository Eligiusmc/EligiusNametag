---
title: Configuration Globale
description: Apprenez à configurer EligiusNametag globalement via config.yml.
---

# Configuration Globale (config.yml)

Le fichier `config.yml` est le cerveau du plugin. Il contrôle la façon dont le plugin interagit avec votre serveur au niveau macro, affectant tous les joueurs de manière égale.

---

## 🛠️ Variables d'Interface

```yaml
# Hauteur de l'hologramme au-dessus de la tête du joueur
# Recommandé : 0.35 pour les humains, 0.50 si vous utilisez des skins avec de grands chapeaux.
y_offset: 0.35

# Distance de vue maximale en blocs
# Des valeurs plus basses peuvent améliorer les performances du client.
view_distance: 64

# Alias de la commande principale
# Tous ceux-ci exécuteront le menu d'aide ou les commandes imbriquées.
command_aliases:
  - "enametag"
  - "eltag"
```

---

## 💾 Connexion Base de Données (HikariCP)

Contrairement aux plugins basiques qui enregistrent les préférences dans des fichiers texte, EligiusNametag permet un flux concurrentiel fluide en utilisant le standard industriel `HikariCP` pour éviter les baisses de performances (Lag Spikes) lors des sauvegardes massives sur d'énormes réseaux Folia.

### 🔹 Option A : SQLite (Local)
Idéal pour les serveurs uniques (Survie isolée, Skyblock). Ne nécessite aucune configuration externe. Un fichier `database.db` sera automatiquement créé dans le dossier du plugin.

```yaml
database:
  type: "SQLITE" 
```

### 🔹 Option B : MySQL (Réseaux Velocity/Bungee)
Idéal si vous possédez plusieurs serveurs connectés et souhaitez que la préférence de la commande `/enametag me` (afficher ou masquer votre nametag) voyage avec le joueur d'un serveur à l'autre.

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

::: warning ⚠️ IMPORTANT CONCERNANT MYSQL
Assurez-vous que la base de données `eligius_network` existe déjà sur votre serveur MySQL/MariaDB avant d'activer le plugin. Le plugin créera les tables en interne, mais ne peut pas créer la base de données racine pour des raisons de sécurité.
:::
