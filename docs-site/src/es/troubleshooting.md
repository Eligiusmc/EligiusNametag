---
title: Resolución de Problemas
description: Problemas comunes y soluciones al usar EligiusNametag.
---

# Resolución de Problemas (Troubleshooting)

¿Tienes algún problema con EligiusNametag? Revisa estas soluciones comunes:

### 1. El Nametag no aparece encima del jugador
- **¿Tienes ProtocolLib instalado?** Nuestro plugin depende estrictamente de inyección de paquetes para que los nametags floten suavemente sin causar lag.
- **¿Tienes PlaceholderAPI instalado?** Si usas variables como `%vault_rank%` y no tienes PAPI, el texto puede fallar en renderizarse.

### 2. Error de "Database Connection Closed"
- Esto ha sido **solucionado en versiones recientes**. Asegúrate de estar corriendo la última versión `v1.x.x` o superior. Si usas **MySQL**, verifica que la IP y las credenciales en `config.yml` sean las correctas. De lo contrario, usa `SQLITE` local.

### 3. Advertencias de ViaVersion en la Consola
Si usas ViaVersion para permitir que jugadores con la 1.20 entren a tu servidor 1.21+, podrías ver mensajes sobre *Metadata* y *Protocol Versions*.
- **Causa:** El cliente antiguo (ej. 1.20.2) no tiene el código nativo para procesar bien entidades grandes de `TEXT_DISPLAY`.
- **Solución:** Recomendamos encarecidamente que exijas versiones 1.21+ para la visualización perfecta de hologramas, o ignores la advertencia si el jugador no presenta crasheos visuales.

### 4. /enametag lang no actualiza el mensaje de ayuda
Al usar `/enametag lang en`, el plugin lee de `lang/en.yml`. Si notas que alguna traducción está vacía o un comando se comporta raro, puedes borrar la carpeta `lang/` en tu directorio de plugins para que EligiusNametag la re-genere limpiamente con los últimos estándares.
