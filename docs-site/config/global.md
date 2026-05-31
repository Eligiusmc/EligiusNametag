# Configuración Global (config.yml)

El archivo `config.yml` es el cerebro del plugin. Controla cómo el plugin interactúa con tu servidor a nivel macro, afectando a todos los jugadores por igual.

---

## 🛠️ Variables de Interfaz

```yaml
# Altura del holograma por encima de la cabeza del jugador
# Recomendado: 0.35 para humanos, 0.50 si usas skins con sombreros grandes.
y_offset: 0.35

# Distancia máxima de visión en bloques
# Valores más bajos pueden mejorar el rendimiento del cliente.
view_distance: 64

# Alias del comando principal
# Todos estos ejecutarán el menú de ayuda o comandos anidados.
command_aliases:
  - "enametag"
  - "eltag"
```

---

## 💾 Conexión de Bases de Datos (HikariCP)

A diferencia de otros plugins básicos que guardan las preferencias en archivos de texto, EligiusNametag permite un flujo concurrente perfecto usando el estándar de la industria `HikariCP` para evitar caídas de rendimiento (Lag Spikes) durante guardados masivos en redes inmensas de Folia.

### 🔹 Opción A: SQLite (Local)
Ideal para servidores únicos (Survival, Skyblock aislado). No requiere configuración externa. Se creará automáticamente un archivo `database.db` en la carpeta del plugin.

```yaml
database:
  type: "SQLITE" 
```

### 🔹 Opción B: MySQL (Redes Velocity/Bungee)
Ideal si posees múltiples servidores conectados y deseas que la preferencia del comando `/enametag me` (mostrar u ocultar tu nametag) viaje con el jugador de un servidor a otro.

```yaml
database:
  type: "MYSQL"
  mysql:
    host: "127.0.0.1"
    port: 3306
    database: "eligius_network"
    username: "admin"
    password: "super_secure_password"
```

::: warning ⚠️ IMPORTANTE SOBRE MYSQL
Asegúrate de que la base de datos `eligius_network` ya exista en tu servidor MySQL/MariaDB antes de encender el plugin. El plugin creará las tablas internamente, pero no puede crear la base de datos raíz por motivos de seguridad.
:::
