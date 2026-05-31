# EligiusNametag - Development & Context

Este documento centraliza el estado actual de desarrollo, las decisiones arquitectónicas y los casos de resolución de bugs más complejos. Su objetivo es proporcionar contexto rápido y profundo a cualquier desarrollador (o Agente de IA) que deba mantener o extender el código del plugin.

---

## 1. Arquitectura Hexagonal (Ports & Adapters)

El plugin está estructurado bajo los principios de Arquitectura Hexagonal. El objetivo principal de esto es desacoplar el núcleo lógico del plugin (`domain/`) de las implementaciones específicas de Bukkit, bases de datos o sistemas de renderizado (`adapter/`).

```text
┌──────────────────────────────────────────────────┐
│                  DOMAIN LAYER                    │
│                                                  │
│  ┌──────────────┐    ┌────────────────────────┐  │
│  │  ConfigPort   │    │  NametagRendererPort   │  │
│  │  DatabasePort │    │  PlatformPort          │  │
│  └──────┬───────┘    └────────┬───────────────┘  │
│         │     NametagService  │                   │
│         └─────────┬───────────┘                   │
└───────────────────┼──────────────────────────────┘
                    │
┌───────────────────┼──────────────────────────────┐
│              ADAPTER LAYER                       │
│                                                  │
│  YamlConfigAdapter    ProtocolLibNametagRenderer  │
│  DatabaseAdapter      PaperPlatformAdapter        │
└──────────────────────────────────────────────────┘
```

### 1.1 Puertos (Ports)
- **`ConfigPort.java`**: Expone las configuraciones del usuario. Oculta completamente cómo o de dónde se leen los datos.
- **`DatabasePort.java`**: Expone métodos de lectura/escritura de persistencia. Actualmente almacena si el usuario tiene activada la visualización de su propio nametag (`view_self`).
- **`NametagRendererPort.java`**: Expone cómo renderizar un holograma para ciertos jugadores, ocultarlo o destruir todo. No asume si se usan ArmorStands, TextDisplays o entidades de Vanilla.
- **`PlatformPort.java`**: Encapsula cualquier método propio de la API de Bukkit/PaperMC (ej. obtener listas de jugadores, resolver Vault, aplicar PAPI, verificar si el jugador puede ver la entidad).

### 1.2 Servicio Principal (NametagService)
Es el orquestador principal.
Se ejecuta en un bucle periódico configurado por el usuario (ej. 10 ticks = 0.5s).
Evalúa la distancia, permisos, visibilidad y los grupos del jugador/mascota, genera el diseño de las líneas y luego delega todo el trabajo final al `NametagRendererPort`.

---

## 2. Pipeline de Renderizado y ProtocolLib

El renderizado se confía enteramente a **ProtocolLib** (`ProtocolLibNametagRenderer`) mediante la inyección directa de paquetes en red hacia el cliente. Nunca se crean entidades físicas reales en el servidor (para evitar basura de memoria y caídas de TPS en servidores masivos).

### 2.1 El ciclo de los paquetes
Cuando el `NametagService` solicita renderizar un holograma, el adaptador de ProtocolLib hace lo siguiente por cada espectador:

1. **Paquete `SPAWN_ENTITY` (Solo Primera Vez):** 
   Se crea una entidad virtual de tipo `TEXT_DISPLAY`. Las coordenadas iniciales de aparición (`X, Y, Z`) son obligatoriamente inyectadas usando la locación de la entidad principal. Si no se hace, la entidad virtual aparecerá en `0,0,0`, y si el espectador no tiene cargado el chunk `0,0`, su cliente ignorará la entidad.
2. **Paquete `ENTITY_METADATA` (Solo Primera Vez):**
   Envía el componente de texto real, así como el offset de altura (translation) para apilar los TextDisplays uno sobre el otro sin empujarse físicamente.
3. **Paquete `MOUNT` (Periódico):**
   Envía al cliente la orden de montar el TextDisplay como "pasajero" de la mascota/jugador. Esto permite que el cliente renderice el texto moviéndose junto con la entidad principal sin latencia o temblores (ya que la interpolación del movimiento la hace el cliente).

### 2.2 Bug Resuelto: Crasheo de ViaVersion (JNI Exception)
**Síntoma:** Al entrar o salir repetidamente, la JVM entera se colapsaba con un error de acceso a la memoria nativa (`EXCEPTION_ACCESS_VIOLATION en jvm.dll`).
**Causa:** Inicialmente, los paquetes `ENTITY_METADATA` y `MOUNT` se enviaban *en cada ciclo* a los jugadores para asegurar el acople correcto, pero `ENTITY_METADATA` contiene componentes de chat muy grandes. ViaVersion, al interceptar esto asincrónicamente y tratar de traducir los componentes JSON a formatos Legacy 2 veces por segundo, generaba corrupción de memoria nativa.
**Solución:** Los metadatos de texto ahora solo se envían estrictamente una vez durante la creación (`isNewSpawn = true`), mientras que solo el paquete `MOUNT` (que es de tamaño insignificante) se repite periódicamente en cada actualización.

