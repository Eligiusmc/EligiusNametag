---
title: Pets Holográficos
description: Como configurar as nametags dos pets no pets.yml.
---

# Pets Holográficos (pets.yml)

O sistema de pets é uma das características mais inovadoras do EligiusNametag. Com ele, qualquer lobo, papagaio ou gato domado por um jogador receberá uma nametag holográfica flutuante que segue o pet suavemente.

### Configuração Base

```yaml
pets:
  enabled: true
  show_unnamed: false 
```

- Se `show_unnamed` estiver `true`, mesmo um lobo domado genérico terá um holograma de texto. 
- Se estiver `false`, apenas os pets que o jogador renomear usando uma **Bigorna** (Name Tag vanilla) ativarão o sistema holográfico, para conservar um estilo mais Vanilla no servidor geral.

### Design das Linhas
Use `<DISPLAYNAME>` para mostrar o nome que o jogador deu na bigorna, e `<PLAYER>` para citar o dono.

```yaml
  default_format:
    - "<gray>Pet de <PLAYER></gray>"
    - "<white><DISPLAYNAME></white>"
```

### 🐾 O Maior Segredo!
Os pets **herdam** o design do rank Vault de seu dono.
Isso significa que se o dono de um gato tiver o rank "Admin" no `players.yml`, e o rank Admin tiver um texto vermelho e um ícone de fogo, **o gato também exibirá o texto vermelho e o ícone de fogo** na sua variável de grupo (se você configurá-lo dessa maneira cruzando variáveis PAPI ou permissões LuckPerms no nível do nó `<PLAYER>`).
