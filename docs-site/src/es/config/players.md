---
title: Diseños de Jugadores
description: Configura diseños personalizados con MiniMessage y Vault en players.yml.
---

# Diseños de Jugadores (players.yml)

Aquí es donde ocurre la magia visual. El archivo `players.yml` te permite definir **grupos ilimitados** usando el moderno motor de *MiniMessage*, interpolado con la versatilidad de *PlaceholderAPI*.

---

## 🎨 Entendiendo MiniMessage

En lugar de usar los antiguos y limitados códigos de color como `&c` o `&l`, ahora puedes usar etiquetas HTML súper descriptivas.

| Etiqueta | Resultado |
|----------|-----------|
| `<red>` | Texto en color rojo puro |
| `<#ff00ff>` | Texto usando código HEX personalizado |
| `<bold>` | Texto en **negrita** |
| `<gradient:red:blue>` | Genera una transición suave entre dos o más colores |

---

## 👥 Formato por Defecto (Default)

Cualquier usuario que no calce en ninguna jerarquía de rangos caerá en este diseño:

```yaml
players:
  default_format:
    - "<yellow>Jugador</yellow>"
    - "<white><PLAYER></white>"
```
*Notarás que puedes crear tantas líneas como desees simplemente añadiendo guiones (`-`) a la lista.*

---

## 👑 Rangos y Vault Avanzados

Puedes crear grupos de nametag que coincidan exactamente con el nombre de tu rango en **LuckPerms** (o cualquier otro sistema de permisos compatible con Vault). El plugin lee el rango principal (*Primary Group*) del jugador y busca su equivalente aquí.

```yaml
  groups:
    vip:
      - "<gradient:gold:yellow>⭐ Usuario VIP</gradient>"
      - "<white><PLAYER></white>"
    admin:
      - "<red>:rank_dev: Administrador Supremo</red>"
      - "<gradient:red:dark_red><bold><PLAYER></bold></gradient>"
```

### 🖼️ Integración con ItemsAdder / Oraxen

Si observas detenidamente el formato de `admin`, notarás el texto `:rank_dev:`. 

Si tienes **ItemsAdder** o cualquier Resource Pack instalado en tu servidor, el cliente del jugador leerá esto y reemplazará automáticamente los dos puntos por una **imagen de fuente real** renderizada sobre la cabeza del administrador.

::: tip 💡 CONSEJO DE DISEÑO
Evita hacer Nametags de más de 3 líneas para los jugadores comunes. Excesivas líneas pueden obstruir la visión en pvp o en construcciones densas.
:::
