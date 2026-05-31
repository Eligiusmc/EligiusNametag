---
title: Designs de Jogadores
description: Configure designs personalizados MiniMessage e Vault no players.yml.
---

# Designs de Jogadores (players.yml)

É aqui que a mágica visual acontece. O arquivo `players.yml` permite que você defina **grupos ilimitados** usando o motor moderno *MiniMessage*, interpolado com a versatilidade do *PlaceholderAPI*.

---

## 🎨 Entendendo MiniMessage

Em vez de usar códigos de cor antigos e limitados como `&c` ou `&l`, agora você pode usar tags HTML altamente descritivas.

| Tag | Resultado |
|-----|-----------|
| `<red>` | Texto em vermelho puro |
| `<#ff00ff>` | Texto usando código HEX personalizado |
| `<bold>` | Texto em **negrito** |
| `<gradient:red:blue>` | Gera uma transição suave entre duas ou mais cores |

---

## 👥 Formato Padrão

Qualquer usuário que não se encaixe em uma hierarquia de rank cairá neste design:

```yaml
players:
  default_format:
    - "<yellow>Jogador</yellow>"
    - "<white><PLAYER></white>"
```
*Você notará que pode criar quantas linhas quiser simplesmente adicionando hifens (`-`) à lista.*

---

## 👑 Ranks Avançados & Vault

Você pode criar grupos de nametag que correspondam exatamente ao nome do seu rank no **LuckPerms** (ou em qualquer outro sistema de permissões compatível com o Vault). O plugin lê o *Primary Group* (Grupo Principal) do jogador e procura o seu equivalente aqui.

```yaml
  groups:
    vip:
      - "<gradient:gold:yellow>⭐ Usuário VIP</gradient>"
      - "<white><PLAYER></white>"
    admin:
      - "<red>:rank_dev: Admin Supremo</red>"
      - "<gradient:red:dark_red><bold><PLAYER></bold></gradient>"
```

### 🖼️ Integração com ItemsAdder / Oraxen

Se você olhar de perto o formato do `admin`, você notará o texto `:rank_dev:`. 

Se você tiver o **ItemsAdder** ou qualquer Pacote de Recursos instalado em seu servidor, o cliente do jogador lerá isso e substituirá automaticamente os dois pontos por uma **imagem de fonte real** renderizada acima da cabeça do admin.

::: tip 💡 DICA DE DESIGN
Evite criar Nametags com mais de 3 linhas para jogadores comuns. Muitas linhas podem obstruir a visão em PvP ou construções densas.
:::
