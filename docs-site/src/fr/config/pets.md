---
title: Animaux Holographiques
description: Comment configurer les nametags des animaux dans pets.yml.
---

# Animaux Holographiques (pets.yml)

Le système d'animaux est l'une des fonctionnalités les plus innovantes d'EligiusNametag. Avec cela, tout loup, perroquet ou chat apprivoisé par un joueur recevra un nametag holographique flottant qui suit l'animal en douceur.

### Configuration de Base

```yaml
pets:
  enabled: true
  show_unnamed: false 
```

- Si `show_unnamed` est `true`, même un loup apprivoisé générique aura un hologramme de texte. 
- Si réglé sur `false`, seuls les animaux que le joueur renomme en utilisant une **Enclume** (Name Tag vanilla) activeront le système holographique pour conserver un style plus Vanilla sur le serveur général.

### Design des Lignes
Utilisez `<DISPLAYNAME>` pour afficher le nom que le joueur lui a donné dans l'enclume, et `<PLAYER>` pour citer le propriétaire.

```yaml
  default_format:
    - "<gray>Animal de <PLAYER></gray>"
    - "<white><DISPLAYNAME></white>"
```

### 🐾 Le Plus Grand Secret !
Les animaux **héritent** du design du rang Vault de leur propriétaire.
Cela signifie que si le propriétaire d'un chat a le rang "Admin" dans `players.yml`, et que le rang Admin a un texte rouge et une icône de feu, **le chat montrera également le texte rouge et l'icône de feu** dans sa variable de groupe (si vous le configurez ainsi en croisant les variables PAPI ou les permissions LuckPerms au niveau du nœud `<PLAYER>`).
