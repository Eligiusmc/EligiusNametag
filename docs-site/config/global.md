# Configuración Global (config.yml)

El archivo `config.yml` es el cerebro del plugin. Controla cómo el plugin interactúa con tu servidor a nivel macro.

### Variables Generales

```yaml
# Altura del holograma por encima de la cabeza del jugador
y_offset: 0.35

# Distancia máxima de visión en bloques
view_distance: 64

# Alias del comando principal
command_aliases:
  - "eltag"
  - "enametag"
```

### Bases de Datos (HikariCP)
A diferencia de otros plugins básicos, EligiusNametag permite un flujo concurrente perfecto usando `HikariCP` para evitar caídas de rendimiento durante guardados masivos.

```yaml
database:
  type: "SQLITE" # Opciones: "SQLITE" o "MYSQL"
  mysql:
    host: "localhost"
    port: 3306
    database: "eligius"
    username: "root"
    password: "password"
```

Si usas una red tipo Velocity o BungeeCord, se recomienda usar **MYSQL** para que las preferencias de los jugadores (`/eltag me`) se sincronicen globalmente.
