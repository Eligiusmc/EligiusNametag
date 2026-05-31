---
title: Instalación y Requisitos
description: Aprende cómo instalar EligiusNametag en tu servidor Paper o Folia.
---

# Instalación y Requisitos

EligiusNametag es un plugin moderno y potente. Al aprovechar las últimas características de la API de texto de Minecraft y el sistema de subprocesos múltiples, requiere un entorno actualizado.

## 📋 Requisitos del Sistema

| Requisito | Versión Mínima | Razón |
|-----------|---------------|-------|
| **Plataforma** | Paper 1.21 - 26.1.2+ o Folia 1.21 - 26.1.2+ | Uso estricto de componentes `TEXT_DISPLAY` y comandos Brigadier. |
| **Java** | Java 21 LTS | Código fuente compilado en formato JDK 21. |

## 📦 Dependencias

Debes tener instalados los siguientes plugins en tu carpeta `plugins/`:

1. **Vault** *(Opcional)*: Si deseas otorgar diseños de nametag específicos dependiendo del rango del usuario (Admin, VIP, etc.).
2. **PlaceholderAPI** *(Opcional)*: Para parsear variables externas como estadísticas, dinero o clanes en los nametags.
3. **ItemsAdder** *(Opcional)*: Para mostrar emojis personalizados en los nombres.

## 🚀 Pasos de Instalación

1. Descarga el archivo `EligiusNametag-1.x.x.jar` desde la pestaña **Releases**.
2. Copia el archivo en el directorio `plugins/` de tu servidor.
3. Inicia tu servidor.
4. Verás en la consola la animación de inicio con el gato de Eligius MC confirmando que el plugin se ha conectado.
5. ¡Listo! Puedes configurar todo desde `plugins/EligiusNametag/`.
