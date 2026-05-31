---
title: Commandes et Permissions
description: Voir toutes les commandes et permissions disponibles pour EligiusNametag.
---

# Commandes et Permissions

Toutes les commandes par défaut sont basées sur `/enametag` (ou ses alias personnalisables `/eltag` et `/eligiusnametag`).

| Commande | Description | Permission Requise |
|----------|-------------|--------------------|
| `/enametag` | Affiche la version du plugin installée et le logo de l'auteur. | *Aucune* |
| `/enametag reload` | Recharge toute la configuration et les fichiers YAML en temps réel, sans redémarrer le serveur. | `eligiusnametag.admin` |
| `/enametag lang <langue>` | Change dynamiquement la langue globale du plugin et recharge les langues instantanément. | `eligiusnametag.admin` |
| `/enametag pets` | Active/désactive globalement l'affichage des hologrammes de texte au-dessus des animaux. | `eligiusnametag.admin` |
| `/enametag me` | Active ou désactive personnellement l'affichage de votre propre nametag (Sauvegarde auto en BDD). | `eligiusnametag.viewself` |

## Détails des Permissions Supplémentaires

- `eligiusnametag.*` : Accorde un accès complet à toutes les commandes d'administration.
- *Remarque :* Si vous utilisez LuckPerms, n'oubliez pas d'attribuer la permission principale à votre groupe *default* ou *admin* pour que les variables de formatage se synchronisent correctement.
