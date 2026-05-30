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

El proyecto sigue una metodología basada en **GitFlow** para asegurar escalabilidad y evitar roturas en los entornos en vivo.

### 6.1 Estructura de Ramas
- **`main`**: Es la rama de **Producción**. Solo contiene código 100% estable, probado y listo para su uso final en servidores en vivo. Nunca se programa directamente aquí.
- **`develop`**: Es la rama **Integradora** y base de nuestro desarrollo continuo. Toda nueva funcionalidad debe fusionarse primero aquí para someterse a pruebas conjuntas.
- **`feature/<nombre>`**: Ramas temporales para desarrollar funcionalidades específicas (ej. `feature/multi-lang`, `feature/sqlite-fix`). Siempre nacen a partir de `develop`.

### 6.2 Ciclo de Trabajo
1. Se clona el proyecto y se ubica en la rama `develop`.
2. Se crea una nueva rama: `git checkout -b feature/nueva-funcionalidad`.
3. Al terminar, se realiza un *Pull Request* (PR) hacia `develop`.
4. Una vez validada y testeada en `develop`, se empaqueta una *Release* y se fusiona hacia `main`.

### 6.3 Política de Versionamiento
Las versiones son estrictamente **consecutivas**.
Toda *feature* o conjunto de características fusionado exitosamente en `develop` implicará un **incremento consecutivo (Version Bump)**.
- El versionamiento se actualiza directamente en el archivo `build.gradle.kts`.
- Por ejemplo, al añadir soporte Folia, se pasa de `v1.0.0` a `v1.1.0`. Parches de errores urgentes incrementan el último número (ej. `v1.1.1`).
