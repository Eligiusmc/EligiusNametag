---
title: Comandos e Permissões
description: Veja todos os comandos e permissões disponíveis para o EligiusNametag.
---

# Comandos e Permissões

Todos os comandos padrão são baseados em `/enametag` (ou seus aliases personalizáveis `/eligiusnametag`).

| Comando | Descrição | Permissão Necessária |
|---------|-------------|----------------------|
| `/enametag` | Exibe a versão do plugin instalada e o logotipo do autor. | *Nenhuma* |
| `/enametag reload` | Recarrega todas as configurações e arquivos YAML em tempo real, sem reiniciar o servidor. | `eligiusnametag.admin` |
| `/enametag lang <idioma>` | Muda dinamicamente o idioma global do plugin e recarrega os idiomas instantaneamente. | `eligiusnametag.admin` |
| `/enametag pets` | Alterna globalmente a exibição de hologramas de texto acima dos pets. | `eligiusnametag.admin` |
| `/enametag me` | Alterna pessoalmente a exibição da sua própria nametag (Salvo automaticamente no banco de dados). | `eligiusnametag.viewself` |

## Detalhes Adicionais de Permissão

- `eligiusnametag.*`: Concede acesso total a todos os comandos de administração.
- *Nota:* Se você usa LuckPerms, não se esqueça de atribuir a permissão principal ao seu grupo *default* ou *admin* para que as variáveis de formatação sejam sincronizadas corretamente.
