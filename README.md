# EligiusNametag

Un plugin avanzado para servidores **Paper 1.21+** que reemplaza los "nametags" aburridos de Vanilla por hologramas de texto flotantes totalmente configurables usando entidades virtuales `TEXT_DISPLAY`. 

Este plugin ha sido diseñado buscando la máxima eficiencia (usando ProtocolLib), garantizando compatibilidad con rangos de Vault, fuentes e íconos de ItemsAdder, y cientos de variables mediante PlaceholderAPI. ¡Funciona tanto para jugadores como para mascotas domadas!

---

## 🌟 Características Principales

- **Soporte Nativo Folia:** Código asíncrono y thread-safe real diseñado para sacar el máximo provecho de las redes multihilo de Folia.
- **Bases de Datos Flexibles:** Persistencia en SQLite local o conexión a MySQL remoto (gestionado inteligentemente por HikariCP) para redes proxy.
- **Múltiples líneas:** Crea nametags de cualquier tamaño, 100% configurables.
- **Jerarquía Vault:** Asigna formatos de nametag diferentes según el rango del usuario (ej. `admin`, `vip`, `usuario`).
- **PlaceholderAPI Integrado:** Soporte para todas tus variables favoritas (ej. `%luckperms_prefix%`).
- **Iconos de ItemsAdder:** Convierte mágicamente atajos como `:rank_dev:` en imágenes de fuente reales arriba de la cabeza de los jugadores.
- **Formato MiniMessage Moderno:** Usa `<red>`, `<bold>`, `<gradient:red:blue>` para diseños increíbles, además de soportar automáticamente códigos legacy (`&c`, `&l`).
- **Control para el Jugador:** Comando `/eltag me` para que el propio usuario decida si quiere ver su propio nametag (sus preferencias se guardan en base de datos al reiniciar).
- **Mascotas Holográficas:** Dale un toque único a los lobos y gatos de tus jugadores. El nametag de la mascota heredará el formato del rango VIP/Admin de su dueño.

---

## 💾 Instalación

1. Descarga el `.jar` compilado de EligiusNametag.
2. Coloca el archivo en la carpeta `plugins/` de tu servidor.
3. Asegúrate de tener instalado el plugin **ProtocolLib**.
4. ¡Inicia el servidor!
5. Se generará automáticamente la carpeta `plugins/EligiusNametag/` con tu `config.yml`, `players.yml`, `pets.yml` y la carpeta `lang/`.

### Dependencias

| Plugin | Requerido | Descripción |
|---|---|---|
| **ProtocolLib** | ✅ Obligatorio | Requerido para enviar la magia visual de los TextDisplays sin laggear el servidor. |
| **PlaceholderAPI** | ❌ Opcional | Recomendado para agregar variables y estadísticas en los nombres. |
| **Vault** | ❌ Opcional | Recomendado para vincular los nombres a los rangos de tu servidor. |
| **ItemsAdder** | ❌ Opcional | Recomendado para inyectar emojis/iconos gráficos en los nombres. |

> **Nota de Compatibilidad:** Solo funciona en servidores **Paper 1.21 o superior** debido a las capacidades nativas de la API de texto y Brigadier introducidas en estas versiones. Se requiere **Java 21**.

---

## 🎮 Comandos y Permisos

Todos los comandos por defecto se basan en `/eltag` (o sus alias personalizables `/enametag` y `/eligiusnametag`).

| Comando | Descripción | Permiso Requerido |
|---|---|---|
| `/eltag` | Muestra la versión del plugin instalada. | *Ninguno* |
| `/eltag reload` | Recarga toda la configuración (formatos, alturas, etc.) en tiempo real. | `eligiusnametag.admin` |
| `/eltag lang <idioma>` | Cambia el idioma global del plugin dinámicamente. | `eligiusnametag.admin` |
| `/eltag pets` | Habilita o deshabilita los hologramas en las mascotas globalmente. | `eligiusnametag.admin` |
| `/eltag me` | Activa o desactiva la visualización de tu propio nametag. | `eligiusnametag.viewself` |

---

## ⚙️ Configuración Modular

El plugin utiliza un sistema de configuración descentralizado para mayor organización:

### `config.yml` (Opciones Globales)
Puedes controlar el entorno y la base de datos:

```yaml
# Altura del holograma por encima de la cabeza del jugador
y_offset: 0.35

# Base de datos (SQLite o MySQL)
database:
  type: "SQLITE"
```

### `players.yml` (Diseños para Jugadores)
El diseño de los jugadores se configura como una lista de líneas. Si el jugador tiene el rango `admin` en Vault, usará ese diseño. Si no, usará el `default_format`.

```yaml
players:
  default_format:
    - "<yellow>Jugador</yellow>"
    - "<white><PLAYER></white>"
  
  groups:
    admin:
      - "<red>:rank_dev: Administrador</red>"
      - "<gradient:red:gold><PLAYER></gradient>"
```

### `pets.yml` (Diseños para Mascotas)
Todos los lobos, gatos o loros domados tendrán su propio nametag holográfico.

```yaml
pets:
  enabled: true
  
  default_format:
    - "<gray>Mascota de <PLAYER></gray>"
    - "<white><DISPLAYNAME></white>"
```

---

## 👨‍💻 Para Desarrolladores

Si planeas compilar el plugin tú mismo, extender sus funcionales o si eres un agente IA intentando comprender cómo arreglar bugs dentro del ecosistema de ProtocolLib:

👉 **[Lee la Documentación Técnica de Desarrollo y GitFlow (DEVELOPMENT.md)](docs/DEVELOPMENT.md)**