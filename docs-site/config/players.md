# Diseños de Jugadores (players.yml)

Aquí es donde ocurre la magia visual. El archivo `players.yml` te permite definir grupos infinitos usando MiniMessage y PlaceholderAPI.

### Formato por Defecto
Cualquier usuario sin rango especial caerá aquí:
```yaml
players:
  default_format:
    - "<yellow>Jugador</yellow>"
    - "<white><PLAYER></white>"
```

### Rangos Vault Avanzados
Puedes crear grupos de nametag que coincidan exactamente con el nombre de tu rango en **LuckPerms** o cualquier otro plugin de permisos basado en Vault.

```yaml
  groups:
    vip:
      - "<gradient:gold:yellow>:star: Usuario VIP</gradient>"
      - "<white><PLAYER></white>"
    admin:
      - "<red>:rank_dev: Administrador Supremo</red>"
      - "<gradient:red:dark_red><bold><PLAYER></bold></gradient>"
```

> **Consejo de ItemsAdder:** Fíjate en los atajos como `:star:` o `:rank_dev:`. Si tienes ItemsAdder o un plugin de ResourcePack, el cliente del jugador automáticamente los transformará en el ícono o imagen personalizada sobre la cabeza del jugador.
