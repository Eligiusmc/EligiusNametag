---
title: Solução de Problemas
description: Problemas comuns e soluções ao usar o EligiusNametag.
---

# Solução de Problemas (Troubleshooting)

Tendo problemas com o EligiusNametag? Verifique estas soluções comuns:

### 1. A Nametag não aparece acima do jogador
- **Você tem o PlaceholderAPI instalado?** Se você usa variáveis como `%vault_rank%` e não tem o PAPI, o texto pode não renderizar.

### 2. Erro "Database Connection Closed"
- Isso foi **corrigido em versões recentes**. Certifique-se de estar executando a versão mais recente `v1.x.x` ou superior. Se você usa **MySQL**, verifique se o IP e as credenciais no `config.yml` estão corretos. Caso contrário, use `SQLITE` local.

### 3. Avisos do ViaVersion no Console
Se você usa o ViaVersion para permitir que jogadores na 1.20 entrem no seu servidor 1.21 - 26.1.2+, você pode ver mensagens sobre *Metadata* e *Protocol Versions*.
- **Causa:** O cliente antigo (ex: 1.20.2) não possui o código nativo para processar entidades `TEXT_DISPLAY` grandes corretamente.
- **Solução:** Recomendamos fortemente exigir versões 1.21 - 26.1.2+ para visualização perfeita dos hologramas, ou simplesmente ignore o aviso se o jogador não tiver problemas visuais.

### 4. /enametag lang não atualiza a mensagem de ajuda
Ao usar `/enametag lang pt`, o plugin lê de `lang/pt.yml`. Se você notar que alguma tradução está vazia ou um comando age estranho, você pode deletar a pasta `lang/` no diretório de plugins para que o EligiusNametag possa regenerá-la de forma limpa com os padrões mais recentes.
