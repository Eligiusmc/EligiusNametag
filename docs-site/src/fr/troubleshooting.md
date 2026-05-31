---
title: Dépannage
description: Problèmes courants et solutions lors de l'utilisation d'EligiusNametag.
---

# Dépannage (Troubleshooting)

Vous rencontrez des problèmes avec EligiusNametag ? Consultez ces solutions courantes :

### 1. Le Nametag n'apparaît pas au-dessus du joueur
- **Avez-vous installé ProtocolLib ?** Notre plugin repose strictement sur l'injection de paquets pour que les nametags flottent en douceur sans causer de lag.
- **Avez-vous installé PlaceholderAPI ?** Si vous utilisez des variables comme `%vault_rank%` et que vous n'avez pas PAPI, le texte pourrait ne pas s'afficher.

### 2. Erreur "Database Connection Closed"
- Ce problème a été **corrigé dans les versions récentes**. Assurez-vous d'utiliser la dernière version `v1.x.x` ou supérieure. Si vous utilisez **MySQL**, vérifiez que l'IP et les identifiants dans `config.yml` sont corrects. Sinon, utilisez `SQLITE` localement.

### 3. Avertissements ViaVersion dans la Console
Si vous utilisez ViaVersion pour permettre aux joueurs en 1.20 de rejoindre votre serveur 1.21+, vous pourriez voir des messages concernant les *Metadata* et *Protocol Versions*.
- **Cause :** L'ancien client (ex. 1.20.2) ne possède pas le code natif pour traiter correctement les grandes entités `TEXT_DISPLAY`.
- **Solution :** Nous recommandons vivement d'exiger des versions 1.21+ pour une visualisation parfaite des hologrammes, ou ignorez simplement l'avertissement si le joueur ne rencontre aucun plantage visuel.

### 4. /enametag lang ne met pas à jour le message d'aide
Lors de l'utilisation de `/enametag lang fr`, le plugin lit à partir de `lang/fr.yml`. Si vous remarquez qu'une traduction est vide ou qu'une commande agit bizarrement, vous pouvez supprimer le dossier `lang/` dans votre répertoire de plugins afin qu'EligiusNametag puisse le regénérer proprement avec les dernières normes.
