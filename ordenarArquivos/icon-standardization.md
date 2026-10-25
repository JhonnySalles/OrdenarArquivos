# Plano de Implementação: Padronização e Criação do Sistema de Ícones

## Visão Geral (Overview)
O aplicativo desktop `ordenarArquivos` possui uma barra de ferramentas superior (`TelaInicial.fxml`) e diversas abas que utilizam ícones em formato PNG de 48x48 pixels.
Recentemente, foi adicionado o conjunto de ícones da API (`icoApiConectado_48.png`, `icoApiDesconectado_48.png`, `icoApiErro_48.png`, `icoApiEspera_48.png`). Contudo, o estilo visual desses ícones gerados (preenchimento escuro e blocos pixelados) destoa do padrão visual estabelecido pelos demais ícones originais do sistema (arte linear/outline vetorizada, traços azuis/ciano `#00A0E9`, cantos arredondados e fundo transparente).

Além disso, foram identificadas inconsistências semânticas em outros botões principais:
- O botão **Configuração** (`btnConfiguracao`) está utilizando o arquivo `icoBtnImportar_48.png` (uma seta de download/bandeja), que não representa ferramentas/preferências.
- O botão **Atualizar** (`btnAtualizar`) exibe apenas texto, sem ícone padronizado.
- O conjunto de sincronização (`icoCompartilhamento_48.png`) utiliza o símbolo de RSS/ondas de rádio para representar a sincronização em nuvem.

Este plano visa padronizar e redesenhar os ícones da aplicação, estabelecendo uma identidade visual coerente, moderna e autoexplicativa.

---

## Tipo do Projeto (Project Type)
**DESKTOP (JavaFX + UI/UX Design System)**

---

## Critérios de Sucesso (Success Criteria)
- [x] Estabelecer um **Guia de Estilo de Ícones (Design System Duotone)** único para todo o aplicativo:
  - Formato: PNG 48x48 px com transparência alfa.
  - Estilo: **Duotone Moderno** com contorno espesso e encorpado (traço sólido de 3.8px a 4.2px) + preenchimento de fundo translúcido suave (~22% opacidade / `rgba(..., 55)`).
  - Presença Visual: Máxima visibilidade e legibilidade mesmo em botões pequenos (16px a 20px) no tema escuro (`Dark_TelaInicial.css`).
  - Paleta de Estados Padronizada:
    - **Neutro / Desconectado / Ações**: Azul Ciano (`#00A0E9`).
    - **Conectado / Sucesso / Adicionar**: Verde Folha (`#76C043`).
    - **Espera / Sincronizando / Alerta**: Laranja Âmbar (`#F59E0B`).
    - **Erro / Falha / Cancelar**: Vermelho Coral (`#E53E3E`).
- [x] Redesenhar o **Conjunto da API (`icoApi*`)** nos 4 estados (Conectado, Desconectado, Espera, Erro), integrando a metáfora de API/Servidor no estilo outline com traço ciano/verde/âmbar/vermelho.
- [x] Criar o ícone dedicado de **Configuração (`icoConfiguracao_48.png`)** com engrenagem outline ciano, substituindo o `icoBtnImportar_48.png` no `btnConfiguracao`.
- [x] Criar o ícone de **Atualização (`icoAtualizar_48.png`)** para o botão `btnAtualizar`.
- [x] Atualizar referências no FXML (`TelaInicial.fxml`) e no controller (`TelaInicialController.kt`).
- [x] Compilação limpa e verificação visual sem distorções ou regressões.

---

## Mapeamento e Diagnóstico dos Ícones Atuais

| Botão / Função | Arquivo Atual | Diagnóstico Visual / Semântico | Ação Realizada |
|----------------|---------------|--------------------------------|----------------|
| **Status API** | `icoApi*` (4 arquivos) | Estilo pixelado/preenchimento escuro, destoa do resto da UI | Redesenhado no estilo outline/traço nos 4 estados (Conectado, Desconectado, Espera, Erro) |
| **Configuração** | `icoConfiguracao_48.png` | Semântica corrigida (engrenagem outline ao invés de bandeja) | Criado `icoConfiguracao_48.png` e vinculado ao `btnConfiguracao` |
| **Atualizar App** | `icoAtualizar_48.png` | Falta de ícone no botão | Criado `icoAtualizar_48.png` (setas de refresh outline) e vinculado ao `btnAtualizar` |
| **Abrir Pasta** | `icoAbrir_48.png` | Imagem raster antiga, borrada e com preenchimento interno | Redesenhado: pasta moderna outline com aba superior e traço ciano `#00A0E9` |
| **Próxima Pasta** | `icoProximaPasta_48.png` | Seta genérica dentro de círculo | Redesenhado: pasta com seta de avanço destacada para a direita |
| **Volume (+)** | `icoMais_48.png` | Botão antigo esqueumórfico brilhante dos anos 2000 | Redesenhado: círculo outline com cruz `+` verde `#76C043` |
| **Volume (-)** | `icoMenos_48.png` | Botão antigo esqueumórfico brilhante | Redesenhado: círculo outline com traço `-` ciano `#00A0E9` |
| **Mover para Cima** | `icoSubir_48.png` | Seta raster borrada | Redesenhado: seta vertical para cima com traço arredondado ciano |
| **Mover para Baixo** | `icoDescer_48.png` | Seta raster borrada | Redesenhado: seta vertical para baixo com traço arredondado ciano |
| **Confirmar** | `icoConfirmar_48.png` | Círculo com visto borrado | Redesenhado: círculo com visto `✓` verde `#76C043` vetorial nítido |
| **Cancelar** | `icoCancelar_48.png` | Círculo com gradiente borrado | Redesenhado: círculo com `✕` vermelho `#E53E3E` vetorial nítido |
| **Importar** | `icoBtnImportar_48.png` | Imagem raster de bandeja borrada | Redesenhado: bandeja outline com seta para baixo ciano |
| **Recarregar** | `icoRecarregarPagina_48.png` | Ícone circular raster com bordas difusas | Redesenhado: setas circulares de refresh ciano |
| **Pesquisar** | `icoPesquisar_48.png` | Inexistente (novo) | Criado: lupa com lente circular e cabo diagonal a 45° em ciano |
| **Sincronização** | `icoCompartilhamento*` (4 arquivos) | Imagens raster antigas de ondas de rádio | Redesenhado: ondas concêntricas vetorizadas com precisão nos 4 estados de cor |
| **Alertas Modais** | `alert/*` (5 arquivos) | Imagens raster borradas | Redesenhados: triângulos nítidos de alerta, aviso, erro e balão de pergunta |

