# Tela Inicial (Dashboard Global) - TelaInicialController

## 🎯 Objetivo / Contexto

Dashboard central / Tela Pai da aplicação. Ele orquestra os ciclos de vida das abas principais (`JFXTabPane`) e serve de âncora estrutural (UI Root) para que atalhos sejam acionados e para ancoragem de progress bars de fundo e painéis flutuantes (Alerts e Modais).

## 🧩 Componentes de UI e Arquivos

- **Classe Principal:** `TelaInicialController.kt`
- **Layout XML (FXML):** `TelaInicial.fxml`
- **Principais Views (IDs):**
  - `tpGlobal`: Instância do `JFXTabPane` que abraça os modulos FXMLs integrados (`AbaArquivo`, `AbaPastas`, `AbaComicInfo`).
  - `rootStack`: `StackPane` raiz. Usado pelas rotinas do `AlertasPopup` para construir os painéis Dialog nativos do JFoenix e embutir o esmaecimento (Efeito Blur).
  - `rootProgress`: Uma barra gráfica que mostra a atividade de Thread das rotinas em Async das subtelas.
  - `rootMessage`: Feedback de texto rodando paralelamente à barra de progresso.

## ⚙️ Regras de Negócio e Lógica (Core Logic)

- **Injeção Centralizada:** Instancia na memória seus controlers filhos (`arquivoController`, `pastasController`, `comicinfoController`) e estabelece as suas referências parentais para que comuniquem uns com os outros.
- **Tratamento Global de Atalhos (KeyEvents):** Através do `configurarAtalhos(scene)`, atrela globalmente o listener do Scene para repassar teclas de forma hierárquica à subtela que estiver em exibição no TabPane atual (Switch Case por seleção do Tab).

## 🔄 Fluxo de Navegação

- Transita sem restrições ou reload de tela usando o `tpGlobal.selectionModel`.

## 🌍 Atalhos / Dicionário (Referência)

- Suas views principais gerenciam as transições globais como o `Notificacoes.rootAnchorPane` para animações em overlay (estilo snackbar/toast).
- O arquivo CSS principal herdado aqui: `Dark_TelaInicial.css`.
