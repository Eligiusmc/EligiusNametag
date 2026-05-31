---
title: Instalação e Requisitos
description: Aprenda como instalar o EligiusNametag no seu servidor Paper ou Folia.
---

# Instalação e Requisitos

O EligiusNametag é um plugin moderno e poderoso. Aproveitando os recursos mais recentes da API de texto do Minecraft e o multi-threading, ele requer um ambiente atualizado.

## 📋 Requisitos do Sistema

| Requisito | Versão Mínima | Motivo |
|-----------|---------------|--------|
| **Plataforma** | Paper 1.21 ou Folia 1.21+ | Uso estrito de componentes `TEXT_DISPLAY` e comandos Brigadier. |
| **Java** | Java 21 LTS | Código fonte compilado no formato JDK 21. |

## 📦 Dependências

Você deve ter os seguintes plugins instalados na sua pasta `plugins/`:

1. **ProtocolLib** *(Obrigatório)*: Usado para enviar os pacotes de hologramas invisivelmente, sem consumir TPS físico.
2. **Vault** *(Opcional)*: Se você deseja conceder designs específicos de nametag dependendo do rank do usuário (Admin, VIP, etc.).
3. **PlaceholderAPI** *(Opcional)*: Para processar variáveis externas como estatísticas, dinheiro ou clãs nos nametags.
4. **ItemsAdder** *(Opcional)*: Para exibir emojis personalizados nos nomes.

## 🚀 Passos de Instalação

1. Baixe o arquivo `EligiusNametag-1.x.x.jar` na aba **Releases**.
2. Copie o arquivo para o diretório `plugins/` do seu servidor.
3. Certifique-se de ter o **ProtocolLib** instalado.
4. Inicie o seu servidor.
5. Você verá a animação de inicialização no console com o gato do Eligius MC confirmando que o plugin conectou com sucesso.
6. Pronto! Você pode configurar tudo a partir de `plugins/EligiusNametag/`.
