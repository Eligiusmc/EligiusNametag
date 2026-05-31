---
title: Designs des Joueurs
description: Configurez les designs personnalisés MiniMessage et Vault dans players.yml.
---

# Designs des Joueurs (players.yml)

C'est ici que la magie visuelle opère. Le fichier `players.yml` vous permet de définir **des groupes illimités** en utilisant le moteur moderne *MiniMessage*, interpolé avec la polyvalence de *PlaceholderAPI*.

---

## 🎨 Comprendre MiniMessage

Au lieu d'utiliser d'anciens codes couleur limités comme `&c` ou `&l`, vous pouvez désormais utiliser des balises HTML hautement descriptives.

| Balise | Résultat |
|--------|----------|
| `<red>` | Texte en rouge pur |
| `<#ff00ff>` | Texte utilisant un code HEX personnalisé |
| `<bold>` | Texte en **gras** |
| `<gradient:red:blue>` | Génère une transition fluide entre deux ou plusieurs couleurs |

---

## 👥 Format par Défaut

Tout utilisateur qui ne correspond pas à une hiérarchie de rangs tombera dans ce design :

```yaml
players:
  default_format:
    - "<yellow>Joueur</yellow>"
    - "<white><PLAYER></white>"
```
*Vous remarquerez que vous pouvez créer autant de lignes que vous le souhaitez en ajoutant simplement des tirets (`-`) à la liste.*

---

## 👑 Rangs Avancés & Vault

Vous pouvez créer des groupes de nametag qui correspondent exactement à votre nom de rang dans **LuckPerms** (ou tout autre système de permissions compatible avec Vault). Le plugin lit le rang principal (*Primary Group*) du joueur et cherche son équivalent ici.

```yaml
  groups:
    vip:
      - "<gradient:gold:yellow>⭐ Utilisateur VIP</gradient>"
      - "<white><PLAYER></white>"
    admin:
      - "<red>:rank_dev: Admin Suprême</red>"
      - "<gradient:red:dark_red><bold><PLAYER></bold></gradient>"
```

### 🖼️ Intégration avec ItemsAdder / Oraxen

Si vous regardez attentivement le format `admin`, vous remarquerez le texte `:rank_dev:`. 

Si vous avez **ItemsAdder** ou tout autre pack de ressources installé sur votre serveur, le client du joueur lira ceci et remplacera automatiquement les deux-points par une **véritable image de police** rendue au-dessus de la tête de l'administrateur.

::: tip 💡 CONSEIL DE DESIGN
Évitez de faire des Nametags de plus de 3 lignes pour les joueurs réguliers. Des lignes excessives peuvent obstruer la vision en JcJ ou dans les constructions denses.
:::
