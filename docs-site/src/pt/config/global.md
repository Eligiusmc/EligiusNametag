---
title: Configuração Global
description: Aprenda como configurar o EligiusNametag globalmente via config.yml.
---

# Configuração Global (config.yml)

O arquivo `config.yml` é o cérebro do plugin. Ele controla como o plugin interage com seu servidor a nível macro, afetando todos os jogadores igualmente.

---

## 🛠️ Variáveis de Interface

```yaml
# Altura do holograma acima da cabeça do jogador
# Recomendado: 0.35 para humanos, 0.50 se usar skins com chapéus grandes.
y_offset: 0.35

# Distância máxima de visão em blocos
# Valores mais baixos podem melhorar o desempenho do cliente.
view_distance: 64

# Aliases do comando principal
# Todos estes executarão o menu de ajuda ou comandos aninhados.
command_aliases:
  - "enametag"
  - "eltag"
```

---

## 💾 Conexão com Banco de Dados (HikariCP)

Ao contrário de plugins básicos que salvam preferências em arquivos de texto, o EligiusNametag permite um fluxo simultâneo perfeito usando o padrão da indústria `HikariCP` para evitar quedas de desempenho (Lag Spikes) durante salvamentos massivos em grandes redes Folia.

### 🔹 Opção A: SQLite (Local)
Ideal para servidores únicos (Survival isolado, Skyblock). Não requer configuração externa. Um arquivo `database.db` será criado automaticamente na pasta do plugin.

```yaml
database:
  type: "SQLITE" 
```

### 🔹 Opção B: MySQL (Redes Velocity/Bungee)
Ideal se você possui vários servidores conectados e deseja que a preferência do comando `/enametag me` (mostrar ou esconder a nametag) viaje com o jogador de um servidor para outro.

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
Certifique-se de que o banco de dados `eligius_network` já exista no seu servidor MySQL/MariaDB antes de ligar o plugin. O plugin criará as tabelas internamente, mas não pode criar o banco de dados raiz por motivos de segurança.
:::