### 2.3 Bug Resuelto: Desincronización del paquete Mount
**Síntoma:** Al hacer un teletransporte largo o reconectarse, el holograma de las mascotas desaparecía, pero el de vainilla seguía ahí.
**Causa:** `NametagService` detectaba que el usuario estaba cerca del perro y enviaba los paquetes `SPAWN` y `MOUNT` al cliente inmediatamente. Sin embargo, el cliente demoraba varios ticks más en recibir y cargar la información de la entidad real del perro. El cliente ignoraba el paquete `MOUNT` porque el perro aún no existía, dejando el TextDisplay flotando sin rumbo en `X, Y, Z`. 
**Solución:** Como se indicó en la sección 2.1, mover el `MOUNT` fuera del bloque `isNewSpawn` garantiza que una vez el cliente termine de cargar al perro, el siguiente `MOUNT` (que llega cada 0.5s) re-enganchará exitosamente el holograma al animal de forma imperceptible.

---

## 3. Resolución de Visibilidad y Tracking

El plugin requiere saber exactamente si un jugador tiene en su campo visual a una mascota o a un jugador para enviarle o dejar de enviarle el holograma.

Originalmente se usaba `Entity#getTrackedBy()` proveído por PaperMC, con la esperanza de alinear la red de Bukkit con nuestros paquetes. Sin embargo, la actualización del tracking en Paper y ViaVersion demostró generar falsos negativos y demoras, resultando en que la condición devolvía falso prematuramente, o a veces nunca devolvía verdadero.

**Implementación Actual:**
Para la visibilidad de los "tamed mobs" y entidades generales, se abandonó el tracking nativo en favor de un chequeo matemático por distancia radial de 64 bloques (la distancia natural de renderizado de entidades en Minecraft):
```java
return entity.getLocation().distanceSquared(viewer.getLocation()) <= (64.0 * 64.0);
```
Es altamente predecible y performante en comparativas relativas por tick.

---

## 4. Archivos de Configuración Dual y #defaults

### 4.1 Modos de Parseo en config.yml
El adaptador `YamlConfigAdapter` fue diseñado con retrocompatibilidad para parsear tanto saltos de línea heredados `\n` como el moderno sistema de listas YAML nativo:
- **Modo Lista (Nuevo):** `- "<red>Texto 1</red>"`
- **Modo Cadena (Antiguo):** `"<red>Texto 1</red>\n<white>Texto 2</white>"`

### 4.2 Modularización (players.yml, pets.yml, lang/)
El `YamlConfigAdapter` carga concurrentemente múltiples `FileConfiguration`. 
- `config.yml` maneja las bases del entorno.
- `players.yml` y `pets.yml` abstraen las listas de formatos y rangos Vault.
- `lang/` contiene las traducciones dinámicas.
Al inyectar el prefijo, el método `getMessage` simplemente concatena el string del nodo `prefix` de `config.yml` con el mensaje encontrado en el archivo cargado en memoria de `lang/`.

### 4.3 Comandos Mutables (Hot-swapping)
El comando `/enametag lang <idioma>` usa un argumento de tipo `word()`. Al validarse la existencia del nuevo archivo en disco (`YamlConfigAdapter#hasLanguage`), muta la propiedad `language` en memoria, la guarda y obliga al plugin a recargar (`reloadPlugin`), reestructurando todos los mensajes al instante sin reiniciar el servidor. El mismo principio aplica para `/enametag pets enable`.

---

## 5. Build & Environment Info

El plugin está diseñado para apuntar exclusivamente a la API de **Paper 1.21+** y compilado en **Java 21 LTS**.

- Se debió forzar `org.gradle.java.home` en el `gradle.properties` local del entorno a JDK 21 para evitar choques con Gradle e inferencias erróneas al intentar usar JDK 25, lo cual corrompía la resolución de dependencias de `MockBukkit`.
- El uso de JDK 21 es estricto e innegociable.
- MockBukkit fue reubicado desde su artefacto deprecado en github hacia su nuevo dominio `org.mockbukkit.mockbukkit` en MavenCentral.

**Build simple:**
```bash
./gradlew build
```

---

## 6. Workflow (GitFlow) y Versionamiento

El proyecto utiliza un flujo de trabajo altamente automatizado mediante **Release Please** y **GitHub Actions** para garantizar lanzamientos estables y versionamiento semántico predecible. Sigue estas reglas al contribuir:

### 6.1 Estructura de Ramas
- **`master`**: Es la rama de **Producción**. Nunca se programa directamente aquí ni se abren PRs manuales para integrar funcionalidades. Su actualización está orquestada por bots.
- **`develop`**: Es la rama **Integradora** y base de nuestro desarrollo continuo (Beta). 
- **`feature/<nombre>`**: Ramas temporales para desarrollar funcionalidades específicas. Siempre nacen a partir de `develop`.

