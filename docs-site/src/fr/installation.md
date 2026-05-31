---
title: Installation et Prérequis
description: Apprenez à installer EligiusNametag sur votre serveur Paper ou Folia.
---

# Installation et Prérequis

EligiusNametag est un plugin moderne et puissant. En tirant parti des dernières fonctionnalités de l'API de texte Minecraft et du multi-threading, il nécessite un environnement à jour.

## 📋 Prérequis Système

| Prérequis | Version Minimale | Raison |
|-----------|------------------|--------|
| **Plateforme** | Paper 1.21 ou Folia 1.21+ | Utilisation stricte des composants `TEXT_DISPLAY` et des commandes Brigadier. |
| **Java** | Java 21 LTS | Code source compilé au format JDK 21. |

## 📦 Dépendances

Vous devez installer les plugins suivants dans votre dossier `plugins/` :

1. **ProtocolLib** *(Requis)* : Utilisé pour envoyer les paquets d'hologrammes de manière invisible sans consommer de TPS physiques.
2. **Vault** *(Optionnel)* : Si vous souhaitez accorder des designs de nametag spécifiques selon le rang de l'utilisateur (Admin, VIP, etc.).
3. **PlaceholderAPI** *(Optionnel)* : Pour analyser des variables externes telles que les statistiques, l'argent ou les clans dans les nametags.
4. **ItemsAdder** *(Optionnel)* : Pour afficher des émojis personnalisés dans les noms.

## 🚀 Étapes d'Installation

1. Téléchargez le fichier `EligiusNametag-1.x.x.jar` depuis l'onglet **Releases**.
2. Copiez le fichier dans le répertoire `plugins/` de votre serveur.
3. Assurez-vous d'avoir installé **ProtocolLib**.
4. Démarrez votre serveur.
5. Vous verrez l'animation de démarrage dans la console avec le chat Eligius MC confirmant que le plugin s'est connecté avec succès.
6. Terminé ! Vous pouvez tout configurer depuis `plugins/EligiusNametag/`.