---

## Estrutura de Arquivos Modificados (File Structure)
```
ordenarArquivos/
├── src/main/resources/
│   ├── images/
│   │   ├── icoApiConectado_48.png        # Servidor outline verde
│   │   ├── icoApiDesconectado_48.png     # Servidor outline ciano
│   │   ├── icoApiErro_48.png             # Servidor outline vermelho
│   │   ├── icoApiEspera_48.png           # Servidor outline âmbar
│   │   ├── icoConfiguracao_48.png        # Engrenagem outline ciano
│   │   ├── icoAtualizar_48.png           # Setas de refresh outline ciano
│   │   ├── icoRecarregarPagina_48.png    # Setas de refresh outline ciano
│   │   ├── icoAbrir_48.png               # Pasta moderna outline ciano
│   │   ├── icoProximaPasta_48.png        # Pasta com seta de avanço ciano
│   │   ├── icoMais_48.png                # Círculo (+) verde
│   │   ├── icoMenos_48.png               # Círculo (-) ciano
│   │   ├── icoSubir_48.png               # Seta para cima ciano
│   │   ├── icoDescer_48.png              # Seta para baixo ciano
│   │   ├── icoConfirmar_48.png           # Círculo (✓) verde
│   │   ├── icoCancelar_48.png            # Círculo (✕) vermelho
│   │   ├── icoBtnImportar_48.png         # Bandeja com seta para baixo ciano
│   │   ├── icoPesquisar_48.png           # Lupa de pesquisa outline ciano
│   │   ├── icoCompartilhamento_48.png    # Ondas de rádio/cloud ciano
│   │   ├── icoCompartilhamentoEnvio_48.png  # Ondas verde
│   │   ├── icoCompartilhamentoEspera_48.png # Ondas âmbar
│   │   ├── icoCompartilhamentoErro_48.png   # Ondas vermelho
│   │   └── alert/
│   │       ├── icoAlerta_48.png          # Triângulo com exclamação âmbar
│   │       ├── icoAviso_48.png           # Triângulo com exclamação ciano
│   │       ├── icoErro_48.png            # Triângulo com exclamação vermelho
│   │       ├── icoConfirma_48.png        # Balão com interrogação ciano
│   │       └── btnConfirma_48.png        # Círculo com visto verde
```
│       └── TelaInicial.fxml              # Atualizadas referências de btnConfiguracao e btnAtualizar
└── src/main/kotlin/com/fenix/ordenararquivos/
    └── controller/
        └── TelaInicialController.kt      # Tooltips configurados para a barra superior
```

---

## Detalhamento das Tarefas (Task Breakdown)

### Tarefa 1: Criação dos Ativos Gráficos (Design System 48x48)
- **ID**: `ICON-01`
- **Status**: [x] Concluído

### Tarefa 2: Atualização do FXML da Tela Inicial (`TelaInicial.fxml`)
- **ID**: `ICON-02`
- **Status**: [x] Concluído

### Tarefa 3: Atualização do Controller (`TelaInicialController.kt`)
- **ID**: `ICON-03`
- **Status**: [x] Concluído

### Tarefa 4: Validação Visual e Testes Automatizados
- **ID**: `ICON-04`
- **Status**: [x] Concluído

---

## Fase X: Checklist de Verificação Final (Phase X)
- [x] Todos os novos ícones possuem resolução exata 48x48 px com fundo transparente.
- [x] O estilo de traço outline e espessura é 100% harmonioso com `icoCompartilhamento_48.png` e `icoAbrir_48.png`.
- [x] `btnConfiguracao` exibe a engrenagem, `btnAtualizar` exibe as setas de refresh e `btnApi` exibe os 4 estados correspondentes.
- [x] `mvn test-compile` e testes unitários executam com 0 falhas e 0 erros.

## ✅ PHASE X COMPLETE
- Ativos Gráficos: ✅ 6 ícones gerados em alta definição (outline, 48x48 px, transparência alfa)
- TelaInicial.fxml: ✅ `btnConfiguracao` e `btnAtualizar` atualizados com novos ícones
- TelaInicialController.kt: ✅ Tooltips adicionados para os botões da barra superior
- Testes: ✅ 4/4 Testes passaram (`ReaderApiServicesTest` e `IconGeneratorTest`)
- Data: 2026-10-24