### 6.2 Ciclo de Trabajo (Conventional Commits)
- Todo trabajo debe realizarse en una rama `feature/xyz`.
- Es obligatorio usar prefijos de **Conventional Commits** tanto en los commits como al fusionar el PR hacia `develop`:
  - `feat:` para nuevas características (Aumenta la versión `MINOR`, ej. 1.0.0 -> 1.1.0).
  - `fix:` para corrección de errores (Aumenta la versión `PATCH`, ej. 1.0.0 -> 1.0.1).
  - `docs:`, `chore:`, `refactor:`, `style:` (No aumentan versión).
  - `BREAKING CHANGE:` en el cuerpo del commit (Aumenta la versión `MAJOR`, ej. 1.0.0 -> 2.0.0).
- Abre un PR de tu rama hacia `develop`. Al aprobarse, se compilará una versión Beta automáticamente.

### 6.3 Promoción a Producción (Develop -> Master)
- Cuando `develop` esté lista para un lanzamiento, los Administradores deben ir a **Actions** en GitHub y ejecutar el flujo **"Promote Develop to Master"**.
- Esto generará automáticamente un Pull Request de `develop` a `master` enumerando los commits realizados.
- El equipo revisa y aprueba este PR.

### 6.4 Lanzamiento y Versionado (Release Please)
- Inmediatamente después de mezclarse el código en `master`, el bot de **Release Please** se activará.
- Analizará el historial y abrirá su propio PR en `master` (ej. `chore(main): release 1.1.0`).
- Este PR contiene la actualización automática del archivo `gradle.properties` y la generación/actualización del `CHANGELOG.md`.
- Al aprobar y mezclar este PR final, la Action publicará automáticamente el TAG (`v1.1.0`), compilará el `.jar` y lo subirá a GitHub Releases, Modrinth y Hangar.

---

## 7. Documentación y Wiki (VitePress)

La Wiki oficial del proyecto está construida usando **VitePress** y se encuentra dentro de la carpeta `docs-site/`. 
Está alojada en GitHub Pages y soporta múltiples idiomas nativamente.

### 7.1 Arquitectura de Traducciones (i18n)
La Wiki no usa un único archivo para todos los idiomas. Cada idioma tiene su propio directorio dentro de `docs-site/src/`:
- `src/` (Inglés - Default)
- `src/es/` (Español)
- `src/fr/` (Francés)
- `src/de/` (Alemán)
- `src/pt/` (Portugués)
- `src/ru/` (Ruso)

### 7.2 Añadir Nuevo Contenido a la Wiki
Si creas una nueva funcionalidad en el plugin y necesitas documentarla:
1. Crea el archivo `.md` en la raíz inglesa (ej. `src/config/new_feature.md`).
2. **Duplica y Traduce:** Debes copiar ese mismo archivo en cada una de las carpetas de idiomas (`src/es/config/new_feature.md`, etc.) y traducir su contenido. No hacerlo causará penalizaciones SEO de Google (Duplicate Content).
3. **Frontmatter SEO:** Todos los archivos Markdown deben iniciar obligatoriamente con metadatos YAML para mejorar su visibilidad en buscadores:
```markdown
---
title: Título de la Página
description: Descripción corta de 150 caracteres.
---
```
4. **Actualiza la Barra Lateral (Sidebar):** Debes registrar el nuevo enlace en el archivo `docs-site/.vitepress/config.mts`, en la sección `themeConfig.sidebar` de **cada uno de los idiomas** (locales) que hayas añadido.

Para compilar y testear la Wiki localmente antes de enviar tu PR, usa:
```bash
cd docs-site
npm install
npm run docs:dev
```

---

## 8. Technical Notes & Fixes (v1.3.0)

### TextDisplay Animation Bug (Fly-in)
By default, native `TextDisplay` entities interpolate movement and mounting. When spawning a `TextDisplay` while a player is moving quickly (sprinting/flying), the client receives the spawn coordinates at the server's tick location, but by the time the packet is processed, the player has moved. This creates a visual "fly-in" animation from behind the player to the mounting offset.

**Solution: Tactical Invisibility:**
To fix this, we set `entity.setTextOpacity((byte) 0);` in the spawn consumer. We then use a 2-tick delayed task (`runTaskLater`) to set the opacity back to 255 (`(byte) -1`). This allows the client to perform the mount interpolation while the text is completely invisible, resulting in a perfect pop-in appearance without visual lag.
Additionally, we added `entity.setTeleportDuration(0)` and `entity.setInterpolationDuration(0)` to prevent native interpolation.

### EntitiesUnloadEvent Memory Leak
Non-persistent `TextDisplay` entities (`entity.setPersistent(false)`) are not saved to the chunk and are silently discarded by Bukkit when chunks unload. Because they are not explicitly removed by the server, their `isValid()` method might still return `true`, causing them to ghost in memory and break our `removeIf` cleanup logic on reload.

**Solution:**
We listen to `EntitiesUnloadEvent`, check for `Tameable` entities, and explicitly call `rendererAdapter.destroyNametag(e.getUniqueId())`, which forces `display.remove()` to properly deallocate the displays before the chunk unloads.

