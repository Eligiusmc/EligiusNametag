---
title: Mascotas Holográficas
description: Cómo configurar los nametags de mascotas en pets.yml.
---

# Mascotas Holográficas (pets.yml)

El sistema de mascotas es una de las características más innovadoras de EligiusNametag. Con él, cualquier lobo, loro, o gato domado por un jugador recibirá un nametag holográfico flotante que sigue a la mascota fluidamente.

### Configuración Base

```yaml
pets:
  enabled: true
  show_unnamed: false 
```

- Si `show_unnamed` está en `true`, incluso un lobo genérico domado tendrá un holograma de texto. 
- Si lo pones en `false`, solo las mascotas que el jugador renombre usando un **Yunque** (Name Tag vanilla) activarán el sistema holográfico para conservar un estilo más Vanilla en el servidor general.

### Diseño de las Líneas
Usa `<DISPLAYNAME>` para mostrar el nombre que le puso el jugador en el yunque y `<PLAYER>` para citar al dueño.

```yaml
  default_format:
    - "<gray>Mascota de <PLAYER></gray>"
    - "<white><DISPLAYNAME></white>"
```

### 🐾 ¡El secreto más importante!
Las mascotas **heredan** el diseño del rango Vault de su dueño.
Esto quiere decir que si el dueño de un gato tiene rango "Admin" en `players.yml`, y el rango Admin tiene un texto rojo y un ícono de fuego, **el gato también lucirá el texto rojo y el ícono de fuego** en su variable de grupo (si lo configuras así cruzando variables PAPI o permisos de LuckPerms a nivel del nodo `<PLAYER>`).
