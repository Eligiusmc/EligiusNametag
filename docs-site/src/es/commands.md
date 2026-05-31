---
title: Comandos y Permisos
description: Mira todos los comandos y permisos disponibles para EligiusNametag.
---

# Comandos y Permisos

Todos los comandos por defecto se basan en `/enametag` (o sus alias personalizables `/eligiusnametag`).

| Comando | Descripción | Permiso Requerido |
|---|---|---|
| `/enametag` | Muestra la versión del plugin instalada y el logo del autor. | *Ninguno* |
| `/enametag help` | Muestra el menú de ayuda con todos los comandos disponibles. | `eligiusnametag.admin` |
| `/enametag reload` | Recarga toda la configuración y los archivos YAML en tiempo real, sin reiniciar el servidor. | `eligiusnametag.admin` |
| `/enametag lang <idioma>` | Cambia el idioma global del plugin dinámicamente y recarga los lenguajes al instante. | `eligiusnametag.admin` |
| `/enametag pets` | Alterna globalmente la habilitación de los hologramas de texto sobre las mascotas. | `eligiusnametag.admin` |
| `/enametag me` | Activa o desactiva de manera personal la visualización de tu propio nametag (Guardado automático en DB). | `eligiusnametag.viewself` |

## Detalles de Permisos Adicionales

- `eligiusnametag.*`: Otorga acceso total a todos los comandos de administración.
- *Nota:* Si tienes LuckPerms, no olvides asignar el permiso principal a tu grupo *default* o *admin* para que las variables de formato se sincronicen correctamente.
